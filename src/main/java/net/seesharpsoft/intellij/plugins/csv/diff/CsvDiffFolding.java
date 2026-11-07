package net.seesharpsoft.intellij.plugins.csv.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Maps the aligned rows of a comparison to the rows displayed by the diff tables. Like the text diff's
 * "Collapse Unchanged Fragments", every unchanged range farther than the context range from a change is displayed
 * as a single fold row, unless the user revealed it.
 */
final class CsvDiffFolding {
    record Fold(int startRow, int endRow) {
        int size() {
            return endRow - startRow;
        }
    }

    // Like the text diff, a single row is never replaced by a placeholder row of the same size.
    private static final int MIN_FOLD_SIZE = 2;

    private final int rowCount;
    private final List<Fold> folds;
    private final int[] foldStarts;
    private final int[] foldDisplayRows;
    private final int[] hiddenBefore;

    private CsvDiffFolding(int rowCount, List<Fold> folds) {
        this.rowCount = rowCount;
        this.folds = List.copyOf(folds);
        foldStarts = new int[folds.size()];
        foldDisplayRows = new int[folds.size()];
        hiddenBefore = new int[folds.size() + 1];
        for (int index = 0; index < folds.size(); index++) {
            Fold fold = folds.get(index);
            foldStarts[index] = fold.startRow();
            foldDisplayRows[index] = fold.startRow() - hiddenBefore[index];
            hiddenBefore[index + 1] = hiddenBefore[index] + fold.size() - 1;
        }
    }

    static CsvDiffFolding expanded(int rowCount) {
        return new CsvDiffFolding(rowCount, List.of());
    }

    /**
     * Folds the unchanged ranges of the comparison except their context rows next to changes and the revealed rows.
     * As in the text diff, unchanged ranges at the start and end of the documents have no outer context.
     */
    static CsvDiffFolding collapse(CsvDiffComparison comparison, int contextRange, List<Fold> revealed) {
        int rowCount = comparison.rows().size();
        List<Fold> folds = new ArrayList<>();
        int start = 0;
        for (CsvDiffComparison.Change change : comparison.differences()) {
            addFolds(folds, start, change.startRow() - contextRange, revealed);
            start = change.endRow() + contextRange;
        }
        addFolds(folds, start, rowCount, revealed);
        return new CsvDiffFolding(rowCount, folds);
    }

    /**
     * Returns the sorted revealed ranges including the given fold.
     */
    static List<Fold> reveal(List<Fold> revealed, Fold fold) {
        List<Fold> result = new ArrayList<>(revealed);
        result.add(fold);
        result.sort(Comparator.comparingInt(Fold::startRow));
        return List.copyOf(result);
    }

    private static void addFolds(List<Fold> folds, int start, int end, List<Fold> revealed) {
        int cursor = start;
        for (Fold shown : revealed) {
            if (shown.startRow() >= end) break;
            if (shown.endRow() <= cursor) continue;
            addFold(folds, cursor, shown.startRow());
            cursor = Math.max(cursor, shown.endRow());
        }
        addFold(folds, cursor, end);
    }

    private static void addFold(List<Fold> folds, int start, int end) {
        if (end - start >= MIN_FOLD_SIZE) folds.add(new Fold(start, end));
    }

    int displayRowCount() {
        return rowCount - hiddenBefore[folds.size()];
    }

    /**
     * Returns the fold displayed in the given row, or {@code null} if the row displays a single aligned row.
     */
    Fold foldAt(int displayRow) {
        int index = Arrays.binarySearch(foldDisplayRows, displayRow);
        return index < 0 ? null : folds.get(index);
    }

    boolean isFold(int displayRow) {
        return foldAt(displayRow) != null;
    }

    /**
     * Returns the first aligned row shown by the given display row.
     */
    int firstRow(int displayRow) {
        int index = indexAtOrBefore(foldDisplayRows, displayRow);
        if (index < 0) return displayRow;
        if (foldDisplayRows[index] == displayRow) return folds.get(index).startRow();
        return displayRow + hiddenBefore[index + 1];
    }

    /**
     * Returns the last aligned row shown by the given display row.
     */
    int lastRow(int displayRow) {
        Fold fold = foldAt(displayRow);
        return fold == null ? firstRow(displayRow) : fold.endRow() - 1;
    }

    /**
     * Returns the display row showing the given aligned row, which is the fold row for hidden rows.
     */
    int displayRow(int row) {
        int index = indexAtOrBefore(foldStarts, row);
        if (index < 0) return row;
        if (row < folds.get(index).endRow()) return foldDisplayRows[index];
        return row - hiddenBefore[index + 1];
    }

    private static int indexAtOrBefore(int[] sorted, int value) {
        int index = Arrays.binarySearch(sorted, value);
        return index >= 0 ? index : -index - 2;
    }
}
