package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvFileEditorProvider;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvFileEditorProviderBase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public abstract class CsvTableEditorProvider extends CsvFileEditorProviderBase {

    @Override
    public @NotNull String getEditorTypeId() {
        return TABLE_EDITOR_TYPE_ID;
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

}
