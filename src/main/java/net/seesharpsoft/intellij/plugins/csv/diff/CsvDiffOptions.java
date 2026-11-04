package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.DiffTooBigException;
import com.intellij.diff.fragments.DiffFragment;
import com.intellij.diff.tools.util.base.HighlightPolicy;
import com.intellij.diff.tools.util.base.IgnorePolicy;
import com.intellij.diff.tools.util.base.TextDiffSettingsHolder.TextDiffSettings;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;

import java.util.List;
import java.util.stream.Collectors;

record CsvDiffOptions(IgnorePolicy ignore, HighlightPolicy highlight) {
    static CsvDiffOptions capture(TextDiffSettings settings) {
        IgnorePolicy ignore = switch (settings.getIgnorePolicy()) {
            case DEFAULT, TRIM_WHITESPACES, IGNORE_WHITESPACES, FORMATTING -> settings.getIgnorePolicy();
            case IGNORE_WHITESPACES_CHUNKS -> IgnorePolicy.IGNORE_WHITESPACES;
            case IGNORE_LANGUAGE_SPECIFIC_CHANGES -> IgnorePolicy.DEFAULT;
            default -> throw new IllegalArgumentException("Unsupported ignore policy: " + settings.getIgnorePolicy());
        };
        HighlightPolicy highlight = settings.getHighlightPolicy() == HighlightPolicy.BY_WORD_SPLIT
                ? HighlightPolicy.BY_WORD : settings.getHighlightPolicy();
        return new CsvDiffOptions(ignore, highlight);
    }

    private record RowKey(boolean comment, String separator, List<String> cells) {
    }

    Object rowKey(CsvDiffData data, CsvDiffData.Row row) {
        ProgressManager.checkCanceled();
        if (ignore == IgnorePolicy.DEFAULT) return row.range().substring(data.text());
        return new RowKey(row.comment(), ignore == IgnorePolicy.FORMATTING ? "" : data.format().separator().getCharacter(),
                row.cells().stream().map(cell -> cellKey(data, cell)).toList());
    }

    String cellKey(CsvDiffData data, CsvDiffData.Cell cell) {
        return switch (ignore) {
            case DEFAULT -> cell.range().substring(data.text());
            case TRIM_WHITESPACES -> cell.range().substring(data.text()).lines()
                    .map(String::strip).collect(Collectors.joining("\n"));
            case IGNORE_WHITESPACES -> withoutWhitespace(cell.range().substring(data.text()));
            case FORMATTING -> cell.value();
            default -> throw new IllegalArgumentException("Unsupported table ignore policy: " + ignore);
        };
    }

    private static String withoutWhitespace(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            if ((index & 1023) == 0) ProgressManager.checkCanceled();
            char character = text.charAt(index);
            if (!Character.isWhitespace(character)) result.append(character);
        }
        return result.toString();
    }

    List<DiffFragment> fragments(String left, String right) throws DiffTooBigException {
        ProgressManager.checkCanceled();
        var indicator = ProgressManager.getGlobalProgressIndicator();
        if (indicator == null) indicator = new EmptyProgressIndicator();
        return switch (highlight) {
            case BY_LINE, DO_NOT_HIGHLIGHT -> List.of();
            case BY_WORD ->
                    ComparisonManager.getInstance().compareWords(left, right, ignore.getComparisonPolicy(), indicator);
            case BY_CHAR ->
                    ComparisonManager.getInstance().compareChars(left, right, ignore.getComparisonPolicy(), indicator);
            default -> throw new IllegalArgumentException("Unsupported table highlight policy: " + highlight);
        };
    }
}
