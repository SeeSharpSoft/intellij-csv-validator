package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.diff.DiffContext;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.contents.EmptyContent;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.DiffRequest;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;
import org.jetbrains.annotations.NotNull;

public final class CsvTableDiffTool implements FrameDiffTool {
    @Override
    public @NotNull String getName() {
        return CsvPluginManager.getLocalizedText("table.diff");
    }

    @Override
    public boolean canShow(@NotNull DiffContext context, @NotNull DiffRequest request) {
        if (!(request instanceof ContentDiffRequest contents) || contents.getContents().size() != 2) return false;
        boolean csv = false;
        for (DiffContent content : contents.getContents()) {
            if (content instanceof EmptyContent) continue;
            if (!(content instanceof DocumentContent document) || !isCsv(document)) return false;
            csv = true;
        }
        return csv;
    }

    private static boolean isCsv(DocumentContent content) {
        return CsvHelper.isCsvFile(content.getContentType())
                || content.getContentType() == null && CsvHelper.isCsvFile(content.getHighlightFile());
    }

    @Override
    public @NotNull DiffViewer createComponent(@NotNull DiffContext context, @NotNull DiffRequest request) {
        if (!canShow(context, request)) throw new IllegalArgumentException("Not a two-way CSV diff request");
        return new CsvTableDiffViewer(context, (ContentDiffRequest) request);
    }
}
