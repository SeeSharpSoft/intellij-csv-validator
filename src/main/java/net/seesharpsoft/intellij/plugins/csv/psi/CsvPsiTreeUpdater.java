package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.table.api.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import net.seesharpsoft.intellij.psi.PsiHelper;
import net.seesharpsoft.intellij.util.Suspendable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.EventListenerList;
import java.util.*;
import java.util.stream.Collectors;

public class CsvPsiTreeUpdater implements PsiFileHolder, Suspendable {

    private final PsiFileHolder myPsiFileHolder;

    private final PsiFileFactory myFileFactory;

    private final CsvPsiParserFileType myFileType;

    private List<PsiAction> myUncommittedActions = new ArrayList<>();

    public CsvPsiTreeUpdater(@NotNull PsiFileHolder psiFileHolder) {
        myPsiFileHolder = psiFileHolder;
        myFileFactory = PsiFileFactory.getInstance(getPsiFile().getProject());
        myFileType = new CsvPsiParserFileType(CsvHelper.getValueSeparator(psiFileHolder.getPsiFile()), CsvHelper.getEscapeCharacter(psiFileHolder.getPsiFile()));
    }

    private FileType getFileType() {
        myFileType.setSeparator(CsvHelper.getValueSeparator(getPsiFile()));
        myFileType.setEscapeCharacter(CsvHelper.getEscapeCharacter(getPsiFile()));
        return myFileType;
    }

    private PsiFile createFile(@NotNull String text) {
        return myFileFactory.createFileFromText("a.csv", getFileType(), text);
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

    @Override
    public void dispose() {
        Suspendable.super.dispose();
        myUncommittedActions = null;
    }

    public @Nullable PsiElement createValueSeparator() {
        return PsiHelper.findFirst(createFile(CsvHelper.getValueSeparator(getPsiFile()).getCharacter()), CsvTypes.COMMA);
    }

    public @Nullable PsiElement createLineBreak() {
        return PsiHelper.findFirst(createFile("\n"), CsvTypes.CRLF);
    }

    public void doAction(PsiAction action) {
        myUncommittedActions.add(action);
    }

    public void appendEmptyFields(@NotNull PsiElement anchor, int no) {
        if (no < 1) return;
        for (int i = 0; i < no; ++i) {
            appendField(anchor);
        }
    }

    public void appendField(@NotNull PsiElement anchor) {
        appendField(anchor, false);
    }

    public void appendField(@NotNull PsiElement anchor, boolean before) {
        appendField(anchor, "", false, before);
    }

    public void appendField(@NotNull PsiElement anchor, String text, boolean enquoteCommentIndicator) {
        appendField(anchor, text, enquoteCommentIndicator, false);
    }

    public void appendField(@NotNull PsiElement anchor, String text, boolean enquoteCommentIndicator, boolean before) {
        while (anchor != null && !(anchor instanceof CsvField || CsvHelper.isCommentElement(anchor))) {
            anchor = anchor.getParent();
        }
        // no columns in comment row
        if (CsvHelper.isCommentElement(anchor)) return;
        assert anchor instanceof CsvField;
        doAddField(anchor, text, enquoteCommentIndicator, before);
        doAddValueSeparator(anchor, before);
    }

    public void removeField(@NotNull PsiElement field) {
        removeField(field, false);
    }

    public void removeField(@NotNull PsiElement field, boolean removeRowIfOnlyField) {
        assert field instanceof CsvField;
        PsiElement separator = PsiTreeUtil.findSiblingBackward(field, CsvTypes.COMMA, null);
        if (separator == null) separator = PsiTreeUtil.findSiblingForward(field, CsvTypes.COMMA, null);
        // no separator means it is the only field in the row
        if (separator == null) {
            if (removeRowIfOnlyField) {
                deleteRow(field.getParent());
            }
            return;
        }
        delete(field, separator);
    }

    /**
     * This can be a heavy operation on PsiFile, so it will be executed on the document itself.
     */
    public void addColumn(int columnIndex, boolean before) {
        List<Pair<TextRange, String>> replacements = new ArrayList<>();
        PsiFile psiFile = getPsiFile();
        String valueSeparator = CsvHelper.getValueSeparator(psiFile).getCharacter();
        for (PsiElement record = psiFile.getFirstChild(); record != null; record = record.getNextSibling()) {
            if (!CsvRecord.class.isInstance(record)) continue;
            PsiElement field = record.getFirstChild();
            if (CsvHelper.isCommentElement(field)) continue;

            int startOffset = 0;
            String value = valueSeparator;
            field = PsiHelper.getNextNthSiblingOfType(field, columnIndex, CsvField.class);
            if (field == null) {
                int currentNoOfColumn = PsiTreeUtil.countChildrenOfType(record, CsvField.class);
                field = PsiHelper.getNthChildOfType(record, currentNoOfColumn - 1, CsvField.class);

                startOffset = field.getTextRange().getEndOffset();
                value = valueSeparator.repeat(columnIndex - currentNoOfColumn + 1);
            } else {
                startOffset = (before ? field.getTextRange().getStartOffset() : field.getTextRange().getEndOffset());
            }
            replacements.add(Pair.create(TextRange.create(startOffset, startOffset), value));
        }

        if (replacements.size() > 0) {
            doAction(new DocumentPsiAction(psiFile, replacements));
        }
    }

    /**
     * This can be a heavy operation on PsiFile, so it will be executed on the document itself.
     */
    public void deleteColumns(Collection<Integer> indices) {
        if (indices.size() == 0) return;

        PsiFile psiFile = getPsiFile();
        int columnCount = CsvTableModel.getColumnCount(psiFile);
        // filter double indices
        List<Integer> distinctIndices = indices.stream().distinct().collect(Collectors.toList());
        if (distinctIndices.size() == columnCount) {
            deleteContent();
            return;
        }

        List<Pair<TextRange, String>> replacements = collectRangesToDelete(distinctIndices);
        if (replacements.size() > 0) {
            doAction(new DocumentPsiAction(psiFile, replacements));
        }
    }

    private List<Pair<TextRange, String>> collectRangesToDelete(List<Integer> indices) {
        Set<PsiElement> toDelete = new LinkedHashSet();
        Collections.sort(indices);
        for (PsiElement record = getPsiFile().getFirstChild(); record != null; record = record.getNextSibling()) {
            if (!CsvRecord.class.isInstance(record)) continue;
            if (CsvHelper.isCommentElement(record.getFirstChild())) continue;
            for (int columnIndex : indices) {
                PsiElement focusedCol = PsiHelper.getNthChildOfType(record, columnIndex, CsvField.class);
                // if no field exists in row, we are done
                if (focusedCol != null) {
                    boolean removePreviousSeparator = columnIndex > 0;
                    PsiElement valueSeparator = PsiHelper.getSiblingOfType(focusedCol, CsvTypes.COMMA, removePreviousSeparator);
                    if (toDelete.contains(valueSeparator)) {
                        valueSeparator = PsiHelper.getSiblingOfType(focusedCol, CsvTypes.COMMA, !removePreviousSeparator);
                    }
                    if (valueSeparator != null) {
                        toDelete.add(focusedCol);
                        toDelete.add(valueSeparator);
                    }
                }
            }
        }
        return toDelete.stream().map(element -> Pair.create(element.getTextRange(), "")).collect(Collectors.toList());
    }

    public void addRow(@NotNull PsiElement anchor, boolean before) {
        while (anchor != null && !(anchor instanceof CsvRecord)) {
            anchor = anchor.getParent();
        }
        assert anchor instanceof CsvRecord;
        doAction(new AddSiblingPsiAction(anchor, createRecord(), before));
        doAddLineBreak(anchor, before);
    }

    public void deleteRow(@NotNull PsiElement record) {
        assert record instanceof CsvRecord;
        PsiElement lf = PsiTreeUtil.findSiblingBackward(record, CsvTypes.CRLF, null);
        if (lf == null) lf = PsiTreeUtil.findSiblingForward(record, CsvTypes.CRLF, null);
        // no lf means only one record exists - this is a must, so don't delete it
        if (lf == null) return;
        delete(record, lf);
    }

    public void deleteRows(Collection<Integer> indices) {
        Set<PsiElement> toDelete = new HashSet<>();
        PsiFile psiFile = getPsiFile();
        for (int rowIndex : indices) {
            CsvRecord row = PsiHelper.getNthChildOfType(psiFile, rowIndex, CsvRecord.class);
            boolean removePreviousLF = rowIndex > 0;
            PsiElement lfElement = PsiHelper.getSiblingOfType(row, CsvTypes.CRLF, removePreviousLF);
            if (toDelete.contains(lfElement)) {
                lfElement = PsiHelper.getSiblingOfType(row, CsvTypes.CRLF, !removePreviousLF);
            }
            if (lfElement != null) {
                toDelete.add(row);
                toDelete.add(lfElement);
            }
        }
        delete(toDelete.toArray(new PsiElement[toDelete.size()]));
    }

    public void replaceComment(@NotNull PsiElement toReplace, @Nullable String text) {
        assert PsiHelper.getElementType(toReplace) == CsvTypes.COMMENT;
        // do not replace if not necessary
        if (toReplace.getText().equals(text)) return;

        doAction(new ReplacePsiAction(toReplace, createComment(text)));
    }

    public void replaceField(@NotNull PsiElement toReplace, @Nullable String text, boolean enquoteCommentIndicator) {
        assert toReplace instanceof CsvField;
        // do not replace if not necessary
        if (toReplace.getText().equals(text)) return;

        doAction(new ReplacePsiAction(toReplace, createField(text, enquoteCommentIndicator)));
    }

    public void delete(@NotNull PsiElement ...toRemove) {
        for (PsiElement element : toRemove) {
            doAction(new DeletePsiAction(element));
        }
    }

    public void deleteAllChildren(@NotNull PsiElement anchor) {
        doAction(new DeleteChildrenPsiAction(anchor));
    }

    public void deleteContent() {
        PsiFile psiFile = getPsiFile();
        deleteAllChildren(psiFile);
        // even an empty file has a row with a field
        doAction(new AddChildPsiAction(psiFile, createRecord()));
    }

    private void doAddValueSeparator(@NotNull PsiElement anchor, boolean before) {
        doAction(new AddSiblingPsiAction(anchor, createValueSeparator(), before));
    }

    private void doAddLineBreak(@NotNull PsiElement anchor, boolean before) {
        doAction(new AddSiblingPsiAction(anchor, createLineBreak(), before));
    }

    private void doAddField(@NotNull PsiElement anchor, @Nullable String text, boolean enquoteCommentIndicator, boolean before) {
        doAction(new AddSiblingPsiAction(anchor, createField(text, enquoteCommentIndicator), before));
    }

    public synchronized void commit() {
        if (isSuspended() || myUncommittedActions == null || myUncommittedActions.size() == 0) return;

        suspend();
        List<PsiAction> actionsToCommit = new ArrayList<>(myUncommittedActions);
        if (!doCommit(() -> {
            try {
                actionsToCommit.forEach(PsiAction::execute);
            } finally {
                resume();
                fireCommitted();
            }
        })) {
            resume();
        }

        // TODO even when not committed, those are cleared -> OK?
        myUncommittedActions.clear();
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

    public interface CommitListener extends EventListener {
        void committed();
    }

    private static abstract class PsiAction {
        private final PsiElement myAnchor;

        PsiAction(@NotNull PsiElement anchor) {
            myAnchor = anchor;
        }

        public PsiElement getAnchor() {
            return myAnchor;
        }

        abstract public void execute();
    }

    private static class AddSiblingPsiAction extends PsiAction {

        private final PsiElement myElementToAdd;
        private final boolean myBefore;

        AddSiblingPsiAction(@NotNull PsiElement anchor, @NotNull PsiElement elementToAdd) {
            this(anchor, elementToAdd, false);
        }

        AddSiblingPsiAction(@NotNull PsiElement anchor, @NotNull PsiElement elementToAdd, boolean before) {
            super(anchor);
            myElementToAdd = elementToAdd;
            myBefore = before;
        }

        @Override
        public void execute() {
            PsiElement anchor = getAnchor();
            if (anchor.getParent() == null) return;
            if (myBefore) {
                anchor.getParent().addBefore(myElementToAdd, anchor);
            } else {
                anchor.getParent().addAfter(myElementToAdd, anchor);
            }
        }
    }

    private static class AddChildPsiAction extends PsiAction {

        private final PsiElement myElementToAdd;

        AddChildPsiAction(@NotNull PsiElement parent, @NotNull PsiElement elementToAdd) {
            super(parent);
            myElementToAdd = elementToAdd;
        }

        @Override
        public void execute() {
            PsiElement anchor = getAnchor();
            anchor.add(myElementToAdd);
        }
    }

    private static class ReplacePsiAction extends PsiAction {

        private final PsiElement myReplacement;

        ReplacePsiAction(@NotNull PsiElement anchor, @NotNull PsiElement replacement) {
            super(anchor);
            myReplacement = replacement;
        }

        @Override
        public void execute() {
            getAnchor().replace(myReplacement);
        }
    }

    private static class DeletePsiAction extends PsiAction {

        DeletePsiAction(@NotNull PsiElement anchor) {
            super(anchor);
        }

        @Override
        public void execute() {
            PsiElement anchor = getAnchor();
            if (anchor.getParent() == null) return;
            anchor.delete();
        }
    }

    private static class DeleteChildrenPsiAction extends PsiAction {

        DeleteChildrenPsiAction(@NotNull PsiElement anchor) {
            super(anchor);
        }

        @Override
        public void execute() {
            PsiElement anchor = getAnchor();
            anchor.deleteChildRange(anchor.getFirstChild(), anchor.getLastChild());
        }
    }

    private static class DocumentPsiAction extends PsiAction {

        private final List<Pair<TextRange, String>> myReplacements;

        DocumentPsiAction(@NotNull PsiElement psiFile, List<Pair<TextRange, String>> replacements) {
            super(psiFile);
            assert psiFile instanceof PsiFile;
            myReplacements = replacements;
        }

        @Override
        public void execute() {
            PsiFile psiFile = (PsiFile) getAnchor();

            PsiDocumentManager manager = PsiDocumentManager.getInstance(psiFile.getProject());
            Document document = manager.getDocument(psiFile);
            manager.doPostponedOperationsAndUnblockDocument(document);

            int offset = 0;
            for (Pair<TextRange, String> replacement : myReplacements) {
                TextRange textRange = replacement.getFirst().shiftRight(offset);
                String text = replacement.getSecond();
                document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), text);
                offset += text.length() - textRange.getLength();
            }
        }
    }
}
