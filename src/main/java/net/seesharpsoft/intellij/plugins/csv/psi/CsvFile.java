package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CsvFile extends PsiFileBase {

    private final LanguageFileType myFileType;
    private CsvColumnInfoMap<PsiElement> myColumnInfoMap;
    private long myColumnInfoMapModifiedStamp;

    public CsvFile(@NotNull FileViewProvider viewProvider, LanguageFileType fileType) {
        super(viewProvider, fileType.getLanguage());
        myFileType = fileType;
    }

    public CsvColumnInfoMap<PsiElement> getColumnInfoMap() {
        if (myColumnInfoMap == null || this.getModificationStamp() != myColumnInfoMapModifiedStamp) {
            myColumnInfoMapModifiedStamp = getModificationStamp();
            myColumnInfoMap = CsvHelper.createColumnInfoMap(this);
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

    @Override
    public PsiReference @NotNull [] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this);
    }
}