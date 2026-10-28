package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
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

    private CsvTableReorder() {
    }

    public static Change prepare(CharSequence text, Lexer lexer, int[] order, boolean columns,
                                 CsvValueSeparator separator, CsvEscapeCharacter escape, String commentIndicator) {
        validateOrder(order, order.length);
        lexer.start(text);
        StringBuilder result = new StringBuilder(text.length());
        List<TextRange> records = columns ? null : new ArrayList<>();
        int[] starts = new int[columns ? Math.max(1, order.length) : 1];
        int[] ends = new int[starts.length];
        int rowStart = 0;
        int fieldCount = 0;
        int maxColumns = 0;
        boolean comment = false;
        boolean quoted = false;
        int tokens = 0;
        while (lexer.getTokenType() != null) {
            if ((tokens++ & 1023) == 0) ProgressManager.checkCanceled();
            if (lexer.getTokenType() == TokenType.BAD_CHARACTER)
                throw new IllegalArgumentException("Invalid CSV character");
            if (lexer.getTokenType() == CsvTypes.QUOTE) quoted = !quoted;
            if (lexer.getTokenType() == CsvTypes.COMMENT) comment = true;
            if (columns && lexer.getTokenType() == CsvTypes.COMMA) {
                if (fieldCount + 1 >= starts.length) throw new IllegalArgumentException("Column count changed");
                ends[fieldCount++] = lexer.getTokenStart();
                starts[fieldCount] = lexer.getTokenEnd();
            } else if (lexer.getTokenType() == CsvTypes.CRLF) {
                if (columns) {
                    ends[fieldCount] = lexer.getTokenStart();
                    maxColumns = Math.max(maxColumns, fieldCount + 1);
                    appendColumns(result, text, rowStart, lexer.getTokenStart(), starts, ends, fieldCount + 1,
                            comment, order, separator, escape, commentIndicator);
                    result.append(text, lexer.getTokenStart(), lexer.getTokenEnd());
                } else {
                    records.add(new TextRange(rowStart, lexer.getTokenStart()));
                }
                rowStart = lexer.getTokenEnd();
                starts[0] = rowStart;
                fieldCount = 0;
                comment = false;
            }
            lexer.advance();
        }
        ProgressManager.checkCanceled();
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quote");
        if (columns) {
            ends[fieldCount] = text.length();
            maxColumns = Math.max(maxColumns, fieldCount + 1);
            if (rowStart < text.length() || text.isEmpty()) {
                appendColumns(result, text, rowStart, text.length(), starts, ends, fieldCount + 1,
                        comment, order, separator, escape, commentIndicator);
            }
            validateOrder(order, maxColumns);
        } else {
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
        return changedSpan(text, result);
    }

    private static void appendColumns(StringBuilder result, CharSequence text, int rowStart, int rowEnd,
                                      int[] starts, int[] ends, int fieldCount, boolean comment, int[] order,
                                      CsvValueSeparator separator, CsvEscapeCharacter escape, String commentIndicator) {
        if (comment) {
            result.append(text, rowStart, rowEnd);
            return;
        }
        int last = order.length - 1;
        while (last > 0 && order[last] >= fieldCount) last--;
        for (int column = 0; column <= last; column++) {
            if (column > 0) result.append(separator.getCharacter());
            int source = order[column];
            if (source >= fieldCount) continue;
            int start = starts[source];
            int end = ends[source];
            if (column == 0 && startsComment(text, start, end, commentIndicator)) {
                String field = text.subSequence(start, end).toString();
                result.append(CsvHelper.quoteCsvField(CsvHelper.unquoteCsvValue(field, escape), escape, separator, true));
            } else {
                result.append(text, start, end);
            }
        }
    }

    private static boolean startsComment(CharSequence text, int start, int end, String indicator) {
        if (indicator.isEmpty()) return false;
        while (start < end && text.charAt(start) <= ' ') start++;
        if (end - start < indicator.length()) return false;
        for (int index = 0; index < indicator.length(); index++) {
            if (text.charAt(start + index) != indicator.charAt(index)) return false;
        }
        return true;
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
