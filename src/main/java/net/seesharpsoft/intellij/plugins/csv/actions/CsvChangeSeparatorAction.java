package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.psi.PsiFile;
import com.intellij.util.FileContentUtilCore;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.CsvSeparatorHolder;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.NotNull;

public class CsvChangeSeparatorAction extends ToggleAction {
    private String mySeparator;

    CsvChangeSeparatorAction(String separator, String mySeparatorTextArg) {
        super(mySeparatorTextArg);
        mySeparator = separator;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return false;
        }
        CsvFileAttributes csvFileAttributes = ServiceManager.getService(psiFile.getProject(), CsvFileAttributes.class);
        return csvFileAttributes.getFileSeparator(psiFile) != null && CsvCodeStyleSettings.getCurrentSeparator(anActionEvent.getProject(), psiFile).equals(mySeparator);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent anActionEvent, boolean selected) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }
        Language language = psiFile.getLanguage();
        if (!language.isKindOf(CsvLanguage.INSTANCE) || language instanceof CsvSeparatorHolder) {
            return;
        }
        CsvFileAttributes csvFileAttributes = ServiceManager.getService(psiFile.getProject(), CsvFileAttributes.class);
        csvFileAttributes.setFileSeparator(psiFile, this.mySeparator);
        FileContentUtilCore.reparseFiles(psiFile.getVirtualFile());

        FileEditor fileEditor = anActionEvent.getData(PlatformDataKeys.FILE_EDITOR);
        if (fileEditor != null) {
            fileEditor.selectNotify();
        }
    }
}
