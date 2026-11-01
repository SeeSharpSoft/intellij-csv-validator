package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;

import java.util.ArrayList;
import java.util.List;

record CsvDiffChange(TextRange range, String text) {
    static CsvDiffChange prepare(CsvDiffComparison comparison, CsvDiffComparison.Change change, int targetSide) {
        CsvDiffData target = comparison.sides().get(targetSide);
        CsvDiffData source = comparison.sides().get(1 - targetSide);
        if (change.finalLineBreak()) {
            int end = target.text().length();
            return source.text().endsWith("\n")
                    ? new CsvDiffChange(new TextRange(end, end), "\n")
                    : new CsvDiffChange(new TextRange(end - 1, end), "");
        }
        var records = change.records();
        int targetStart = targetSide == 0 ? records.start1 : records.start2;
        int targetEnd = targetSide == 0 ? records.end1 : records.end2;
        int sourceStart = targetSide == 0 ? records.start2 : records.start1;
        int sourceEnd = targetSide == 0 ? records.end2 : records.end1;
        List<String> rows = new ArrayList<>();
        for (int row = sourceStart; row < sourceEnd; row++) {
            ProgressManager.checkCanceled();
            rows.add(serialize(source, source.rows().get(row), target.format()));
        }
        int start = offset(target, targetStart);
        int end = offset(target, targetEnd);
        boolean finalLineBreak = target.text().endsWith("\n");
        String replacement = String.join("\n", rows);
        if (!rows.isEmpty()) {
            if (targetEnd < target.rows().size() || finalLineBreak) replacement += "\n";
            if (targetStart == target.rows().size() && !target.text().isEmpty() && !finalLineBreak)
                replacement = "\n" + replacement;
        } else if (targetEnd == target.rows().size() && targetStart > 0 && !finalLineBreak) {
            start--;
        }
        return new CsvDiffChange(new TextRange(start, end), replacement);
    }

    private static int offset(CsvDiffData data, int row) {
        return row < data.rows().size() ? data.rows().get(row).range().getStartOffset() : data.text().length();
    }

    private static String serialize(CsvDiffData source, CsvDiffData.Row row, CsvDiffData.Format target) {
        if (source.format().separator().equals(target.separator()) && source.format().escape().equals(target.escape())
                && source.format().comment().equals(target.comment())) return row.range().substring(source.text());
        if (row.comment()) {
            if (target.comment().isEmpty()) throw new IllegalArgumentException("Target does not support CSV comments");
            return target.comment() + row.cells().getFirst().value();
        }
        StringBuilder result = new StringBuilder();
        for (int column = 0; column < row.cells().size(); column++) {
            if (column > 0) result.append(target.separator().getCharacter());
            CsvDiffData.Cell cell = row.cells().get(column);
            boolean quoting = target.quoting() || cell.range().substring(source.text()).trim().startsWith("\"")
                    || column == 0 && !target.comment().isEmpty() && cell.value().trim().startsWith(target.comment());
            result.append(CsvHelper.quoteCsvField(cell.value(), target.escape(), target.separator(), quoting));
        }
        return result.toString();
    }
}
