package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiFile;
import com.intellij.util.FileContentUtilCore;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvSeparatorHolder;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import org.jetbrains.annotations.NotNull;

public class CsvChangeSeparatorAction extends ToggleAction {
    private final CsvValueSeparator mySeparator;

    CsvChangeSeparatorAction(CsvValueSeparator separator) {
        super(separator.getDisplay());
        mySeparator = separator;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (!CsvHelper.isCsvFile(psiFile)) {
            return false;
        }
        return CsvHelper.hasValueSeparatorAttribute(psiFile) && CsvHelper.getValueSeparator(psiFile).equals(mySeparator);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent anActionEvent, boolean selected) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (!CsvHelper.isCsvFile(psiFile)) {
            return;
        }
        FileType fileType = psiFile.getFileType();
        if (fileType instanceof CsvSeparatorHolder) {
            return;
        }
        CsvFileAttributes csvFileAttributes = psiFile.getProject().getService(CsvFileAttributes.class);
        csvFileAttributes.setFileSeparator(psiFile, this.mySeparator);
        FileContentUtilCore.reparseFiles(CsvHelper.getVirtualFile(psiFile));

        FileEditor fileEditor = anActionEvent.getData(PlatformDataKeys.FILE_EDITOR);
        if (fileEditor != null) {
            fileEditor.selectNotify();
        }
    }
}
