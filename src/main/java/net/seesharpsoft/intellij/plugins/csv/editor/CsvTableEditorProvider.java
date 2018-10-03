package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.SingleRootFileViewProvider;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;

public class CsvTableEditorProvider implements FileEditorProvider {

    public static final String EDITOR_TYPE_ID = "csv-table-editor";

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
                return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
//            case TABLE_ONLY:
            case TABLE_FIRST:
                return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
            default:
                throw new IllegalArgumentException("unhandled EditorPrio: " + CsvEditorSettingsExternalizable.getInstance().getEditorPrio());
        }
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return CsvEditorSettingsExternalizable.getInstance().getEditorPrio() != CsvEditorSettingsExternalizable.EditorPrio.TEXT_ONLY &&
                isCsvFile(file) && !SingleRootFileViewProvider.isTooLargeForContentLoading(file);
    }


    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new CsvTableEditor(project, virtualFile);
    }

}