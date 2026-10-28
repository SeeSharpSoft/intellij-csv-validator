package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter;
import com.intellij.psi.PsiTreeChangeListener;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CsvTableModelBase<T extends PsiFileHolder> implements CsvTableModel {
    public record ModelState(PsiFile file, long stamp, int rows, int columns, Set<Integer> comments,
                             List<String> headers, boolean errors) {
    }

    private record RowMetadata(int rows, int columns, Set<Integer> comments) {
    }

    private final T myPsiFileHolder;
    private volatile boolean myDisposed = false;

    private int myCachedRowCount = -1;
    private int myCachedColumnCount = -1;
    private CsvEscapeCharacter myCachedEscapeCharacter;
    private Boolean myCachedHasErrors = null;
    private int myPointedRow = -1;
    private long myPointedStamp = -1;
    private PsiElement myPointedRecord = null;
    private volatile Set<Integer> myCachedCommentRows = Collections.emptySet();
    private final CsvPsiTreeUpdater myPsiTreeUpdater;
    private volatile PsiFile myObservedPsiFile;
    private PsiManager myPsiManager;

    private final PsiTreeChangeListener myPsiTreeChangeListener = new PsiTreeAnyChangeAbstractAdapter() {
        @Override
        protected void onChange(@Nullable PsiFile file) {
            onPsiTreeChanged(file);
        }
    };

    public CsvTableModelBase(@NotNull T psiFileHolder) {
        myPsiFileHolder = psiFileHolder;
        myPsiTreeUpdater = new CsvPsiTreeUpdater(psiFileHolder);
        myPsiTreeUpdater.addCommitListener(this::onUpdaterCommitted);
        updatePsiFile(getPsiFile());
        resetPointer();
        getRowCount();
    }

    public T getPsiFileHolder() {
        return myPsiFileHolder;
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    private void updatePsiFile(@Nullable PsiFile psiFile) {
        myObservedPsiFile = psiFile;
        invalidateModelState();
        updatePsiListener(psiFile);
    }

    private void updatePsiListener(@Nullable PsiFile psiFile) {
        PsiManager newManager = psiFile == null ? null : psiFile.getManager();
        if (newManager == myPsiManager) return;

        detachPsiListener();
        if (newManager != null && !myDisposed) {
            newManager.addPsiTreeChangeListener(myPsiTreeChangeListener, myPsiFileHolder);
            myPsiManager = newManager;
        }
    }

    private void detachPsiListener() {
        if (myPsiManager != null) {
            myPsiManager.removePsiTreeChangeListener(myPsiTreeChangeListener);
            myPsiManager = null;
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;
        detachPsiListener();
        myObservedPsiFile = null;
        invalidateModelState();
        CsvTableModel.super.dispose();
        myPsiTreeUpdater.dispose();
    }

    private void onPsiTreeChanged(@Nullable PsiFile file) {
        if (myDisposed || !canNotifyUpdates()) return;

        PsiFile currentPsiFile = getPsiFile();
        if (file != null && !isSameVirtualFile(file, currentPsiFile)) {
            return;
        }

        notifyUpdate();
    }

    private boolean canNotifyUpdates() {
        return !myDisposed && !myPsiTreeUpdater.isSuspended() && !isSuspended();
    }

    private void onUpdaterCommitted() {
        if (canNotifyUpdates()) notifyUpdate();
    }

    private static boolean isSameVirtualFile(@Nullable PsiFile first, @Nullable PsiFile second) {
        if (first == second) return true;
        if (first == null || second == null) return false;
        return first.getVirtualFile() != null && first.getVirtualFile().equals(second.getVirtualFile());
    }

    @Override
    public PsiFile getPsiFile() {
        PsiFile psiFile = myPsiFileHolder.getPsiFile();
        if (psiFile != myObservedPsiFile) {
            updatePsiFile(psiFile);
        }
        return psiFile;
    }

    @Override
    public boolean isCommentRow(int rowIndex) {
        getRowCount();
        return myCachedCommentRows.contains(rowIndex);
    }

    @Override
    public void notifyUpdate() {
        invalidateModelState();
        resetPointer();
        getRowCount();
    }

    /** Captures model metadata under a background read action, without changing the displayed model. */
    public ModelState captureState() {
        PsiFile file = myPsiFileHolder.getPsiFile();
        if (file == null) throw new IllegalStateException("CSV file is no longer available");
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) throw new IllegalStateException("CSV document is no longer available");
        RowMetadata metadata = readRowMetadata(file);
        int[] visited = {0};
        boolean errors = !PsiTreeUtil.processElements(file, element -> {
            if ((visited[0]++ & 1023) == 0) ProgressManager.checkCanceled();
            return !(element instanceof PsiErrorElement);
        });
        return new ModelState(file, document.getModificationStamp(), metadata.rows(), metadata.columns(),
                metadata.comments(), readHeaderTexts(file), errors);
    }

    /** Reads metadata for the records that can be displayed, stopping at a top-level parse error. */
    private static RowMetadata readRowMetadata(@Nullable PsiFile file) {
        int rows = 0;
        int columns = 0;
        Set<Integer> comments = new HashSet<>();
        for (PsiElement child = file == null ? null : file.getFirstChild();
             child != null; child = child.getNextSibling()) {
            if ((rows & 1023) == 0) ProgressManager.checkCanceled();
            if (child instanceof PsiErrorElement) break;
            if (child instanceof CsvRecord record) {
                if (CsvHelper.isCommentElement(record.getFirstChild())) comments.add(rows);
                columns = Math.max(columns, PsiTreeUtil.countChildrenOfType(record, CsvField.class));
                rows++;
            }
        }
        return new RowMetadata(rows, comments.isEmpty() ? columns : Math.max(1, columns), Set.copyOf(comments));
    }

    /** Decodes the first non-comment record's headers; the caller must hold a read action. */
    protected static List<String> readHeaderTexts(@Nullable PsiFile file) {
        PsiElement field = PsiHelper.findFirst(file, CsvTypes.FIELD);
        if (field == null) return List.of();
        CsvEscapeCharacter escapeCharacter = CsvHelper.getEscapeCharacter(file);
        List<String> headers = new ArrayList<>();
        for (; field != null; field = field.getNextSibling()) {
            if (field instanceof CsvField) {
                headers.add(CsvHelper.unquoteCsvValue(field.getText(), escapeCharacter).trim());
            }
        }
        return List.copyOf(headers);
    }

    protected boolean applyState(ModelState state) {
        PsiFile file = getPsiFile();
        if (file != state.file()) return false;
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null || document.getModificationStamp() != state.stamp()) return false;
        invalidateModelState();
        myCachedRowCount = state.rows();
        myCachedColumnCount = state.columns();
        myCachedCommentRows = state.comments();
        myCachedHasErrors = state.errors();
        resetPointer();
        return true;
    }

    private void invalidateModelState() {
        myCachedRowCount = -1;
        myCachedColumnCount = -1;
        myCachedHasErrors = null;
        myCachedEscapeCharacter = null;
        myCachedCommentRows = Collections.emptySet();
        myPointedRecord = null;
        myPointedRow = 0;
    }

    private void resetPointer() {
        ReadAction.run(() -> {
            PsiFile psiFile = getPsiFile();
            myPointedRecord = psiFile == null ? null : PsiHelper.getFirstChildOfType(psiFile, CsvRecord.class);
            myPointedRow = 0;
            myPointedStamp = psiFile == null ? -1 : psiFile.getModificationStamp();
        });
    }

    protected CsvPsiTreeUpdater getPsiTreeUpdater() {
        return myPsiTreeUpdater;
    }

    protected CsvEscapeCharacter getEscapeCharacter() {
        if (myCachedEscapeCharacter == null) {
            myCachedEscapeCharacter = CsvHelper.getEscapeCharacter(getPsiFile());
        }
        return myCachedEscapeCharacter;
    }

    @Override
    public PsiElement getFieldAt(int row, int column) {
        return ReadAction.compute(() -> {
            PsiFile psiFile = getPsiFile();
            if (psiFile == null) return null;
            long stamp = psiFile.getModificationStamp();
            if (myPointedStamp != stamp) {
                myPointedRecord = null;
                myPointedStamp = stamp;
            }

            int diffToCurrent = Math.abs(myPointedRow - row);
            if (diffToCurrent > row || myPointedRecord == null || !myPointedRecord.isValid()) {
                myPointedRecord = PsiHelper.getFirstChildOfType(psiFile, CsvRecord.class);
                myPointedRow = 0;
                diffToCurrent = row;
            }
            if (myPointedRecord == null) return null;

            CsvRecord record = PsiHelper.getNthSiblingOfType(myPointedRecord, diffToCurrent, CsvRecord.class, myPointedRow > row);
            if (record == null) return null;

            myPointedRecord = record;
            myPointedRow = row;

            if (PsiHelper.getElementType(record.getFirstChild()) == CsvTypes.COMMENT) return record.getFirstChild();

            return PsiHelper.getNthChildOfType(record, column, CsvField.class);
        });
    }

    @Override
    public boolean hasErrors() {
        getPsiFile();
        if (myCachedHasErrors == null) {
            myCachedHasErrors = ReadAction.compute(() -> {
                PsiFile psiFile = getPsiFile();
                return psiFile != null && PsiTreeUtil.hasErrorElements(psiFile);
            });
        }
        return myCachedHasErrors;
    }

    @Override
    public int getRowCount() {
        getPsiFile();
        if (myCachedRowCount == -1) {
            myCachedRowCount = ReadAction.compute(() -> {
                RowMetadata metadata = readRowMetadata(getPsiFile());
                myCachedColumnCount = metadata.columns();
                myCachedCommentRows = metadata.comments();
                return metadata.rows();
            });
        }
        return myCachedRowCount;
    }

    @Override
    public int getColumnCount() {
        getRowCount();
        return myCachedColumnCount;
    }

    @Override
    public void setValue(String value, int rowIndex, int columnIndex) {
        setValueAt(value, rowIndex, columnIndex, true);
    }

    private int getColumnCount(int rowIndex) {
        PsiFile psiFile = getPsiFile();
        if (psiFile == null) return 0;
        return getColumnCount(PsiHelper.getNthChildOfType(psiFile, rowIndex, CsvRecord.class));
    }

    private int getColumnCount(PsiElement record) {
        if (record == null) return -1;
        return PsiTreeUtil.countChildrenOfType(record, CsvField.class);
    }

    public void setValueAt(String value, int rowIndex, int columnIndex, boolean commitImmediately) {
        ReadAction.run(() -> prepareSetValueAt(value, rowIndex, columnIndex));
        if (commitImmediately) getPsiTreeUpdater().commit();
    }

    public void setValuesAt(List<List<String>> values, Set<Integer> commentRows, int rowIndex, int columnIndex) {
        Pair<TextRange, String> replacement = ReadAction.compute(() -> {
            PsiFile file = getPsiFile();
            List<CsvRecord> records = PsiTreeUtil.getChildrenOfTypeAsList(file, CsvRecord.class);
            if (rowIndex < 0 || rowIndex >= records.size() || columnIndex < 0 || values.isEmpty()) {
                throw new IllegalArgumentException("Invalid paste position or empty grid");
            }
            String source = file.getText();
            StringBuilder result = new StringBuilder();
            int start = records.get(rowIndex).getTextOffset();
            int end = start;
            String separator = CsvHelper.getValueSeparator(file).getCharacter();
            String comment = CsvEditorSettings.getInstance().getCommentIndicator();
            for (int index = 0; index < values.size(); index++) {
                int targetRow = rowIndex + index;
                List<String> row = values.get(index);
                if (row.isEmpty()) throw new IllegalArgumentException("Empty clipboard row");
                boolean pastedComment = commentRows.contains(index);
                if (pastedComment && (columnIndex != 0 || row.size() != 1 || row.getFirst().contains("\n") || comment.isEmpty())) {
                    throw new IllegalArgumentException("A clipboard comment requires a single-line cell in the first column");
                }
                if (targetRow < records.size()) {
                    CsvRecord record = records.get(targetRow);
                    result.append(source, end, record.getTextOffset());
                    end = record.getTextRange().getEndOffset();
                    if (pastedComment) {
                        result.append(comment).append(row.getFirst());
                        continue;
                    }
                    if (CsvHelper.isCommentElement(record.getFirstChild())) {
                        if (columnIndex != 0 || row.size() != 1 || row.getFirst().contains("\n")) {
                            throw new IllegalArgumentException("A comment row has only one single-line cell");
                        }
                        result.append(comment).append(row.getFirst());
                        continue;
                    }
                    List<CsvField> fields = PsiTreeUtil.getChildrenOfTypeAsList(record, CsvField.class);
                    if (columnIndex < fields.size()) {
                        result.append(source, record.getTextOffset(), fields.get(columnIndex).getTextOffset());
                    } else {
                        result.append(record.getText());
                        result.repeat(separator, columnIndex - Math.max(0, fields.size() - 1));
                    }
                    appendPastedValues(result, row, columnIndex, file, comment);
                    int lastColumn = columnIndex + row.size() - 1;
                    if (lastColumn < fields.size()) {
                        result.append(source, fields.get(lastColumn).getTextRange().getEndOffset(), end);
                    }
                } else {
                    result.append('\n');
                    if (pastedComment) result.append(comment).append(row.getFirst());
                    else {
                        result.repeat(separator, columnIndex);
                        appendPastedValues(result, row, columnIndex, file, comment);
                    }
                }
            }
            return Pair.create(new TextRange(start, end), result.toString());
        });
        getPsiTreeUpdater().replaceTexts(Collections.singletonList(replacement));
        getPsiTreeUpdater().commit();
    }

    private void appendPastedValues(StringBuilder result, List<String> values, int columnIndex, PsiFile file, String comment) {
        var separator = CsvHelper.getValueSeparator(file);
        var escape = CsvHelper.getEscapeCharacter(file);
        boolean quoting = CsvEditorSettings.getInstance().isQuotingEnforced();
        for (int column = 0; column < values.size(); column++) {
            if (column > 0) result.append(separator.getCharacter());
            String value = values.get(column);
            result.append(CsvHelper.quoteCsvField(value, escape, separator,
                    quoting || columnIndex + column == 0 && !comment.isEmpty() && value.trim().startsWith(comment)));
        }
    }

    private void prepareSetValueAt(String value, int rowIndex, int columnIndex) {
        PsiElement field = getFieldAt(rowIndex, columnIndex);
        CsvPsiTreeUpdater updater = getPsiTreeUpdater();
        if (field == null) {
            int currentColumnCount = getColumnCount(rowIndex);
            field = getFieldAt(rowIndex, currentColumnCount - 1);
            if (field != null) {
                updater.appendField(field, value, true);
                updater.appendEmptyFields(field, columnIndex - currentColumnCount);
            }
        } else {
            if (CsvHelper.isCommentElement(field)) {
                updater.replaceComment(field, value);
            } else {
                updater.replaceField(field, value, columnIndex == 0);
            }
        }
    }

    @Override
    public @NotNull String getValue(int rowIndex, int columnIndex) {
        return ReadAction.compute(() -> {
            PsiElement field = getFieldAt(rowIndex, columnIndex);
            return CsvHelper.getFieldValue(field, getEscapeCharacter());
        });
    }

    @Override
    public void addRow(int anchorRowIndex, boolean before) {
        ReadAction.run(() -> {
            CsvRecord row = PsiHelper.getNthChildOfType(getPsiFile(), anchorRowIndex, CsvRecord.class);
            if (row == null) return;
            getPsiTreeUpdater().addRow(row, before);
        });
        getPsiTreeUpdater().commit();
    }

    @Override
    public void removeRows(Collection<Integer> indices) {
        CsvPsiTreeUpdater updater = getPsiTreeUpdater();
        updater.deleteRows(indices);
        updater.commit();
    }

    @Override
    public void addColumn(int anchorColumnIndex, boolean before) {
        CsvPsiTreeUpdater updater = getPsiTreeUpdater();
        updater.addColumn(anchorColumnIndex, before);
        updater.commit();
    }

    @Override
    public void removeColumns(Collection<Integer> indices) {
        CsvPsiTreeUpdater updater = getPsiTreeUpdater();
        updater.deleteColumns(indices);
        updater.commit();
    }

    @Override
    public void reorderRows(int[] order) {
        getPsiTreeUpdater().reorderRows(order);
        getPsiTreeUpdater().commit();
    }

    @Override
    public void reorderColumns(int[] order) {
        getPsiTreeUpdater().reorderColumns(order);
        getPsiTreeUpdater().commit();
    }

    @Override
    public void clearCells(Collection<Integer> rows, Collection<Integer> columns) {
        ReadAction.run(() -> {
            for (int currentColumn : columns) {
                for (int currentRow : rows) {
                    prepareSetValueAt("", currentRow, currentColumn);
                }
            }
        });
        getPsiTreeUpdater().commit();
    }
}
