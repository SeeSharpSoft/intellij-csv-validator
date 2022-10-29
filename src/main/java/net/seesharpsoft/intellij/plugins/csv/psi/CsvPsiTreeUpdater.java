package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;
import com.intellij.util.DocumentUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

public class CsvPsiTreeUpdater implements PsiFileHolder {

    private final PsiFileHolder myPsiFileHolder;

    private final PsiFileFactory myFileFactory;

    private final List<UpdateAction> myUncommittedActions = new ArrayList<>();

    public CsvPsiTreeUpdater(@NotNull PsiFileHolder psiFileHolder) {
        myPsiFileHolder = psiFileHolder;
        myFileFactory = PsiFileFactory.getInstance(getPsiFile().getProject());
    }

    private PsiFile createFile(@NotNull String text) {
        return myFileFactory.createFileFromText("a.csv", CsvLanguage.INSTANCE, text, false, false);
    }

    private boolean isIndicatingComment(@NotNull String text) {
        return text.trim().startsWith(CsvEditorSettings.getInstance().getCommentIndicator());
    }

    public @Nullable CsvField createField(@NotNull String text, boolean enquoteCommentIndicator) {
        boolean enforceQuoting = CsvEditorSettings.getInstance().isQuotingEnforced();
        if (enquoteCommentIndicator && isIndicatingComment(text)) enforceQuoting = true;

        String sanitizedValue = CsvHelper.quoteCsvField(text, CsvHelper.getEscapeCharacter(getPsiFile()), CsvHelper.getValueSeparator(getPsiFile()), enforceQuoting);

        return SyntaxTraverser.psiTraverser(createFile(sanitizedValue)).filter(CsvField.class).first();
    }

    public @NotNull CsvRecord createRecord() {
        return SyntaxTraverser.psiTraverser(createFile("\n")).filter(CsvRecord.class).first();
    }

    public @Nullable PsiElement createComment(@NotNull String text) {
        return PsiHelper.findFirst(createFile(!isIndicatingComment(text) ? CsvEditorSettings.getInstance().getCommentIndicator() + text : text), CsvTypes.COMMENT);
    }

    public PsiFile getPsiFile() {
        return myPsiFileHolder.getPsiFile();
    }

    public @Nullable PsiElement createValueSeparator() {
        return PsiHelper.findFirst(createFile(CsvHelper.getValueSeparator(getPsiFile()).getCharacter()), CsvTypes.COMMA);
    }

    public @Nullable PsiElement createLineBreak() {
        return PsiHelper.findFirst(createFile("\n"), CsvTypes.CRLF);
    }

    public void doAction(UpdateAction action) {
        myUncommittedActions.add(action);
    }

    public void addEmptyColumns(@NotNull PsiElement anchor, int no) {
        if (no < 1) return;
        for (int i = 0; i < no; ++i) {
            addColumn(anchor);
        }
    }

    public void addColumn(@NotNull PsiElement anchor) {
        addColumn(anchor, false);
    }

    public void addColumn(@NotNull PsiElement anchor, boolean before) {
        addColumn(anchor, "", false, before);
    }

    public void addColumn(@NotNull PsiElement anchor, String text, boolean enquoteCommentIndicator) {
        addColumn(anchor, text, enquoteCommentIndicator, false);
    }

    public void addColumn(@NotNull PsiElement anchor, String text, boolean enquoteCommentIndicator, boolean before) {
        while (anchor != null && !(anchor instanceof CsvField)) {
            anchor = anchor.getParent();
        }
        assert anchor instanceof CsvField;
        doAddField(anchor, text, enquoteCommentIndicator, before);
        doAddValueSeparator(anchor, before);
    }

    public void addRow(@NotNull PsiElement anchor, boolean before) {
        while (anchor != null && !(anchor instanceof CsvRecord)) {
            anchor = anchor.getParent();
        }
        assert anchor instanceof CsvRecord;
        doAction(new AddUpdateAction(anchor, createRecord(), before));
        doAddLineBreak(anchor, before);
    }

    public void replaceField(@NotNull PsiElement toReplace, @Nullable String text, boolean enquoteCommentIndicator) {
        assert toReplace instanceof CsvField;
        // do not replace if not necessary
        if (toReplace.getText().equals(text)) return;

        doAction(new ReplaceUpdateAction(toReplace, createField(text, enquoteCommentIndicator)));
    }

    public void delete(@NotNull PsiElement toRemove) {
        doAction(new DeleteUpdateAction(toRemove));
    }

    public void deleteAllChildren(@NotNull PsiElement anchor) {
        doAction(new DeleteChildrenUpdateAction(anchor));
    }

    public void deleteContent() {
        deleteAllChildren(getPsiFile());
    }

    private void doAddValueSeparator(@NotNull PsiElement anchor, boolean before) {
        doAction(new AddUpdateAction(anchor, createValueSeparator(), before));
    }

    private void doAddLineBreak(@NotNull PsiElement anchor, boolean before) {
        doAction(new AddUpdateAction(anchor, createLineBreak(), before));
    }

    private void doAddField(@NotNull PsiElement anchor, @Nullable String text, boolean enquoteCommentIndicator, boolean before) {
        doAction(new AddUpdateAction(anchor, createField(text, enquoteCommentIndicator), before));
    }

    private boolean committing = false;

    public boolean isCommitting() {
        return committing;
    }

    public synchronized void commit() {
        if (committing || myUncommittedActions.size() == 0) return;
        committing = true;

        List<UpdateAction> actionsToCommit = new ArrayList<>(myUncommittedActions);
        if (doCommit(() -> {
            try {
                actionsToCommit.forEach(UpdateAction::execute);
                fireCommitted();
            } finally {
                committing = false;
            }
        })) {
            myUncommittedActions.clear();
        } else {
            committing = false;
        }
    }

    private boolean doCommit(@NotNull Runnable runnable) {
        if (ApplicationManager.getApplication().isUnitTestMode() || !getPsiFile().isWritable()) return false;

        Document document = PsiDocumentManager.getInstance(getPsiFile().getProject()).getDocument(getPsiFile());

        ApplicationManager.getApplication().runWriteAction(() -> {
            CommandProcessor.getInstance().executeCommand(
                    getPsiFile().getProject(),
                    () -> DocumentUtil.executeInBulk(document, runnable),
                    "CSV Table Editor changes",
                    null,
                    document);
        });

        return true;
    }

    /** List of listeners */
    protected EventListenerList listenerList = new EventListenerList();

    public void addCommitListener(CommitListener l) {
        listenerList.add(CommitListener.class, l);
    }

    public void removeCommitListener(CommitListener l) {
        listenerList.remove(CommitListener.class, l);
    }

    protected void fireCommitted() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CommitListener.class) {
                ((CommitListener)listeners[i+1]).committed();
            }
        }
    }

    public static interface CommitListener extends EventListener {
        void committed();
    }

    private static abstract class UpdateAction {
        private final PsiElement myAnchor;

        UpdateAction(@NotNull PsiElement anchor) {
            myAnchor = anchor;
        }

        public PsiElement getAnchor() {
            return myAnchor;
        }

        abstract public void execute();
    }

    private static class AddUpdateAction extends UpdateAction {

        private final PsiElement myElementToAdd;
        private final boolean myBefore;

        AddUpdateAction(@NotNull PsiElement anchor, @NotNull PsiElement elementToAdd) {
            this(anchor, elementToAdd, false);
        }

        AddUpdateAction(@NotNull PsiElement anchor, @NotNull PsiElement elementToAdd, boolean before) {
            super(anchor);
            myElementToAdd = elementToAdd;
            myBefore = before;
        }

        @Override
        public void execute() {
            PsiElement anchor = getAnchor();
            if (myBefore) {
                anchor.getParent().addBefore(myElementToAdd, anchor);
            } else {
                anchor.getParent().addAfter(myElementToAdd, anchor);
            }
        }
    }

    private static class ReplaceUpdateAction extends UpdateAction {

        private final PsiElement myReplacement;

        ReplaceUpdateAction(@NotNull PsiElement anchor, @NotNull PsiElement replacement) {
            super(anchor);
            myReplacement = replacement;
        }

        @Override
        public void execute() {
            getAnchor().replace(myReplacement);
        }
    }

    private static class DeleteUpdateAction extends UpdateAction {

        DeleteUpdateAction(@NotNull PsiElement anchor) {
            super(anchor);
        }

        @Override
        public void execute() {
            getAnchor().delete();
        }
    }

    private static class DeleteChildrenUpdateAction extends UpdateAction {

        DeleteChildrenUpdateAction(@NotNull PsiElement anchor) {
            super(anchor);
        }

        @Override
        public void execute() {
            PsiElement anchor = getAnchor();
            anchor.deleteChildRange(anchor.getFirstChild(), anchor.getLastChild());
        }
    }
}
