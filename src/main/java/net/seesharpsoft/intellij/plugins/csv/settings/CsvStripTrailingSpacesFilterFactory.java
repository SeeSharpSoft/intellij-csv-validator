package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.StripTrailingSpacesFilter;
import com.intellij.openapi.editor.StripTrailingSpacesFilterFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvStripTrailingSpacesFilterFactory extends StripTrailingSpacesFilterFactory {
    @NotNull
    @Override
    public StripTrailingSpacesFilter createFilter(@Nullable Project project, @NotNull Document document) {
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (project != null &&
                virtualFile != null &&
                virtualFile.getFileType() instanceof LanguageFileType &&
                ((LanguageFileType) virtualFile.getFileType()).getLanguage().isKindOf(CsvLanguage.INSTANCE) &&
                CsvEditorSettings.getInstance().getKeepTrailingSpaces()) {
            return StripTrailingSpacesFilter.NOT_ALLOWED;
        }
        return StripTrailingSpacesFilter.ALL_LINES;
    }
}
