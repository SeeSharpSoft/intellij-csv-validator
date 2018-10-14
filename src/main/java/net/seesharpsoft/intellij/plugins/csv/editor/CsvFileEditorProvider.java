package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.SingleRootFileViewProvider;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class CsvFileEditorProvider implements FileEditorProvider {

    public static final String EDITOR_TYPE_ID = "csv-text-editor";

    protected static boolean isCsvFile(VirtualFile file) {
        return file.getFileType() instanceof LanguageFileType && ((LanguageFileType) file.getFileType()).getLanguage().isKindOf(CsvLanguage.INSTANCE);
    }

    @Override
    public String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    @Override
    public FileEditorPolicy getPolicy() {
        switch (CsvEditorSettingsExternalizable.getInstance().getEditorPrio()) {
            case TEXT_FIRST:
            case TEXT_ONLY:
                return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
//            case TABLE_ONLY:
            case TABLE_FIRST:
                return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
            default:
                throw new IllegalArgumentException("unhandled EditorPrio: " + CsvEditorSettingsExternalizable.getInstance().getEditorPrio());
        }
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return isCsvFile(file) && !SingleRootFileViewProvider.isTooLargeForContentLoading(file);
//                && CsvEditorSettingsExternalizable.getInstance().getEditorPrio() != CsvEditorSettingsExternalizable.EditorPrio.TABLE_ONLY;

    }

    protected void applySettings(EditorSettings editorSettings, CsvEditorSettingsExternalizable csvEditorSettingsExternalizable) {
        if (editorSettings == null || csvEditorSettingsExternalizable == null) {
            return;
        }
        editorSettings.setCaretRowShown(csvEditorSettingsExternalizable.isCaretRowShown());
        editorSettings.setUseSoftWraps(csvEditorSettingsExternalizable.isUseSoftWraps());
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        TextEditor textEditor = (TextEditor) TextEditorProvider.getInstance().createEditor(project, virtualFile);
        applySettings(textEditor.getEditor().getSettings(), CsvEditorSettingsExternalizable.getInstance());
        return textEditor;
    }

    @Override
    public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile file) {
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
    
}
