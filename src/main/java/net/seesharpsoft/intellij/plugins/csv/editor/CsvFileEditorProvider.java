package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.SingleRootFileViewProvider;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;

public class CsvFileEditorProvider implements FileEditorProvider {

    protected static boolean isCsvFile(VirtualFile file) {
        return file.getFileType() instanceof LanguageFileType && ((LanguageFileType) file.getFileType()).getLanguage().isKindOf(CsvLanguage.INSTANCE);
    }

    @Override
    public String getEditorTypeId() {
        return "csv-editor";
    }

    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return isCsvFile(file) && !SingleRootFileViewProvider.isTooLargeForContentLoading(file);
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

}
