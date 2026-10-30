package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Prepares a single document edit from an immutable snapshot, without building or traversing PSI.
 */
public final class CsvTableReorder {
    public record Change(TextRange range, String text) {
    }

    private abstract static class Reordering {
        protected final CharSequence text;
        protected final int[] order;
        protected final StringBuilder result;

        Reordering(CharSequence text, int[] order) {
            this.text = text;
            this.order = order;
            this.result = new StringBuilder(text.length());
        }

        final StringBuilder reorder(Lexer lexer) {
            lexer.start(text);
            boolean quoted = false;
            int tokens = 0;
            for (IElementType type = lexer.getTokenType(); type != null; type = lexer.getTokenType()) {
                if ((tokens++ & 1023) == 0) ProgressManager.checkCanceled();
                if (type == TokenType.BAD_CHARACTER) throw new IllegalArgumentException("Invalid CSV character");
                if (type == CsvTypes.QUOTE) quoted = !quoted;
                onToken(type, lexer.getTokenStart(), lexer.getTokenEnd());
                lexer.advance();
            }
            ProgressManager.checkCanceled();
            if (quoted) throw new IllegalArgumentException("Unclosed CSV quote");
            finish();
            return result;
        }

        protected abstract void onToken(IElementType type, int start, int end);

        protected abstract void finish();
    }

    private static final class RowReordering extends Reordering {
        private final List<TextRange> records = new ArrayList<>();
        private int rowStart;

        RowReordering(CharSequence text, int[] order) {
            super(text, order);
        }

        @Override
        protected void onToken(IElementType type, int start, int end) {
            if (type != CsvTypes.CRLF) return;
            records.add(new TextRange(rowStart, start));
            rowStart = end;
        }

        @Override
        protected void finish() {
            records.add(new TextRange(rowStart, text.length()));
            validateOrder(order, records.size());
            if (records.size() > 1 && records.getLast().isEmpty() && order[order.length - 1] != order.length - 1) {
                throw new IllegalArgumentException("The terminal empty record must remain last");
            }
            for (int row = 0; row < order.length; row++) {
                if ((row & 1023) == 0) ProgressManager.checkCanceled();
                TextRange source = records.get(order[row]);
                result.append(text, source.getStartOffset(), source.getEndOffset());
                if (row + 1 < records.size()) {
                    result.append(text, records.get(row).getEndOffset(), records.get(row + 1).getStartOffset());
                }
            }
        }
    }

    private static final class ColumnReordering extends Reordering {
        private final CsvValueSeparator separator;
        private final CsvEscapeCharacter escape;
        private final String commentIndicator;
        private final int[] starts;
        private final int[] ends;
        private int fieldCount;
        private int maxColumns;
        private boolean comment;

        ColumnReordering(CharSequence text, int[] order, CsvValueSeparator separator, CsvEscapeCharacter escape,
                         String commentIndicator) {
            super(text, order);
            this.separator = separator;
            this.escape = escape;
            this.commentIndicator = commentIndicator;
            this.starts = new int[Math.max(1, order.length)];
            this.ends = new int[starts.length];
        }

        @Override
        protected void onToken(IElementType type, int start, int end) {
            if (type == CsvTypes.COMMENT) {
                comment = true;
            } else if (type == CsvTypes.COMMA) {
                if (fieldCount + 1 >= starts.length) throw new IllegalArgumentException("Column count changed");
                ends[fieldCount++] = start;
                starts[fieldCount] = end;
            } else if (type == CsvTypes.CRLF) {
                endRow(start);
                appendRow(start);
                result.append(text, start, end);
                starts[0] = end;
                fieldCount = 0;
                comment = false;
            }
        }

        @Override
        protected void finish() {
            endRow(text.length());
            if (starts[0] < text.length() || text.isEmpty()) appendRow(text.length());
            validateOrder(order, maxColumns);
        }

        private void endRow(int rowEnd) {
            ends[fieldCount] = rowEnd;
            maxColumns = Math.max(maxColumns, fieldCount + 1);
        }

        private void appendRow(int rowEnd) {
            if (comment) {
                result.append(text, starts[0], rowEnd);
                return;
            }
            int fields = fieldCount + 1;
            int last = order.length - 1;
            while (last > 0 && order[last] >= fields) last--;
            for (int column = 0; column <= last; column++) {
                if (column > 0) result.append(separator.getCharacter());
                int source = order[column];
                if (source < fields) appendField(column, starts[source], ends[source]);
            }
        }

        private void appendField(int column, int start, int end) {
            if (column == 0 && startsComment(start, end)) {
                String field = text.subSequence(start, end).toString();
                result.append(CsvHelper.quoteCsvField(CsvHelper.unquoteCsvValue(field, escape), escape, separator, true));
            } else {
                result.append(text, start, end);
            }
        }

        private boolean startsComment(int start, int end) {
            if (commentIndicator.isEmpty()) return false;
            int offset = start;
            while (offset < end && text.charAt(offset) <= ' ') offset++;
            if (end - offset < commentIndicator.length()) return false;
            for (int index = 0; index < commentIndicator.length(); index++) {
                if (text.charAt(offset + index) != commentIndicator.charAt(index)) return false;
            }
            return true;
        }
    }

    private CsvTableReorder() {
    }

    public static Change prepare(CharSequence text, Lexer lexer, int[] order, boolean columns,
                                 CsvValueSeparator separator, CsvEscapeCharacter escape, String commentIndicator) {
        validateOrder(order, order.length);
        Reordering reordering = columns
                ? new ColumnReordering(text, order, separator, escape, commentIndicator)
                : new RowReordering(text, order);
        return changedSpan(text, reordering.reorder(lexer));
    }

    private static Change changedSpan(CharSequence original, StringBuilder reordered) {
        int start = 0;
        int end = original.length();
        int newEnd = reordered.length();
        while (start < end && start < newEnd && original.charAt(start) == reordered.charAt(start)) {
            if ((start & 16383) == 0) ProgressManager.checkCanceled();
            start++;
        }
        while (end > start && newEnd > start && original.charAt(end - 1) == reordered.charAt(newEnd - 1)) {
            if ((end & 16383) == 0) ProgressManager.checkCanceled();
            end--;
            newEnd--;
        }
        return start == end && start == newEnd ? null : new Change(new TextRange(start, end), reordered.substring(start, newEnd));
    }

    private static void validateOrder(int[] order, int count) {
        if (order.length != count) throw new IllegalArgumentException("Reordering must include every index");
        boolean[] seen = new boolean[count];
        for (int index : order) {
            if (index < 0 || index >= count || seen[index]) {
                throw new IllegalArgumentException("Reordering must be a permutation");
            }
            seen[index] = true;
        }
    }
}
