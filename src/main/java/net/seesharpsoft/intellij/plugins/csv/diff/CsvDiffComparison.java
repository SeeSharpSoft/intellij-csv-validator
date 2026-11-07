package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.diff.util.Range;
import com.intellij.diff.util.TextDiffType;
import com.intellij.diff.comparison.DiffTooBigException;
import com.intellij.diff.fragments.DiffFragment;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.diff.Diff;
import com.intellij.util.diff.FilesTooBigForDiffException;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

record CsvDiffComparison(List<CsvDiffData> sides, List<AlignedRow> rows, List<Change> differences,
                         int columns, List<Integer> rowLines, String error, CsvDiffOptions options,
                         Map<Long, List<DiffFragment>> cellDifferences) {
    record AlignedRow(int left, int right) {
        int index(int side) {
            return side == 0 ? left : right;
        }
    }

    record Change(int startRow, int endRow, Range records, boolean finalLineBreak) {
        TextDiffType type() {
            if (finalLineBreak) return TextDiffType.MODIFIED;
            if (records.start1 == records.end1) return TextDiffType.INSERTED;
            if (records.start2 == records.end2) return TextDiffType.DELETED;
            return TextDiffType.MODIFIED;
        }
    }

    @SuppressWarnings("IncorrectCancellationExceptionHandling")
    // DiffTooBigException reports a size limit, not cancellation.
    static CsvDiffComparison compare(List<CsvDiffData> sides, CsvDiffOptions options) {
        if (sides.stream().anyMatch(side -> !side.valid())) {
            return error(sides, options, "table.diff.invalid");
        }
        try {
            return align(sides, options);
        } catch (FilesTooBigForDiffException | DiffTooBigException exception) {
            return error(sides, options, "table.diff.too.big");
        }
    }

    private static CsvDiffComparison error(List<CsvDiffData> sides, CsvDiffOptions options, String error) {
        return new CsvDiffComparison(sides, List.of(), List.of(), 0, List.of(), error, options, Map.of());
    }

    private static CsvDiffComparison align(List<CsvDiffData> sides, CsvDiffOptions options)
            throws FilesTooBigForDiffException, DiffTooBigException {
        CsvDiffData left = sides.getFirst();
        CsvDiffData right = sides.getLast();
        ProgressManager.checkCanceled();
        Diff.Change diff = Diff.buildChanges(keys(left, options), keys(right, options));
        List<AlignedRow> rows = new ArrayList<>();
        List<Change> differences = new ArrayList<>();
        int first = 0;
        int second = 0;
        for (Diff.Change current = diff; current != null; current = current.link) {
            ProgressManager.checkCanceled();
            Range change = new Range(current.line0, current.line0 + current.deleted,
                    current.line1, current.line1 + current.inserted);
            while (first < change.start1) rows.add(new AlignedRow(first++, second++));
            int startRow = rows.size();
            while (first < change.end1 || second < change.end2) {
                rows.add(new AlignedRow(first < change.end1 ? first++ : -1, second < change.end2 ? second++ : -1));
            }
            differences.add(new Change(startRow, rows.size(), change, false));
        }

        while (first < left.rows().size()) rows.add(new AlignedRow(first++, second++));
        if (left.text().endsWith("\n") != right.text().endsWith("\n")) {
            differences.add(new Change(rows.size(), rows.size() + 1,
                    new Range(first, first, second, second), true));
            rows.add(new AlignedRow(first, second));
        }
        int columns = sides.stream().flatMap(data -> data.rows().stream())
                .mapToInt(row -> row.cells().size()).max().orElse(0);
        List<Integer> rowLines = rows.stream()
                .map(row -> Math.max(lines(left, row.left()), lines(right, row.right()))).toList();
        Map<Long, List<DiffFragment>> cells = compareCells(sides, rows, differences, options);
        return new CsvDiffComparison(sides, List.copyOf(rows), List.copyOf(differences), columns, rowLines, null,
                options, Map.copyOf(cells));
    }

    private static Map<Long, List<DiffFragment>> compareCells(List<CsvDiffData> sides, List<AlignedRow> rows,
                                                              List<Change> differences, CsvDiffOptions options)
            throws DiffTooBigException {
        Map<Long, List<DiffFragment>> result = new HashMap<>();
        CsvDiffData left = sides.getFirst();
        CsvDiffData right = sides.getLast();
        for (Change change : differences) {
            if (change.finalLineBreak()) continue;
            for (int row = change.startRow(); row < change.endRow(); row++) {
                ProgressManager.checkCanceled();
                AlignedRow aligned = rows.get(row);
                if (aligned.left() < 0 || aligned.right() < 0) continue;
                CsvDiffData.Row first = left.rows().get(aligned.left());
                CsvDiffData.Row second = right.rows().get(aligned.right());
                int columns = Math.min(first.cells().size(), second.cells().size());
                for (int column = 0; column < columns; column++) {
                    ProgressManager.checkCanceled();
                    CsvDiffData.Cell a = first.cells().get(column);
                    CsvDiffData.Cell b = second.cells().get(column);
                    if (first.comment() != second.comment() || !options.cellKey(left, a).equals(options.cellKey(right, b))) {
                        result.put(cellKey(row, column), List.copyOf(options.fragments(a.value(), b.value())));
                    }
                }
            }
        }
        return result;
    }

    private static long cellKey(int row, int column) {
        return ((long) row << 32) | (column & 0xffffffffL);
    }

    List<DiffFragment> fragments(int row, int column) {
        return cellDifferences.getOrDefault(cellKey(row, column), List.of());
    }

    Change changeAt(int row) {
        int index = firstChangeEndingAfter(row);
        return index < differences.size() && differences.get(index).startRow() <= row ? differences.get(index) : null;
    }

    /**
     * Returns the index of the first change ending after the given row, or the number of changes if there is none.
     */
    int firstChangeEndingAfter(int row) {
        int low = 0;
        int high = differences.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (differences.get(middle).endRow() <= row) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private static Object[] keys(CsvDiffData data, CsvDiffOptions options) {
        return data.rows().stream().map(row -> options.rowKey(data, row)).toArray();
    }

    private static int lines(CsvDiffData data, int row) {
        ProgressManager.checkCanceled();
        if (row < 0 || row >= data.rows().size()) return 1;
        return data.rows().get(row).cells().stream().mapToInt(CsvDiffData.Cell::lines).max().orElse(1);
    }

    String value(int side, int row, int column) {
        if (isLineBreakRow(row)) {
            if (column != 0) return "";
            return CsvPluginManager.getLocalizedText(sides.get(side).text().endsWith("\n")
                    ? "table.diff.final.line.break" : "table.diff.no.final.line.break");
        }
        int index = rows.get(row).index(side);
        if (index < 0) return "";
        List<CsvDiffData.Cell> cells = sides.get(side).rows().get(index).cells();
        return column < cells.size() ? cells.get(column).value() : "";
    }

    /**
     * Returns the 1-based record number of the row on the given side, or an empty label if the side lacks the row.
     */
    String rowNumber(int side, int row) {
        int index = rows.get(row).index(side);
        return index < 0 || isLineBreakRow(row) ? "" : Integer.toString(index + 1);
    }

    boolean changed(int row, int column) {
        if (isLineBreakRow(row)) return column == 0;
        AlignedRow aligned = rows.get(row);
        if (aligned.left() < 0 || aligned.right() < 0) return true;
        CsvDiffData left = sides.getFirst();
        CsvDiffData right = sides.getLast();
        CsvDiffData.Row first = left.rows().get(aligned.left());
        CsvDiffData.Row second = right.rows().get(aligned.right());
        if (column >= first.cells().size() || column >= second.cells().size()) {
            return (column < first.cells().size()) != (column < second.cells().size());
        }
        return cellDifferences.containsKey(cellKey(row, column));
    }

    int firstChangedColumn(int row) {
        for (int column = 0; column < columns; column++) {
            if (changed(row, column)) return column;
        }
        return 0;
    }

    boolean isLineBreakRow(int row) {
        return rows.get(row).left() == sides.getFirst().rows().size()
                && rows.get(row).right() == sides.getLast().rows().size();
    }
}
