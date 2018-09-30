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

    protected static boolean isCsvFile(VirtualFile file) {
        return file.getFileType() instanceof LanguageFileType && ((LanguageFileType) file.getFileType()).getLanguage().isKindOf(CsvLanguage.INSTANCE);
    }

    @Override
    public String getEditorTypeId() {
        return "csv-table-editor";
    }

    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.NONE;
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return isCsvFile(file) && !SingleRootFileViewProvider.isTooLargeForContentLoading(file);
    }


    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new CsvTableEditor(project, virtualFile);
    }

}