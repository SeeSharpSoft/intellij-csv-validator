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
        // Defensive: avoid creating references for invalid/non-physical files.
        // Certain IDE operations (e.g., Safe Delete) may attempt to create SmartPointers
        // for file-level references. If the underlying VirtualFile has been invalidated
        // (e.g., file deleted/moved or non-physical), creating such pointers can throw
        // PluginException: PsiUtilCore.ensureValid(...). To prevent that, short-circuit
        // and return no references when this PsiFile isn't in a safe/valid state.
        if (!isValid()) {
            return PsiReference.EMPTY_ARRAY;
        }

        // Check associated virtual file state as additional safeguard
        var virtualFile = getVirtualFile();
        if (virtualFile == null || !virtualFile.isValid()) {
            return PsiReference.EMPTY_ARRAY;
        }

        // Skip non-physical files (light or in-memory) to avoid unexpected pointer creation
        if (!getViewProvider().isPhysical()) {
            return PsiReference.EMPTY_ARRAY;
        }

        return ReferenceProvidersRegistry.getReferencesFromProviders(this);
    }
}