package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.psi.PsiFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import org.jetbrains.annotations.NotNull;

public class CsvStructureViewModel extends StructureViewModelBase
        implements StructureViewModel.ElementInfoProvider, StructureViewModel.ExpandInfoProvider {

    public CsvStructureViewModel(PsiFile psiFile) {
        super(psiFile, new CsvStructureViewElement.File(psiFile));
    }

    @Override
    @NotNull
    public Sorter[] getSorters() {
        return Sorter.EMPTY_ARRAY;
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return element instanceof CsvFile;
    }

    @Override
    public boolean isAutoExpand(@NotNull StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isSmartExpand() {
        return true;
    }
}