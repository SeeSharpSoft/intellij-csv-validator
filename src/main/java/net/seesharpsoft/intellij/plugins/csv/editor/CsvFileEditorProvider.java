package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.diff.editor.DiffViewerVirtualFile;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.SingleRootFileViewProvider;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class CsvFileEditorProvider implements AsyncFileEditorProvider, DumbAware {

    public static final String EDITOR_TYPE_ID = "csv-text-editor";

    public static boolean acceptCsvFile(@NotNull Project project, @NotNull VirtualFile file) {
        try {
            return !SingleRootFileViewProvider.isTooLargeForContentLoading(file)
                    && !SingleRootFileViewProvider.isTooLargeForIntelligence(file)
                    && !(file instanceof DiffViewerVirtualFile)
                    && CsvHelper.isCsvFile(project, file);
        } catch(Exception exc) {
            return false;
        }
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        switch (CsvEditorSettings.getInstance().getEditorPrio()) {
            case TEXT_FIRST:
            case TEXT_ONLY:
                return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
            case TABLE_FIRST:
                return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
            default:
                throw new IllegalArgumentException("unhandled EditorPrio: " + CsvEditorSettings.getInstance().getEditorPrio());
        }
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return CsvFileEditorProvider.acceptCsvFile(project, file);
    }

    protected void applySettings(EditorSettings editorSettings, CsvEditorSettings csvEditorSettings) {
        if (editorSettings == null || csvEditorSettings == null) {
            return;
        }
        editorSettings.setCaretRowShown(csvEditorSettings.isCaretRowShown());
        editorSettings.setUseSoftWraps(csvEditorSettings.isUseSoftWraps());
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return createEditorAsync(project, virtualFile).build();
    }

    @Override
    public @NotNull FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile file) {
        return TextEditorProvider.getInstance().readState(sourceElement, project, file);
    }

    @Override
    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        TextEditorProvider.getInstance().writeState(state, project, targetElement);
    }

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        TextEditorProvider.getInstance().disposeEditor(editor);
    }

    @NotNull
    @Override
    public Builder createEditorAsync(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new Builder() {
            @Override
            public @NotNull FileEditor build() {
                TextEditorProvider provider = TextEditorProvider.getInstance();
                TextEditor textEditor = (TextEditor) provider.createEditor(project, virtualFile);
                applySettings(textEditor.getEditor().getSettings(), CsvEditorSettings.getInstance());
                return textEditor;
            }
        };
    }
}
