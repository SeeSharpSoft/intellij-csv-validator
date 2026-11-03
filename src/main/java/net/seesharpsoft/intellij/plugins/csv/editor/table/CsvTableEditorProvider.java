package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvFileEditorProvider;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvFileEditorProviderBase;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableEditorSwing;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class CsvTableEditorProvider extends CsvFileEditorProviderBase {

    public static final String EDITOR_TYPE_ID = "csv-table-editor";

    @Override
    public @NotNull String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    @Override
    protected boolean isPrimaryEditor(@NotNull CsvEditorSettings.EditorPrio editorPrio) {
        return switch (editorPrio) {
            case TEXT_FIRST, TEXT_ONLY -> false;
            case TABLE_FIRST -> true;
            default -> throw new IllegalArgumentException("unhandled EditorPrio: " + editorPrio);
        };
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return CsvEditorSettings.getInstance().getEditorPrio() != CsvEditorSettings.EditorPrio.TEXT_ONLY &&
                CsvFileEditorProvider.acceptCsvFile(project, file);
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return createEditorAsync(project, virtualFile).build();
    }

    @Override
    public @NotNull FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile file) {
        return CsvTableEditorState.create(sourceElement, project, file);
    }

    @Override
    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        if (!(state instanceof CsvTableEditorState csvTableEditorState)) {
            return;
        }
        csvTableEditorState.write(project, targetElement);
    }

    @NotNull
    @Override
    public Builder createEditorAsync(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new Builder() {
            @Override
            public @NotNull FileEditor build() {
                return new CsvTableEditorSwing(project, virtualFile);
            }
        };
    }
}
