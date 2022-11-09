package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.fileEditor.AsyncFileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvFileEditorProvider;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableEditorSwing;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class CsvTableEditorProvider implements AsyncFileEditorProvider, DumbAware {

    public static final String EDITOR_TYPE_ID = "csv-table-editor";

    @Override
    public String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    @Override
    public FileEditorPolicy getPolicy() {
        switch (CsvEditorSettings.getInstance().getEditorPrio()) {
            case TEXT_FIRST:
            case TEXT_ONLY:
                return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
            case TABLE_FIRST:
                return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
            default:
                throw new IllegalArgumentException("unhandled EditorPrio: " + CsvEditorSettings.getInstance().getEditorPrio());
        }
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
    public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile file) {
        return CsvTableEditorState.create(sourceElement, project, file);
    }

    @Override
    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        if (!(state instanceof CsvTableEditorState)) {
            return;
        }
        CsvTableEditorState csvTableEditorState = (CsvTableEditorState) state;
        csvTableEditorState.write(project, targetElement);
    }

    @NotNull
    @Override
    public Builder createEditorAsync(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new Builder() {
            @Override
            public FileEditor build() {
                return new CsvTableEditorSwing(project, virtualFile);
            }
        };
    }
}
