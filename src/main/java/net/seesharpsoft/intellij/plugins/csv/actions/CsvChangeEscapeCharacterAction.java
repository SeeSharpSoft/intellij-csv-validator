package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.psi.PsiFile;
import com.intellij.util.FileContentUtilCore;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import org.jetbrains.annotations.NotNull;

public class CsvChangeEscapeCharacterAction extends ToggleAction implements ActionUpdateThreadBGT {
    private final CsvEscapeCharacter myEscapeCharacter;

    CsvChangeEscapeCharacterAction(CsvEscapeCharacter escapeCharacter) {
        super(escapeCharacter.getDisplay());
        myEscapeCharacter = escapeCharacter;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (!CsvHelper.isCsvFile(psiFile)) {
            return false;
        }
        return CsvHelper.hasEscapeCharacterAttribute(psiFile) && CsvHelper.getEscapeCharacter(psiFile).equals(myEscapeCharacter);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent anActionEvent, boolean selected) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (!CsvHelper.isCsvFile(psiFile)) {
            return;
        }
        CsvFileAttributes.getInstance(psiFile.getProject()).setEscapeCharacter(psiFile, this.myEscapeCharacter);
        FileContentUtilCore.reparseFiles(CsvHelper.getVirtualFile(psiFile));

        FileEditor fileEditor = anActionEvent.getData(PlatformDataKeys.FILE_EDITOR);
        if (fileEditor != null) {
            fileEditor.selectNotify();
        }
    }
}
