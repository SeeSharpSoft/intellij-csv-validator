package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.util.FileContentUtilCore;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class CsvFile extends PsiFileBase {

    private class CsvEditorSettingsPropertyChangeListener implements PropertyChangeListener{
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            switch (evt.getPropertyName()) {
                case "defaultEscapeCharacter":
                case "defaultValueSeparator":
                    FileContentUtilCore.reparseFiles(CsvFile.this.getVirtualFile());
                    break;
                default:
                    // does not influence file
                    break;
            }
        }
    }

    private final LanguageFileType myFileType;
    private CsvColumnInfoMap<PsiElement> myColumnInfoMap;
    private long myColumnInfoMapModifiedStamp;

    public CsvFile(@NotNull FileViewProvider viewProvider, LanguageFileType fileType) {
        super(viewProvider, fileType.getLanguage());
        myFileType = fileType;
        CsvEditorSettings.getInstance().addPropertyChangeListener(new CsvEditorSettingsPropertyChangeListener());
    }

    public CsvColumnInfoMap<PsiElement> getColumnInfoMap() {
        if (myColumnInfoMap == null || this.getModificationStamp() != myColumnInfoMapModifiedStamp) {
            myColumnInfoMap = CsvHelper.createColumnInfoMap(this);
            myColumnInfoMapModifiedStamp = getModificationStamp();
        }
        return myColumnInfoMap;
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return myFileType;
    }

    @Override
    public String toString() {
        return String.format("%s File", myFileType.getName());
    }

    @Override
    public Icon getIcon(int flags) {
        return super.getIcon(flags);
    }
}