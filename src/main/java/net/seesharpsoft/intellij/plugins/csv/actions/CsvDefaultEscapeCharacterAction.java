package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.psi.PsiFile;
import com.intellij.util.FileContentUtilCore;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import org.jetbrains.annotations.NotNull;

public class CsvDefaultEscapeCharacterAction extends ToggleAction implements ActionUpdateThreadBGT {
    CsvDefaultEscapeCharacterAction() {
        super("Project Default");
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (!CsvHelper.isCsvFile(psiFile)) {
            return false;
        }
        return !CsvHelper.hasEscapeCharacterAttribute(psiFile);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent anActionEvent, boolean selected) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (!CsvHelper.isCsvFile(psiFile)) {
            return;
        }
        CsvFileAttributes.getInstance(psiFile.getProject()).resetEscapeSeparator(psiFile);
        FileContentUtilCore.reparseFiles(CsvHelper.getVirtualFile(psiFile));

        FileEditor fileEditor = anActionEvent.getData(PlatformDataKeys.FILE_EDITOR);
        if (fileEditor != null) {
            fileEditor.selectNotify();
        }
    }
}
