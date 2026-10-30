package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import java.util.List;
import java.util.Set;

/** Prepares one text replacement that pastes a cell grid over the records starting at a given cell. */
final class CsvTablePaste {
    private final String source;
    private final int columnIndex;
    private final CsvValueSeparator separator;
    private final CsvEscapeCharacter escape;
    private final String comment;
    private final boolean quoting;
    private final StringBuilder result = new StringBuilder();

    private CsvTablePaste(PsiFile file, int columnIndex) {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        this.source = file.getText();
        this.columnIndex = columnIndex;
        this.separator = CsvHelper.getValueSeparator(file);
        this.escape = CsvHelper.getEscapeCharacter(file);
        this.comment = settings.getCommentIndicator();
        this.quoting = settings.isQuotingEnforced();
    }

    static Pair<TextRange, String> prepare(PsiFile file, List<List<String>> values, Set<Integer> commentRows,
                                           int rowIndex, int columnIndex) {
        List<CsvRecord> records = PsiTreeUtil.getChildrenOfTypeAsList(file, CsvRecord.class);
        if (rowIndex < 0 || rowIndex >= records.size() || columnIndex < 0 || values.isEmpty()) {
            throw new IllegalArgumentException("Invalid paste position or empty grid");
        }
        CsvTablePaste paste = new CsvTablePaste(file, columnIndex);
        int start = records.get(rowIndex).getTextOffset();
        int end = start;
        for (int index = 0; index < values.size(); index++) {
            List<String> row = values.get(index);
            boolean pastedComment = commentRows.contains(index);
            paste.validate(row, pastedComment);
            int targetRow = rowIndex + index;
            if (targetRow < records.size()) {
                CsvRecord record = records.get(targetRow);
                paste.result.append(paste.source, end, record.getTextOffset());
                end = record.getTextRange().getEndOffset();
                paste.overwrite(record, row, pastedComment);
            } else {
                paste.append(row, pastedComment);
            }
        }
        return Pair.create(new TextRange(start, end), paste.result.toString());
    }

    private void validate(List<String> row, boolean pastedComment) {
        if (row.isEmpty()) throw new IllegalArgumentException("Empty clipboard row");
        if (pastedComment && (comment.isEmpty() || !isSingleLineFirstCell(row))) {
            throw new IllegalArgumentException("A clipboard comment requires a single-line cell in the first column");
        }
    }

    private boolean isSingleLineFirstCell(List<String> row) {
        return columnIndex == 0 && row.size() == 1 && !row.getFirst().contains("\n");
    }

    private void overwrite(CsvRecord record, List<String> row, boolean pastedComment) {
        if (pastedComment) {
            appendComment(row);
        } else if (CsvHelper.isCommentElement(record.getFirstChild())) {
            if (!isSingleLineFirstCell(row)) {
                throw new IllegalArgumentException("A comment row has only one single-line cell");
            }
            appendComment(row);
        } else {
            overwriteFields(record, row);
        }
    }

    private void overwriteFields(CsvRecord record, List<String> row) {
        List<CsvField> fields = PsiTreeUtil.getChildrenOfTypeAsList(record, CsvField.class);
        if (columnIndex < fields.size()) {
            result.append(source, record.getTextOffset(), fields.get(columnIndex).getTextOffset());
        } else {
            result.append(record.getText());
            result.repeat(separator.getCharacter(), columnIndex - Math.max(0, fields.size() - 1));
        }
        appendValues(row);
        int lastColumn = columnIndex + row.size() - 1;
        if (lastColumn < fields.size()) {
            result.append(source, fields.get(lastColumn).getTextRange().getEndOffset(), record.getTextRange().getEndOffset());
        }
    }

    private void append(List<String> row, boolean pastedComment) {
        result.append('\n');
        if (pastedComment) {
            appendComment(row);
        } else {
            result.repeat(separator.getCharacter(), columnIndex);
            appendValues(row);
        }
    }

    private void appendComment(List<String> row) {
        result.append(comment).append(row.getFirst());
    }

    private void appendValues(List<String> values) {
        for (int column = 0; column < values.size(); column++) {
            if (column > 0) result.append(separator.getCharacter());
            String value = values.get(column);
            result.append(CsvHelper.quoteCsvField(value, escape, separator, isQuotingRequired(value, columnIndex + column)));
        }
    }

    private boolean isQuotingRequired(String value, int column) {
        return quoting || column == 0 && !comment.isEmpty() && value.trim().startsWith(comment);
    }
}
