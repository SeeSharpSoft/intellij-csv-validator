package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.psi.PsiFile;
import com.intellij.util.FileContentUtilCore;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import org.jetbrains.annotations.NotNull;

public class CsvDefaultSeparatorAction extends ToggleAction {
    CsvDefaultSeparatorAction() {
        super("Project Default");
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return false;
        }
        CsvFileAttributes csvFileAttributes = ServiceManager.getService(psiFile.getProject(), CsvFileAttributes.class);
        return csvFileAttributes.getFileSeparator(psiFile) == null;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent anActionEvent, boolean selected) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }

        CsvFileAttributes.getInstance(psiFile.getProject()).resetValueSeparator(psiFile);
        FileContentUtilCore.reparseFiles(psiFile.getVirtualFile());

        FileEditor fileEditor = anActionEvent.getData(PlatformDataKeys.FILE_EDITOR);
        if (fileEditor != null) {
            fileEditor.selectNotify();
        }
    }
}
