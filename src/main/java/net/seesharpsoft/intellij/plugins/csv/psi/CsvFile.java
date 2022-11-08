package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CsvFile extends PsiFileBase {

    private final FileType myFileType;

    public CsvFile(@NotNull FileViewProvider viewProvider, FileType fileType) {
        super(viewProvider, CsvLanguage.INSTANCE);
        myFileType = fileType;
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return myFileType;
    }

    @Override
    public String toString() {
        return String.format("%s File", myFileType.getName().toUpperCase());
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