package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.table.api.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class CsvTableModelBase implements CsvTableModel {
    private CsvTableEditor myEditor = null;

    private int myCachedRowCount = -1;
    private int myCachedColumnCount = -1;
    private Boolean myCachedHasErrors = null;

    private int myPointerRow = -1;
    private CsvField myPointerElement = null;

    private DocumentUpdater myDocumentUpdater = new DocumentUpdater();

    public CsvTableModelBase(@NotNull CsvTableEditor editor) {
        myEditor = editor;
    }

    @Override
    public void notifyUpdate() {
        this.getDocumentUpdater().clearCache();
        this.resetCachedValues();
        this.resetPointer();
    }

    private void resetCachedValues() {
        myCachedRowCount = -1;
        myCachedColumnCount = -1;
        myCachedHasErrors = null;
    }

    private CsvField resetPointer() {
        myPointerElement = PsiTreeUtil.findChildOfType(this.getEditor().getCsvFile(), CsvField.class);
        myPointerRow = 0;
        return myPointerElement;
    }

    protected DocumentUpdater getDocumentUpdater() {
        return myDocumentUpdater;
    }

    @Override
    public PsiElement getFieldAt(int row, int column) {
        int diffToCurrent = Math.abs(myPointerRow - row);
        if (diffToCurrent > row) {
            resetPointer();
            diffToCurrent = row;
        }

        CsvRecord record = PsiHelper.getNthSiblingOfType(myPointerElement.getParent(), diffToCurrent, CsvRecord.class, myPointerRow > row);
        if (record == null) return null;

        if (PsiHelper.getElementType(record.getFirstChild()) == CsvTypes.COMMENT) return record.getFirstChild();

        CsvField field = PsiHelper.getNthChildOfType(record, column, CsvField.class);
        if (field == null) return null;

        myPointerElement = field;
        myPointerRow = row;
        return myPointerElement;
    }

    @Override
    public CsvTableEditor getEditor() {
        return myEditor;
    }

    @Override
    public boolean hasErrors() {
        if (myCachedHasErrors == null) {
            myCachedHasErrors = CsvTableModel.super.hasErrors();
        }
        return myCachedHasErrors;
    }

    @Override
    public int getRowCount() {
        if (myCachedRowCount == -1) {
            myCachedRowCount = CsvTableModel.super.getRowCount();
        }
        return myCachedRowCount;
    }

    @Override
    public int getColumnCount() {
        if (myCachedColumnCount == -1) {
            myCachedColumnCount = CsvTableModel.super.getColumnCount();
        }
        return myCachedColumnCount;
    }

    private String sanitizeFieldValue(Object value) {
        return sanitizeFieldValue(value, CsvEditorSettings.getInstance().isQuotingEnforced());
    }

    private String sanitizeFieldValue(Object value, boolean enforceQuoting) {
        if (value == null) {
            return "";
        }
        return CsvHelper.quoteCsvField(value.toString(), this.getEscapeCharacter(), this.getValueSeparator(), enforceQuoting);
    }

    @Override
    public void setValueAt(String value, int rowIndex, int columnIndex) {
        setValueAt(value, rowIndex, columnIndex, true);
    }

    private TextRange getNewFieldOffsetAtIndex(int rowIndex) {
        return getNewFieldOffsetAtIndex(getFieldAt(rowIndex, 0));
    }

    private TextRange getNewFieldOffsetAtIndex(@Nullable PsiElement firstRowElement) {
        return getNewFieldOffsetAtIndex(firstRowElement, getColumnCount());
    }

    private TextRange getNewFieldOffsetAtIndex(@Nullable PsiElement firstRowElement, int targetColumnCount) {
        if (firstRowElement == null || CsvHelper.isCommentElement(firstRowElement)) return TextRange.EMPTY_RANGE;

        int thisColumnCount = 0;
        while (thisColumnCount < targetColumnCount) {
            PsiElement nextField = PsiTreeUtil.getNextSiblingOfType(firstRowElement, CsvField.class);
            if (nextField == null) {
                int noOfSeparators = targetColumnCount - thisColumnCount - 1;
                return TextRange.from(firstRowElement.getTextRange().getEndOffset(), noOfSeparators);
            }
            firstRowElement = nextField;
            ++thisColumnCount;
        }
        return TextRange.EMPTY_RANGE;
    }

    private OffsetTextRange createAdditionalOffsetRange(@NotNull TextRange additionalColumnInsertRange, int columnIndex) {
        int noOfInsertedSeparators = additionalColumnInsertRange.getLength();
        int startColIndex = getColumnCount() - noOfInsertedSeparators - 1;
        int additionalOffset = (columnIndex - startColIndex) * getValueSeparator().getCharacter().length();
        return new OffsetTextRange(additionalColumnInsertRange.getStartOffset() + additionalOffset, - noOfInsertedSeparators);
    }

    private TextRange createMissingColumns(int rowIndex) {
        return createMissingColumns(getFieldAt(rowIndex, 0));
    }

    private TextRange createMissingColumns(PsiElement firstRowElement) {
        return createMissingColumns(firstRowElement, getColumnCount());
    }

    private TextRange createMissingColumns(PsiElement firstRowElement, int targetColumnCount) {
        TextRange newSeparators = getNewFieldOffsetAtIndex(firstRowElement, targetColumnCount);
        // add value separators
        getDocumentUpdater().replace(
                new OffsetTextRange(newSeparators.getStartOffset(), 0),
                getValueSeparator().getCharacter().repeat(newSeparators.getLength()));
        return newSeparators;
    }

    public void setValueAt(String value, int rowIndex, int columnIndex, boolean commitImmediately) {
        PsiElement field = getFieldAt(rowIndex, columnIndex);
        if (field == null) {
            // no field means we need to add value separators
            TextRange newSeparators = createMissingColumns(rowIndex);
            // insert actual value (to be cached separately)
            getDocumentUpdater().replace(
                    createAdditionalOffsetRange(newSeparators, columnIndex),
                    sanitizeFieldValue(value),
                    commitImmediately);
        } else {
            String sanitizedValue = value;
            if (!CsvHelper.isCommentElement(field)) {
                sanitizedValue = sanitizeFieldValue(value);
            }

            getDocumentUpdater().replace(field, sanitizedValue, commitImmediately);
        }
    }

    @Override
    public String getValueAt(int rowIndex, int columnIndex) {
        PsiElement field = getFieldAt(rowIndex, columnIndex);
        String value = "";
        if (field == null) {
            TextRange newSeparators = getNewFieldOffsetAtIndex(rowIndex);
            value = getDocumentUpdater().getUpdatedValue(createAdditionalOffsetRange(newSeparators, columnIndex));
        } else {
            value = getDocumentUpdater().getUpdatedValue(field);
            if (value == null) value = field.getText();
        }
        return CsvHelper.unquoteCsvValue(value, getEscapeCharacter());
    }

    @Override
    public void addRow(int focusedRowIndex, boolean before) {
        CsvRecord row = PsiHelper.getNthChildOfType(getEditor().getCsvFile(), focusedRowIndex, CsvRecord.class);
        getDocumentUpdater().insert(before ? row.getTextOffset() : row.getTextRange().getEndOffset(), "\n");
        getDocumentUpdater().commit();
    }

    @Override
    public void removeRows(int[] indices) {
        DocumentUpdater updater = getDocumentUpdater();
        for (int rowIndex : indices) {
            CsvRecord row = PsiHelper.getNthChildOfType(getEditor().getCsvFile(), rowIndex, CsvRecord.class);
            boolean removePreviousLF = rowIndex > 0;
            PsiElement lfElement = PsiHelper.getSiblingOfType(row, CsvTypes.CRLF,removePreviousLF);
            int startOffset = removePreviousLF ? lfElement.getTextOffset() : row.getTextOffset();
            int endOffset = removePreviousLF || lfElement == null ? row.getTextRange().getEndOffset() : lfElement.getTextRange().getEndOffset();
            updater.delete(startOffset, endOffset);
        }
        updater.commit();
    }

    @Override
    public void addColumn(int focusedColumnIndex, boolean before) {
        DocumentUpdater updater = getDocumentUpdater();
        for (PsiElement record = getEditor().getCsvFile().getFirstChild(); record != null; record = record.getNextSibling()) {
            if (!CsvRecord.class.isInstance(record)) continue;
            if (CsvHelper.isCommentElement(record.getFirstChild())) continue;
            PsiElement focusedCol = PsiHelper.getNthChildOfType(record, focusedColumnIndex, CsvField.class);
            if (focusedCol == null) {
                // create missing columns,  +1 for the one to add
                createMissingColumns(record.getFirstChild(), getColumnCount() + 1);
            } else {
                updater.insert(before ? focusedCol.getTextRange().getStartOffset() : focusedCol.getTextRange().getEndOffset(), getValueSeparator().getCharacter());
            }
        }
        updater.commit();
    }

    @Override
    public void removeColumns(int[] indices) {
        for (int columnIndex : indices) {
            removeColumn(columnIndex);
        }
        getDocumentUpdater().commit();
    }

    private void removeColumn(int columnIndex) {
        DocumentUpdater updater = getDocumentUpdater();
        if (getColumnCount() == 1) {
            Document document = getEditor().getDocument();
            updater.replace(0, document.getTextLength(), "");
            return;
        }
        for (PsiElement record = getEditor().getCsvFile().getFirstChild(); record != null; record = record.getNextSibling()) {
            if (!CsvRecord.class.isInstance(record)) continue;
            if (CsvHelper.isCommentElement(record.getFirstChild())) continue;
            PsiElement focusedCol = PsiHelper.getNthChildOfType(record, columnIndex, CsvField.class);
            // if no field exists in row, we are done
            if (focusedCol != null) {
                boolean removePreviousSeparator = columnIndex > 0;
                PsiElement valueSeparator = PsiHelper.getSiblingOfType(focusedCol,CsvTypes.COMMA, removePreviousSeparator);
                int startOffset = removePreviousSeparator ? valueSeparator.getTextOffset() : focusedCol.getTextOffset();
                int endOffset = removePreviousSeparator || valueSeparator == null ? focusedCol.getTextRange().getEndOffset() : valueSeparator.getTextRange().getEndOffset();
                updater.delete(startOffset, endOffset);
            }
        }
    }

    @Override
    public void clearCells(int[] rows, int[] columns) {
        for (int currentColumn : columns) {
            for (int currentRow : rows) {
                setValueAt("", currentRow, currentColumn, false);
            }
        }
        getDocumentUpdater().commit();
    }

    static class OffsetTextRange extends TextRange {
        private int myTextRangeOffset = 0;

        public OffsetTextRange(int startAndEndOffset, int textRangeOffset) {
            this(startAndEndOffset, startAndEndOffset, textRangeOffset);
        }

        public OffsetTextRange(int startOffset, int endOffset, int textRangeOffset) {
            super(startOffset, endOffset);
            myTextRangeOffset = textRangeOffset;
        }

        public int getTextRangeOffset() {
            return myTextRangeOffset;
        }

        @NotNull
        public TextRange shiftRight(int delta) {
            if (delta == 0) return this;
            return new OffsetTextRange(getStartOffset(), getTextRangeOffset() + delta);
        }

        @NotNull
        public TextRange shiftLeft(int delta) {
            if (delta == 0) return this;
            return new OffsetTextRange(getStartOffset(), getTextRangeOffset() - delta);
        }

        @Override
        public String toString() {
            return "(" + getStartOffset() + "," + getEndOffset() + "," + getTextRangeOffset() + ")";
        }

        @Override
        public boolean contains(int offset) {
            return getStartOffset() + getTextRangeOffset() <= offset && offset < getEndOffset() + getTextRangeOffset();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof OffsetTextRange)) return false;
            OffsetTextRange range = (OffsetTextRange)obj;
            return getTextRangeOffset() == range.getTextRangeOffset() && super.equals(obj);
        }

        @Override
        public int hashCode() {
            return super.hashCode() ^ getTextRangeOffset();
        }
    }

    // TODO
    class DocumentUpdater {
        private Map<TextRange, String> uncommittedChanges = new HashMap<>();

        private Map<TextRange, String> committedChanges = new HashMap<>();

        public void clearCache() {
            committedChanges.clear();
        }

        protected TextRange getOffsetRange(@NotNull TextRange textRange) {
            int committedOffset = committedChanges.entrySet().stream()
                    .filter(entry -> entry.getKey().getStartOffset() < textRange.getStartOffset())
                    .mapToInt(entry -> entry.getValue().length() - entry.getKey().getLength())
                    .sum();

            return textRange.shiftRight(committedOffset);
        }

        public String getUpdatedValue(@NotNull PsiElement element) {
            return getUpdatedValue(element.getTextRange());
        }

        // workaround: TextRange.equals(OffsetTextRange) is considered to be equal, but it shouldn't
        private Map.Entry<TextRange, String> findMatchingRangeEntry(Map<TextRange, String> map, @NotNull TextRange range) {
            return map.entrySet().stream()
                    .filter(entry -> entry.getKey().equals(range) && entry.getKey().getClass().equals(range.getClass()))
                    .findFirst().orElse(null);
        }

        public String getUpdatedValue(@NotNull TextRange range) {
            Map.Entry<TextRange, String> matchingEntry = findMatchingRangeEntry(committedChanges, range);
            if (matchingEntry == null && uncommittedChanges.size() > 0) {
                TextRange offsetRange = getOffsetRange(range);
                matchingEntry = findMatchingRangeEntry(uncommittedChanges, offsetRange);
            }
            return matchingEntry == null ? null : matchingEntry.getValue();
        }

        public String getInsertedValue(int startOffset) {
            return getUpdatedValue(TextRange.create(startOffset, startOffset));
        }

        public void insert(int startOffset, @NotNull String value) {
            insert(startOffset, value, false);
        }

        public void insert(int startOffset, @NotNull String value, boolean commitImmediately) {
            replace(TextRange.create(startOffset, startOffset), value, commitImmediately);
        }

        public void delete(int startOffset, int endOffset) {
            delete(startOffset, endOffset, false);
        }

        public void delete(int startOffset, int endOffset, boolean commitImmediately) {
            replace(TextRange.create(startOffset, endOffset), "", commitImmediately);
        }

        public void replace(@NotNull PsiElement fieldOrComment, @NotNull String value) {
            replace(fieldOrComment, value, false);
        }

        public void replace(@NotNull PsiElement fieldOrComment, @NotNull String value, boolean commitImmediately) {
            if (fieldOrComment.textMatches(value)) {
                uncommittedChanges.remove(getOffsetRange(fieldOrComment.getTextRange()));
            } else {
                replace(fieldOrComment.getTextRange(), value, commitImmediately);
            }
        }

        public void replace(int startOffset, int endOffset, @NotNull String value) {
            replace(TextRange.create(startOffset, endOffset), value);
        }

        public void replace(@NotNull TextRange range, @NotNull String value) {
            replace(range, value, false);
        }

        public void replace(@NotNull TextRange range, @NotNull String value, boolean commitImmediately) {
            uncommittedChanges.put(getOffsetRange(range), value);
            if (commitImmediately) {
                commit();
            }
        }

        private int getActualStartOffset(@NotNull TextRange textRange) {
            int startOffset = textRange.getStartOffset();
            if (textRange instanceof OffsetTextRange) {
                startOffset += ((OffsetTextRange) textRange).getTextRangeOffset();
            }
            return startOffset;
        }

        private int getActualEndOffset(@NotNull TextRange textRange) {
            int endOffset = textRange.getEndOffset();
            if (textRange instanceof OffsetTextRange) {
                endOffset += ((OffsetTextRange) textRange).getTextRangeOffset();
            }
            return endOffset;
        }

        public synchronized void commit() {
            AtomicInteger offset = new AtomicInteger(0);
            List<Consumer<Document>> updates =
                    uncommittedChanges.entrySet().stream()
                            .sorted(Comparator.comparingInt(entry -> entry.getKey().getStartOffset()))
                            .map(entry -> (Consumer<Document>) (Document document) -> {
                                TextRange textRange = entry.getKey();
                                document.replaceString(
                                        Math.min(getActualStartOffset(textRange) + offset.get(), document.getTextLength()),
                                        Math.min(getActualEndOffset(textRange) + offset.get(), document.getTextLength()),
                                        entry.getValue());
                                offset.set(offset.get() + entry.getValue().length() - textRange.getLength());
                            })
                            .collect(Collectors.toList());

            if (commit(updates)) {
                committedChanges.putAll(uncommittedChanges);
                uncommittedChanges.clear();
            }
        }

        private boolean commit(List<Consumer<Document>> updates) {
            return CsvTableModelBase.this.getEditor().doUpdateDocument(updates);
        }
    }
}
