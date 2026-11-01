package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.diff.util.Range;
import com.intellij.diff.util.TextDiffType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.diff.Diff;
import com.intellij.util.diff.FilesTooBigForDiffException;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;

import java.util.ArrayList;
import java.util.List;

record CsvDiffComparison(List<CsvDiffData> sides, List<AlignedRow> rows, List<Change> differences,
                         int columns, List<Integer> rowLines, String error) {
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

    static CsvDiffComparison compare(List<CsvDiffData> sides) {
        if (sides.stream().anyMatch(side -> !side.valid())) {
            return new CsvDiffComparison(sides, List.of(), List.of(), 0, List.of(), "table.diff.invalid");
        }
        try {
            return align(sides);
        } catch (FilesTooBigForDiffException exception) {
            return new CsvDiffComparison(sides, List.of(), List.of(), 0, List.of(), "table.diff.too.big");
        }
    }

    private static CsvDiffComparison align(List<CsvDiffData> sides) throws FilesTooBigForDiffException {
        CsvDiffData left = sides.getFirst();
        CsvDiffData right = sides.getLast();
        ProgressManager.checkCanceled();
        Diff.Change diff = Diff.buildChanges(keys(left).toArray(String[]::new), keys(right).toArray(String[]::new));
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
        return new CsvDiffComparison(sides, List.copyOf(rows), List.copyOf(differences), columns, rowLines, null);
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

    private static List<String> keys(CsvDiffData data) {
        return data.rows().stream().map(row -> row.range().substring(data.text())).toList();
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
        return !first.cells().get(column).range().substring(left.text())
                .equals(second.cells().get(column).range().substring(right.text()));
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
