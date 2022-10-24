package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.psi.PsiFile;
import com.intellij.util.FileContentUtilCore;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CsvCustomSeparatorAction extends ToggleAction {
    CsvCustomSeparatorAction() {
        super("Custom");
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (!CsvHelper.isCsvFile(psiFile)) {
            return false;
        }
        return CsvHelper.getValueSeparator(psiFile).isCustom();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent anActionEvent, boolean selected) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (!CsvHelper.isCsvFile(psiFile)) {
            return;
        }

        FileEditor fileEditor = anActionEvent.getData(PlatformDataKeys.FILE_EDITOR);
        CsvValueSeparator currentSeparator = CsvHelper.getValueSeparator(psiFile);
        String customValueSeparator = JOptionPane.showInputDialog(fileEditor == null ? null : fileEditor.getComponent(),
                "Value separator",
                currentSeparator.getCharacter());

        if (customValueSeparator == null) {
            return;
        }
        if (customValueSeparator.length() == 0 || customValueSeparator.contains(" ")) {
            JOptionPane.showMessageDialog(fileEditor == null ? null : fileEditor.getComponent(), "Value separator must have at least one character and no spaces!");
            return;
        }

        CsvFileAttributes csvFileAttributes = psiFile.getProject().getService(CsvFileAttributes.class);
        csvFileAttributes.setFileSeparator(psiFile, new CsvValueSeparator(customValueSeparator));
        FileContentUtilCore.reparseFiles(psiFile.getVirtualFile());

        if (fileEditor != null) {
            fileEditor.selectNotify();
        }
    }
}
