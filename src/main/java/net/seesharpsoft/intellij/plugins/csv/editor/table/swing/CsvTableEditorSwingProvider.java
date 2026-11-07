package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorProvider;
import org.jetbrains.annotations.NotNull;

public class CsvTableEditorSwingProvider extends CsvTableEditorProvider {
    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new CsvTableEditorSwing(project, virtualFile);
    }

    @Override
    public @NotNull Builder createEditorAsync(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new Builder() {
            @Override
            public @NotNull FileEditor build() {
                return createEditor(project, virtualFile);
            }
        };
    }
}
