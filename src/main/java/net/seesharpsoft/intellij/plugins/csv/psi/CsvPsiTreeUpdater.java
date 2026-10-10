package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;
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

    protected final EventListenerList myEventListenerList = new EventListenerList();

    private final PsiFileHolder myPsiFileHolder;

    private PsiFileFactory myFileFactory;

    private CsvPsiParserFileType myFileType;

    private final List<PsiAction> myUncommittedActions = new ArrayList<>();

    public CsvPsiTreeUpdater(@NotNull PsiFileHolder psiFileHolder) {
        myPsiFileHolder = psiFileHolder;
    }

    public CsvPsiTreeUpdater(@NotNull PsiFile psiFile) {
        this(new PsiFileHolder() {
            @Override
            public PsiFile getPsiFile() {
                return psiFile;
            }

            @Override
            public void dispose() {
            }
        });
    }

    private FileType getFileType() {
        PsiFile psiFile = getPsiFile();
        if (psiFile == null) return null;

        if (myFileType == null) {
            myFileType = new CsvPsiParserFileType(CsvHelper.getValueSeparator(psiFile), CsvHelper.getEscapeCharacter(psiFile));
        } else {
            myFileType.setSeparator(CsvHelper.getValueSeparator(psiFile));
            myFileType.setEscapeCharacter(CsvHelper.getEscapeCharacter(psiFile));
        }
        return myFileType;
    }

    private PsiFileFactory getFileFactory() {
        PsiFile psiFile = getPsiFile();
        if (psiFile == null) return null;

        if (myFileFactory == null) {
            myFileFactory = PsiFileFactory.getInstance(getPsiFile().getProject());
        }
        return myFileFactory;
    }

    private PsiFile createFile(@NotNull String text) {
        PsiFileFactory fileFactory = getFileFactory();
        FileType fileType = getFileType();
        return fileFactory == null || fileType == null ? null : fileFactory.createFileFromText("a.csv", fileType, text);
    }

    private boolean isIndicatingComment(@NotNull String text) {
        return text.trim().startsWith(CsvEditorSettings.getInstance().getCommentIndicator());
    }

    public @Nullable CsvField createField(@NotNull String text, boolean enquoteCommentIndicator) {
        return ReadAction.compute(() -> createFieldImpl(text, enquoteCommentIndicator));
    }

    private @Nullable CsvField createFieldImpl(@NotNull String text, boolean enquoteCommentIndicator) {
        boolean enforceQuoting = CsvEditorSettings.getInstance().isQuotingEnforced();
        if (enquoteCommentIndicator && isIndicatingComment(text)) enforceQuoting = true;

        String sanitizedValue = CsvHelper.quoteCsvField(text, CsvHelper.getEscapeCharacter(getPsiFile()), CsvHelper.getValueSeparator(getPsiFile()), enforceQuoting);

        return SyntaxTraverser.psiTraverser(createFile(sanitizedValue)).filter(CsvField.class).first();
    }

    public @Nullable CsvRecord createRecord() {
        return ReadAction.compute(this::createRecordImpl);
    }

    private @Nullable CsvRecord createRecordImpl() {
        return SyntaxTraverser.psiTraverser(createFile("\n")).filter(CsvRecord.class).first();
    }

    public @Nullable PsiElement createComment(@NotNull String text) {
        return ReadAction.compute(() -> createCommentImpl(text));
    }

    private @Nullable PsiElement createCommentImpl(@NotNull String text) {
        return PsiHelper.findFirst(createFile(!isIndicatingComment(text) ? CsvEditorSettings.getInstance().getCommentIndicator() + text : text), CsvTypes.COMMENT);
    }

    public Document getDocument() {
        return ReadAction.compute(this::getDocumentImpl);
    }

    private Document getDocumentImpl() {
        PsiFile psiFile = getPsiFile();
        if (psiFile == null) return null;

        return PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
    }

    public PsiFile getPsiFile() {
        return myPsiFileHolder.getPsiFile();
    }

    @Override
    public void dispose() {
        Suspendable.super.dispose();
    }

    public @Nullable PsiElement createValueSeparator() {
        return ReadAction.compute(this::createValueSeparatorImpl);
    }

    private @Nullable PsiElement createValueSeparatorImpl() {
        return PsiHelper.findFirst(createFile(CsvHelper.getValueSeparator(getPsiFile()).getCharacter()), CsvTypes.COMMA);
    }

    public @Nullable PsiElement createLineBreak() {
        return ReadAction.compute(this::createLineBreakImpl);
    }

    private @Nullable PsiElement createLineBreakImpl() {
        return PsiHelper.findFirst(createFile("\n"), CsvTypes.CRLF);
    }

    public void doAction(PsiAction action) {
        myUncommittedActions.add(action);
    }

    /**
     * Queues document replacements that are applied as one document change when committed.
     */
    public void replaceTexts(@NotNull List<Pair<TextRange, String>> replacements) {
        if (replacements.isEmpty()) return;
        ReadAction.run(() -> replaceTextsImpl(replacements));
    }

    private void replaceTextsImpl(@NotNull List<Pair<TextRange, String>> replacements) {
        doAction(new DocumentPsiAction(getPsiFile(), replacements));
    }

    public void appendEmptyFields(@NotNull PsiElement anchor, int no) {
        if (no < 1) return;
        ReadAction.run(() -> appendEmptyFieldsImpl(anchor, no));
    }

    private void appendEmptyFieldsImpl(@NotNull PsiElement anchor, int no) {
        for (int i = 0; i < no; ++i) {
            appendFieldImpl(anchor, "", false, false);
        }
    }

    public void appendField(@NotNull PsiElement anchor) {
        ReadAction.run(() -> appendFieldImpl(anchor, "", false, false));
    }

    public void appendField(@NotNull PsiElement anchor, boolean before) {
        ReadAction.run(() -> appendFieldImpl(anchor, "", false, before));
    }

    public void appendField(@NotNull PsiElement anchor, String text, boolean enquoteCommentIndicator) {
        ReadAction.run(() -> appendFieldImpl(anchor, text, enquoteCommentIndicator, false));
    }

    public void appendField(@NotNull PsiElement anchor, String text, boolean enquoteCommentIndicator, boolean before) {
        ReadAction.run(() -> appendFieldImpl(anchor, text, enquoteCommentIndicator, before));
    }

    private void appendFieldImpl(@NotNull PsiElement anchor, String text, boolean enquoteCommentIndicator, boolean before) {
        PsiElement commentOrField = anchor;
        while (commentOrField != null && !(commentOrField instanceof CsvField || CsvHelper.isCommentElement(commentOrField))) {
            commentOrField = commentOrField.getParent();
        }
        // no columns in comment row
        if (CsvHelper.isCommentElement(commentOrField)) return;
        assert commentOrField instanceof CsvField;
        doAddFieldImpl(commentOrField, text, enquoteCommentIndicator, before);
        doAddValueSeparatorImpl(commentOrField, before);
    }

    public void removeField(@NotNull PsiElement field) {
        ReadAction.run(() -> removeFieldImpl(field));
    }

    private void removeFieldImpl(@NotNull PsiElement field) {
        assert field instanceof CsvField;
        PsiElement separator = PsiTreeUtil.findSiblingBackward(field, CsvTypes.COMMA, null);
        if (separator == null) separator = PsiTreeUtil.findSiblingForward(field, CsvTypes.COMMA, null);
        // no separator means it is the only field in the row
        if (separator == null) {
            doAction(new DeleteChildrenPsiAction(field));
            return;
        }
        delete(field, separator);
    }

    /**
     * This can be a heavy operation on PsiFile, so it will be executed on the document itself.
     */
    public void addColumn(int columnIndex, boolean before) {
        ReadAction.run(() -> addColumnImpl(columnIndex, before));
    }

    private void addColumnImpl(int columnIndex, boolean before) {
        List<Pair<TextRange, String>> replacements = new ArrayList<>();
        PsiFile psiFile = getPsiFile();
        String valueSeparator = CsvHelper.getValueSeparator(psiFile).getCharacter();
        for (PsiElement record = psiFile.getFirstChild(); record != null; record = record.getNextSibling()) {
            if (!(record instanceof CsvRecord)) continue;
            PsiElement field = record.getFirstChild();
            if (CsvHelper.isCommentElement(field)) continue;

            int startOffset;
            String value = valueSeparator;
            field = PsiHelper.getNextNthSiblingOfType(field, columnIndex, CsvField.class);
            if (field == null) {
                int currentNoOfColumn = PsiTreeUtil.countChildrenOfType(record, CsvField.class);
                field = PsiHelper.getNthChildOfType(record, currentNoOfColumn - 1, CsvField.class);

                assert field != null;
                startOffset = field.getTextRange().getEndOffset();
                value = valueSeparator.repeat(columnIndex - currentNoOfColumn + 1);
            } else {
                startOffset = (before ? field.getTextRange().getStartOffset() : field.getTextRange().getEndOffset());
            }
            replacements.add(Pair.create(TextRange.create(startOffset, startOffset), value));
        }

        replaceTextsImpl(replacements);
    }

    /**
     * This can be a heavy operation on PsiFile, so it will be executed on the document itself.
     */
    public void deleteColumns(Collection<Integer> indices) {
        ReadAction.run(() -> deleteColumnsImpl(indices));
    }

    private void deleteColumnsImpl(Collection<Integer> indices) {
        if (indices.isEmpty()) return;

        PsiFile psiFile = getPsiFile();
        int columnCount = CsvTableModel.getColumnCount(psiFile);
        // filter double indices
        List<Integer> distinctIndices = indices.stream().distinct().collect(Collectors.toList());
        if (distinctIndices.size() == columnCount) {
            deleteContentImpl();
            return;
        }

        List<Pair<TextRange, String>> replacements = collectRangesToDelete(distinctIndices);
        replaceTextsImpl(replacements);
    }

    private List<Pair<TextRange, String>> collectRangesToDelete(List<Integer> indices) {
        Set<PsiElement> toDelete = new LinkedHashSet<>();
        Collections.sort(indices);
        for (PsiElement record = getPsiFile().getFirstChild(); record != null; record = record.getNextSibling()) {
            if (!(record instanceof CsvRecord)) continue;
            if (CsvHelper.isCommentElement(record.getFirstChild())) continue;
            for (int columnIndex : indices) {
                PsiElement focusedCol = PsiHelper.getNthChildOfType(record, columnIndex, CsvField.class);
                // if no field exists in row, we are done
                if (focusedCol != null) {
                    boolean removePreviousSeparator = columnIndex > 0;
                    PsiElement valueSeparator = PsiHelper.getSiblingOfType(focusedCol, CsvTypes.COMMA, removePreviousSeparator);
                    if (valueSeparator == null || toDelete.contains(valueSeparator)) {
                        valueSeparator = PsiHelper.getSiblingOfType(focusedCol, CsvTypes.COMMA, !removePreviousSeparator);
                    }
                    if (toDelete.contains(valueSeparator)) {
                        valueSeparator = null;
                    }
                    toDelete.add(focusedCol);
                    if (valueSeparator != null) {
                        toDelete.add(valueSeparator);
                    }
                }
            }
        }
        return toDelete.stream().map(element -> Pair.create(element.getTextRange(), "")).collect(Collectors.toList());
    }

    public void addRow(@NotNull PsiElement anchor, boolean before) {
        ReadAction.run(() -> addRowImpl(anchor, before));
    }

    private void addRowImpl(@NotNull PsiElement anchor, boolean before) {
        PsiElement record = anchor;
        while (record != null && !(record instanceof CsvRecord)) {
            record = record.getParent();
        }
        assert record != null;
        doAction(new AddSiblingPsiAction(record, createRecordImpl(), before));
        doAddLineBreakImpl(record, before);
    }

    public void deleteRow(@NotNull PsiElement row) {
        ReadAction.run(() -> deleteRowImpl(row));
    }

    private void deleteRowImpl(@NotNull PsiElement row) {
        assert row instanceof CsvRecord;
        PsiElement lf = PsiTreeUtil.findSiblingBackward(row, CsvTypes.CRLF, null);
        if (lf == null) lf = PsiTreeUtil.findSiblingForward(row, CsvTypes.CRLF, null);
        // no lf means only one record exists - this is a must, so don't delete it
        if (lf == null) doAction(new ReplacePsiAction(row, createRecordImpl()));
        else {
            doAction(new DeletePsiAction(row));
            doAction(new DeletePsiAction(lf));
        }
    }

    public void deleteRows(Collection<Integer> indices) {
        ReadAction.run(() -> deleteRowsImpl(indices));
    }

    private void deleteRowsImpl(Collection<Integer> indices) {
        PsiFile psiFile = getPsiFile();
        if (psiFile == null) return;

        Set<PsiElement> toDelete = new HashSet<>();
        List<Integer> sortedIndices = new ArrayList<>(indices);
        Collections.sort(sortedIndices);
        for (int rowIndex : sortedIndices) {
            CsvRecord row = PsiHelper.getNthChildOfType(psiFile, rowIndex, CsvRecord.class);
            if (row == null) continue;

            boolean removePreviousLF = rowIndex > 0;
            PsiElement lf = PsiHelper.getSiblingOfType(row, CsvTypes.CRLF, removePreviousLF);
            if (lf == null || toDelete.contains(lf)) {
                lf = PsiHelper.getSiblingOfType(row, CsvTypes.CRLF, !removePreviousLF);
            }
            if (toDelete.contains(lf)) {
                lf = null;
            }
            if (lf != null) {
                toDelete.add(row);
                toDelete.add(lf);
            } else {
                doAction(new ReplacePsiAction(row, createRecordImpl()));
            }
        }
        deleteImpl(toDelete.toArray(new PsiElement[0]));
    }

    public void replaceComment(@NotNull PsiElement toReplace, @Nullable String textArg) {
        ReadAction.run(() -> replaceCommentImpl(toReplace, textArg));
    }

    private void replaceCommentImpl(@NotNull PsiElement toReplace, @Nullable String textArg) {
        assert PsiHelper.getElementType(toReplace) == CsvTypes.COMMENT;
        String text = textArg == null ? "" : textArg;
        // do not replace if not necessary
        if (toReplace.getText().equals(text)) return;

        doAction(new ReplacePsiAction(toReplace, createCommentImpl(text)));
    }

    public void replaceField(@NotNull PsiElement toReplace, @Nullable String textArg, boolean enquoteCommentIndicator) {
        ReadAction.run(() -> replaceFieldImpl(toReplace, textArg, enquoteCommentIndicator));
    }

    private void replaceFieldImpl(@NotNull PsiElement toReplace, @Nullable String textArg, boolean enquoteCommentIndicator) {
        assert toReplace instanceof CsvField;
        String text = textArg == null ? "" : textArg;
        // do not replace if not necessary
        if (toReplace.getText().equals(text)) return;

        doAction(new ReplacePsiAction(toReplace, createFieldImpl(text, enquoteCommentIndicator)));
    }

    public void delete(@NotNull PsiElement... toRemove) {
        deleteImpl(toRemove);
    }

    private void deleteImpl(@NotNull PsiElement... toRemove) {
        for (PsiElement element : toRemove) {
            doAction(new DeletePsiAction(element));
        }
    }

    public void deleteAllChildren(@NotNull PsiElement anchor) {
        doAction(new DeleteChildrenPsiAction(anchor));
    }

    public void deleteContent() {
        ReadAction.run(this::deleteContentImpl);
    }

    private void deleteContentImpl() {
        PsiFile psiFile = getPsiFile();
        deleteAllChildren(psiFile);
        // even an empty file has a row with a field
        doAction(new AddChildPsiAction(psiFile, createRecordImpl()));
    }

    private void doAddValueSeparatorImpl(@NotNull PsiElement anchor, boolean before) {
        doAction(new AddSiblingPsiAction(anchor, createValueSeparatorImpl(), before));
    }

    private void doAddLineBreakImpl(@NotNull PsiElement anchor, boolean before) {
        doAction(new AddSiblingPsiAction(anchor, createLineBreakImpl(), before));
    }

    private void doAddFieldImpl(@NotNull PsiElement anchor, @NotNull String text, boolean enquoteCommentIndicator, boolean before) {
        doAction(new AddSiblingPsiAction(anchor, createFieldImpl(text, enquoteCommentIndicator), before));
    }

    public synchronized void commit() {
        if (isSuspended() || myUncommittedActions.isEmpty()) return;

        List<PsiAction> actionsToCommit = new ArrayList<>(myUncommittedActions);
        myUncommittedActions.clear();

        doCommit(() -> actionsToCommit.forEach(PsiAction::execute));
    }

    private boolean doCommit(@NotNull Runnable runnable) {
        PsiFile psiFile = getPsiFile();
        Document document = getDocument();

        if (psiFile == null || !psiFile.isWritable() || document == null || !document.isWritable() || document.isInBulkUpdate())
        {
            return false;
        }

        suspend();
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                CommandProcessor.getInstance().executeCommand(
                        getPsiFile().getProject(),
                        () -> DocumentUtil.executeInBulk(document, runnable),
                        "CSV Editor Changes",
                        null,
                        document);
            } finally {
                resume();
                fireCommitted();
            }
        });

        return true;
    }

    public void addCommitListener(CommitListener l) {
        myEventListenerList.add(CommitListener.class, l);
    }

    public void removeCommitListener(CommitListener l) {
        myEventListenerList.remove(CommitListener.class, l);
    }

    protected void fireCommitted() {
        // Guaranteed to return a non-null array
        Object[] listeners = myEventListenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CommitListener.class) {
                ((CommitListener) listeners[i + 1]).committed();
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

        AddSiblingPsiAction(@NotNull PsiElement anchor, @Nullable PsiElement elementToAdd) {
            this(anchor, elementToAdd, false);
        }

        AddSiblingPsiAction(@NotNull PsiElement anchor, @Nullable PsiElement elementToAdd, boolean before) {
            super(anchor);
            myElementToAdd = elementToAdd;
            myBefore = before;
        }

        @Override
        public void execute() {
            if (myElementToAdd == null) return;

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

        AddChildPsiAction(@NotNull PsiElement parent, @Nullable PsiElement elementToAdd) {
            super(parent);
            myElementToAdd = elementToAdd;
        }

        @Override
        public void execute() {
            if (myElementToAdd == null) return;

            PsiElement anchor = getAnchor();
            anchor.add(myElementToAdd);
        }
    }

    private static class ReplacePsiAction extends PsiAction {

        private final PsiElement myReplacement;

        ReplacePsiAction(@NotNull PsiElement anchor, @Nullable PsiElement replacement) {
            super(anchor);
            myReplacement = replacement;
        }

        @Override
        public void execute() {
            if (myReplacement == null) return;

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
            myReplacements.sort(Comparator.comparingInt(replacement -> replacement.getFirst().getStartOffset()));
        }

        @Override
        public void execute() {
            WriteAction.run(() -> {
                PsiFile psiFile = (PsiFile) getAnchor();

                PsiDocumentManager manager = PsiDocumentManager.getInstance(psiFile.getProject());
                Document document = manager.getDocument(psiFile);
                if (document == null) return;

                manager.doPostponedOperationsAndUnblockDocument(document);

                // Build the changed span first so listeners receive a single document event,
                // even when a batched operation affects thousands of records.
                CharSequence original = document.getImmutableCharSequence();
                int startOffset = myReplacements.getFirst().getFirst().getStartOffset();
                int endOffset = myReplacements.getLast().getFirst().getEndOffset();
                StringBuilder updated = new StringBuilder(endOffset - startOffset);
                int offset = startOffset;
                for (Pair<TextRange, String> replacement : myReplacements) {
                    TextRange textRange = replacement.getFirst();
                    updated.append(original, offset, textRange.getStartOffset());
                    updated.append(replacement.getSecond());
                    offset = textRange.getEndOffset();
                }
                document.replaceString(startOffset, endOffset, updated);

                manager.commitDocument(document);
            });
        }
    }
}
