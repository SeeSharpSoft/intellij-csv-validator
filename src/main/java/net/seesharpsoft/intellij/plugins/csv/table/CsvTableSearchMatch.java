package net.seesharpsoft.intellij.plugins.csv.table;

import org.jetbrains.annotations.NotNull;

public record CsvTableSearchMatch(int row, int column, int start, int end) implements Comparable<CsvTableSearchMatch> {
    @Override
    public int compareTo(@NotNull CsvTableSearchMatch other) {
        int comparison = Integer.compare(row, other.row);
        if (comparison == 0) comparison = Integer.compare(column, other.column);
        return comparison == 0 ? Integer.compare(start, other.start) : comparison;
    }
}
