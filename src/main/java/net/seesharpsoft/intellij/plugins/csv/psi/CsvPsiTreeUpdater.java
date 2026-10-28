package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvLexerFactory;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableReorder;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import net.seesharpsoft.intellij.psi.PsiHelper;
import net.seesharpsoft.intellij.util.Suspendable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CsvPsiTreeUpdater implements PsiFileHolder, Suspendable {

    private final List<CommitListener> myCommitListeners = new CopyOnWriteArrayList<>();

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
                // Intentionally left empty
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
            myFileFactory = PsiFileFactory.getInstance(psiFile.getProject());
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
        return ReadAction.compute(() -> createFieldInternal(text, enquoteCommentIndicator));
    }

    private @Nullable CsvField createFieldInternal(@NotNull String text, boolean enquoteCommentIndicator) {
        boolean enforceQuoting = CsvEditorSettings.getInstance().isQuotingEnforced();
        if (enquoteCommentIndicator && isIndicatingComment(text)) enforceQuoting = true;

        PsiFile psiFile = getPsiFile();
        String sanitizedValue = CsvHelper.quoteCsvField(text, CsvHelper.getEscapeCharacter(psiFile), CsvHelper.getValueSeparator(psiFile), enforceQuoting);

        return SyntaxTraverser.psiTraverser(createFile(sanitizedValue)).filter(CsvField.class).first();
    }

    public @Nullable CsvRecord createRecord() {
        return ReadAction.compute(this::createRecordInternal);
    }

    private @Nullable CsvRecord createRecordInternal() {
        return SyntaxTraverser.psiTraverser(createFile("\n")).filter(CsvRecord.class).first();
    }

    public @Nullable PsiElement createComment(@NotNull String text) {
        return ReadAction.compute(() -> createCommentInternal(text));
    }

    public @Nullable PsiElement createCommentFromContent(@NotNull String content) {
        return createComment(CsvEditorSettings.getInstance().getCommentIndicator() + content);
    }

    private @Nullable PsiElement createCommentInternal(@NotNull String text) {
        String commentText = !isIndicatingComment(text) ? CsvEditorSettings.getInstance().getCommentIndicator() + text : text;
        return PsiHelper.findFirst(createFile(commentText), CsvTypes.COMMENT);
    }

    public Document getDocument() {
        return ReadAction.compute(this::getDocumentInternal);
    }

    private Document getDocumentInternal() {
        PsiFile psiFile = getPsiFile();
        if (psiFile == null) return null;

        return PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
    }

    public PsiFile getPsiFile() {
        return myPsiFileHolder.getPsiFile();
    }

    @Override
    public void dispose() {
        synchronized (this) {
            myUncommittedActions.clear();
        }
        myCommitListeners.clear();
        myFileFactory = null;
        myFileType = null;
        Suspendable.super.dispose();
    }

    public @Nullable PsiElement createValueSeparator() {
        return ReadAction.compute(this::createValueSeparatorInternal);
    }

    private @Nullable PsiElement createValueSeparatorInternal() {
        return PsiHelper.findFirst(createFile(CsvHelper.getValueSeparator(getPsiFile()).getCharacter()), CsvTypes.COMMA);
    }

    public @Nullable PsiElement createLineBreak() {
        return ReadAction.compute(this::createLineBreakInternal);
    }

    private @Nullable PsiElement createLineBreakInternal() {
        return PsiHelper.findFirst(createFile("\n"), CsvTypes.CRLF);
    }

    public synchronized void doAction(PsiAction action) {
        myUncommittedActions.add(action);
    }

    /**
     * Queues document replacements that are applied as one document change when committed.
     */
    public void replaceTexts(@NotNull List<Pair<TextRange, String>> replacements) {
        if (replacements.isEmpty()) return;
        doAction(new DocumentPsiAction(getPsiFile(), replacements));
    }

    public void appendEmptyFields(@NotNull PsiElement anchor, int no) {
        if (no < 1) return;
        ReadAction.run(() -> {
            for (int i = 0; i < no; ++i) {
                appendFieldInternal(anchor, "", false, false);
            }
        });
    }

    public void appendField(@NotNull PsiElement anchor) {
        appendField(anchor, "", false, false);
    }

    public void appendField(@NotNull PsiElement anchor, boolean before) {
        appendField(anchor, "", false, before);
    }

    public void appendField(@NotNull PsiElement anchor, String text, boolean enquoteCommentIndicator) {
        appendField(anchor, text, enquoteCommentIndicator, false);
    }

    public void appendField(@NotNull PsiElement anchor, String text, boolean enquoteCommentIndicator, boolean before) {
        ReadAction.run(() -> appendFieldInternal(anchor, text, enquoteCommentIndicator, before));
    }

    private void appendFieldInternal(@NotNull PsiElement anchor, String text, boolean enquoteCommentIndicator, boolean before) {
        PsiElement commentOrField = anchor;
        while (commentOrField != null && !(commentOrField instanceof CsvField || CsvHelper.isCommentElement(commentOrField))) {
            commentOrField = commentOrField.getParent();
        }
        // no columns in comment row
        if (CsvHelper.isCommentElement(commentOrField)) return;
        assert commentOrField instanceof CsvField;
        doAddField(commentOrField, text, enquoteCommentIndicator, before);
        doAddValueSeparator(commentOrField, before);
    }

    public void removeField(@NotNull PsiElement field) {
        ReadAction.run(() -> removeFieldInternal(field));
    }

    private void removeFieldInternal(@NotNull PsiElement field) {
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
        PsiFile psiFile = getPsiFile();
        doAction(new DocumentPsiAction(
                psiFile,
                () -> collectColumnReplacements(psiFile, columnIndex, before)));
    }

    private List<Pair<TextRange, String>> collectColumnReplacements(
            @NotNull PsiFile psiFile,
            int columnIndex,
            boolean before) {
        List<Pair<TextRange, String>> replacements = new ArrayList<>();
        String valueSeparator = CsvHelper.getValueSeparator(psiFile).getCharacter();
        for (PsiElement record = psiFile.getFirstChild(); record != null; record = record.getNextSibling()) {
            if (!(record instanceof CsvRecord)) continue;
            // The grammar exposes the record after a terminal line break as an empty PSI record.
            // It is not a user row and must not receive a field when a column is inserted.
            if (record.getTextLength() == 0 && record.getNextSibling() == null) continue;
            PsiElement field = record.getFirstChild();
            if (CsvHelper.isCommentElement(field)) continue;

            field = PsiHelper.getNextNthSiblingOfType(field, columnIndex, CsvField.class);
            if (field == null) continue;

            int startOffset = before ? field.getTextRange().getStartOffset() : field.getTextRange().getEndOffset();
            replacements.add(Pair.create(TextRange.create(startOffset, startOffset), valueSeparator));
        }
        return replacements;
    }

    /**
     * This can be a heavy operation on PsiFile, so it will be executed on the document itself.
     */
    public void deleteColumns(Collection<Integer> indices) {
        if (indices.isEmpty()) return;
        ReadAction.run(() -> deleteColumnsInternal(indices));
    }

    public void reorderRows(int[] order) {
        reorder(order, false);
    }

    public void reorderColumns(int[] order) {
        reorder(order, true);
    }

    private void reorder(int[] order, boolean columns) {
        ReadAction.run(() -> {
            PsiFile file = getPsiFile();
            Document document = getDocumentInternal();
            if (file == null || document == null) return;
            CsvTableReorder.Change change = CsvTableReorder.prepare(document.getImmutableCharSequence(),
                    CsvLexerFactory.getInstance().createLexer(file), order, columns, CsvHelper.getValueSeparator(file),
                    CsvHelper.getEscapeCharacter(file), CsvEditorSettings.getInstance().getCommentIndicator());
            if (change != null) {
                replaceTexts(Collections.singletonList(Pair.create(change.range(), change.text())));
            }
        });
    }

    private void deleteColumnsInternal(Collection<Integer> indices) {
        PsiFile psiFile = getPsiFile();
        int columnCount = CsvTableModel.getColumnCount(psiFile);
        List<Integer> distinctIndices = indices.stream().distinct().collect(Collectors.toList());
        if (distinctIndices.size() == columnCount) {
            deleteContentInternal();
            return;
        }

        List<Pair<TextRange, String>> replacements = collectRangesToDelete(indices);
        replaceTexts(replacements);
    }

    private List<Pair<TextRange, String>> collectRangesToDelete(Collection<Integer> indices) {
        Set<PsiElement> toDelete = new LinkedHashSet<>();
        List<Integer> sortedIndices = indices.stream().distinct().sorted().collect(Collectors.toList());
        for (PsiElement record = getPsiFile().getFirstChild(); record != null; record = record.getNextSibling()) {
            if (!(record instanceof CsvRecord)) continue;
            if (CsvHelper.isCommentElement(record.getFirstChild())) continue;
            for (int columnIndex : sortedIndices) {
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
        ReadAction.run(() -> addRowInternal(anchor, before));
    }

    private void addRowInternal(@NotNull PsiElement anchor, boolean before) {
        PsiElement record = anchor;
        while (record != null && !(record instanceof CsvRecord)) {
            record = record.getParent();
        }
        assert record != null;
        doAction(new AddSiblingPsiAction(record, createRecordInternal(), before));
        doAddLineBreak(record, before);
    }

    public void deleteRow(@NotNull PsiElement row) {
        ReadAction.run(() -> deleteRowInternal(row));
    }

    private void deleteRowInternal(@NotNull PsiElement row) {
        assert row instanceof CsvRecord;
        PsiElement lf = PsiTreeUtil.findSiblingBackward(row, CsvTypes.CRLF, null);
        if (lf == null) lf = PsiTreeUtil.findSiblingForward(row, CsvTypes.CRLF, null);
        // no lf means only one record exists - this is a must, so don't delete it
        if (lf == null) doAction(new ReplacePsiAction(row, createRecordInternal()));
        else {
            doAction(new DeletePsiAction(row));
            doAction(new DeletePsiAction(lf));
        }
    }

    public void deleteRows(Collection<Integer> indices) {
        ReadAction.run(() -> deleteRowsInternal(indices));
    }

    private void deleteRowsInternal(Collection<Integer> indices) {
        PsiFile psiFile = getPsiFile();
        if (psiFile == null) return;

        Set<PsiElement> toDelete = new HashSet<>();
        List<Integer> sortedIndices = indices.stream().distinct().sorted().collect(Collectors.toList());
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
                doAction(new ReplacePsiAction(row, createRecordInternal()));
            }
        }
        delete(toDelete.toArray(new PsiElement[0]));
    }

    public void replaceComment(@NotNull PsiElement toReplace, @Nullable String textArg) {
        ReadAction.run(() -> replaceCommentInternal(toReplace, textArg));
    }

    private void replaceCommentInternal(@NotNull PsiElement toReplace, @Nullable String textArg) {
        assert PsiHelper.getElementType(toReplace) == CsvTypes.COMMENT;
        String text = textArg == null ? "" : textArg;
        // do not replace if not necessary
        if (toReplace.getText().equals(text)) return;

        doAction(new ReplacePsiAction(toReplace, createCommentInternal(text)));
    }

    public void replaceField(@NotNull PsiElement toReplace, @Nullable String textArg, boolean enquoteCommentIndicator) {
        ReadAction.run(() -> replaceFieldInternal(toReplace, textArg, enquoteCommentIndicator));
    }

    private void replaceFieldInternal(@NotNull PsiElement toReplace, @Nullable String textArg, boolean enquoteCommentIndicator) {
        assert toReplace instanceof CsvField;
        String text = textArg == null ? "" : textArg;
        // do not replace if not necessary
        if (toReplace.getText().equals(text)) return;

        doAction(new ReplacePsiAction(toReplace, createFieldInternal(text, enquoteCommentIndicator)));
    }

    public void delete(@NotNull PsiElement... toRemove) {
        for (PsiElement element : toRemove) {
            doAction(new DeletePsiAction(element));
        }
    }

    public void deleteAllChildren(@NotNull PsiElement anchor) {
        doAction(new DeleteChildrenPsiAction(anchor));
    }

    public void deleteContent() {
        ReadAction.run(this::deleteContentInternal);
    }

    private void deleteContentInternal() {
        PsiFile psiFile = getPsiFile();
        deleteAllChildren(psiFile);
        // even an empty file has a row with a field
        doAction(new AddChildPsiAction(psiFile, createRecordInternal()));
    }

    private void doAddValueSeparator(@NotNull PsiElement anchor, boolean before) {
        doAction(new AddSiblingPsiAction(anchor, createValueSeparatorInternal(), before));
    }

    private void doAddLineBreak(@NotNull PsiElement anchor, boolean before) {
        doAction(new AddSiblingPsiAction(anchor, createLineBreakInternal(), before));
    }

    private void doAddField(@NotNull PsiElement anchor, @NotNull String text, boolean enquoteCommentIndicator, boolean before) {
        doAction(new AddSiblingPsiAction(anchor, createFieldInternal(text, enquoteCommentIndicator), before));
    }

    public synchronized void commit() {
        if (isSuspended() || myUncommittedActions.isEmpty()) return;

        PsiFile psiFile = getPsiFile();
        Document document = getDocument();

        if (psiFile == null ||
                !psiFile.isWritable() ||
                document == null ||
                !document.isWritable() ||
                document.isInBulkUpdate()) {
            return; // Keep actions queued for a later commit.
        }

        List<PsiAction> actionsToCommit = new ArrayList<>(myUncommittedActions);
        myUncommittedActions.clear();

        suspend();
        try {
            ApplicationManager.getApplication().runWriteAction(() ->
                    doCommit(
                            psiFile,
                            document,
                            () -> actionsToCommit.forEach(PsiAction::execute)));
        } finally {
            resume();
            fireCommitted();
        }
    }

    private void doCommit(
            @NotNull PsiFile psiFile,
            @NotNull Document document,
            @NotNull Runnable runnable) {
        CommandProcessor.getInstance().executeCommand(
                psiFile.getProject(),
                () -> DocumentUtil.executeInBulk(document, runnable),
                "CSV Editor Changes",
                null,
                document);
    }

    public void addCommitListener(CommitListener l) {
        if (l != null) myCommitListeners.add(l);
    }

    public void removeCommitListener(CommitListener l) {
        myCommitListeners.remove(l);
    }

    protected void fireCommitted() {
        for (CommitListener listener : myCommitListeners) {
            listener.committed();
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

        private final Supplier<List<Pair<TextRange, String>>> myReplacementsSupplier;

        DocumentPsiAction(@NotNull PsiElement psiFile, List<Pair<TextRange, String>> replacements) {
            this(psiFile, () -> replacements);
        }

        DocumentPsiAction(
                @NotNull PsiElement psiFile,
                @NotNull Supplier<List<Pair<TextRange, String>>> replacementsSupplier) {
            super(psiFile);
            assert psiFile instanceof PsiFile;
            myReplacementsSupplier = replacementsSupplier;
        }

        @Override
        public void execute() {
            PsiFile psiFile = (PsiFile) getAnchor();

            PsiDocumentManager manager = PsiDocumentManager.getInstance(psiFile.getProject());
            Document document = manager.getDocument(psiFile);
            if (document == null) return;

            manager.doPostponedOperationsAndUnblockDocument(document);

            List<Pair<TextRange, String>> replacements = myReplacementsSupplier.get();
            if (replacements.isEmpty()) return;
            replacements.sort(Comparator.comparingInt(replacement -> replacement.getFirst().getStartOffset()));

            // Build the changed span first so listeners receive a single document event,
            // even when a batched operation affects thousands of records.
            CharSequence original = document.getImmutableCharSequence();
            int startOffset = replacements.getFirst().getFirst().getStartOffset();
            int endOffset = replacements.getLast().getFirst().getEndOffset();
            StringBuilder updated = new StringBuilder(endOffset - startOffset);
            int offset = startOffset;
            for (Pair<TextRange, String> replacement : replacements) {
                TextRange textRange = replacement.getFirst();
                updated.append(original, offset, textRange.getStartOffset());
                updated.append(replacement.getSecond());
                offset = textRange.getEndOffset();
            }
            document.replaceString(startOffset, endOffset, updated);

            manager.commitDocument(document);
        }
    }
}
