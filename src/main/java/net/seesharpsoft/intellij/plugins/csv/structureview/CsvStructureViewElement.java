package net.seesharpsoft.intellij.plugins.csv.structureview;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvIconPovider;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.util.Iconable.ICON_FLAG_VISIBILITY;

public abstract class CsvStructureViewElement implements StructureViewTreeElement, SortableTreeElement, ItemPresentation {
    protected PsiElement element;

    public CsvStructureViewElement(PsiElement element) {
        this.element = element;
    }

    @Override
    public Object getValue() {
        return element;
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (element instanceof NavigationItem) {
            ((NavigationItem) element).navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        return element instanceof NavigationItem &&
                ((NavigationItem) element).canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return element instanceof NavigationItem &&
                ((NavigationItem) element).canNavigateToSource();
    }

    @Override
    public String getAlphaSortKey() {
        return element instanceof PsiNamedElement ? ((PsiNamedElement) element).getName() : null;
    }

    @Override
    public ItemPresentation getPresentation() {
        ItemPresentation presentation = element instanceof NavigationItem ?
                ((NavigationItem) element).getPresentation() : this;
        return presentation == null ? this : presentation;
    }

    @Nullable
    @Override
    public String getPresentableText() {
        return element.getText();
    }

    @Nullable
    @Override
    public String getLocationString() {
        return null;
    }

    @Nullable
    @Override
    public Icon getIcon(boolean unused) {
        return element.getIcon(ICON_FLAG_VISIBILITY);
    }

    public static class File extends CsvStructureViewElement {
        public File(PsiFile element) {
            super(element);
        }

        @Override
        public TreeElement[] getChildren() {
            if (element instanceof CsvFile) {
                CsvFile csvFile = (CsvFile)element;
                Map<Integer, CsvColumnInfo<PsiElement>> columnInfoMap = csvFile.getMyColumnInfoMap().getColumnInfos();
                TreeElement[] children = new TreeElement[columnInfoMap.size()];
                for (Map.Entry<Integer, CsvColumnInfo<PsiElement>> entry : columnInfoMap.entrySet()) {
                    CsvColumnInfo<PsiElement> columnInfo = entry.getValue();
                    PsiElement psiElement = columnInfo.getHeaderElement();
                    if (psiElement == null) {
                        psiElement = CsvHelper.createEmptyCsvField(element.getProject());
                    }
                    children[entry.getKey()] = new Header(psiElement, columnInfo);
                }
                return children;
            } else {
                return EMPTY_ARRAY;
            }
        }
    }

    private static class Header extends CsvStructureViewElement {
        private CsvColumnInfo<PsiElement> columnInfo;

        public Header(PsiElement element, CsvColumnInfo<PsiElement> columnInfo) {
            super(element);
            this.columnInfo = columnInfo;
        }

        @NotNull
        @Override
        public TreeElement[] getChildren() {
            int rowIndex = 0;
            List<PsiElement> elements = columnInfo.getElements();
            TreeElement[] children = new TreeElement[elements.size() - 1];
            for (PsiElement element : elements) {
                if (rowIndex > 0) {
                    children[rowIndex - 1] = new Field(element == null ? CsvHelper.createEmptyCsvField(element.getProject()) : element, rowIndex - 1);
                }
                ++rowIndex;
            }
            return children;
        }

        @Nullable
        @Override
        public String getLocationString() {
            return String.format("Header (%s entries)", columnInfo.getElements().size() - 1);
        }

        @Nullable
        @Override
        public Icon getIcon(boolean unused) {
            return CsvIconPovider.HEADER;
        }
    }

    private static class Field extends CsvStructureViewElement {
        private int rowIndex;

        public Field(PsiElement element, int rowIndex) {
            super(element);
            this.rowIndex = rowIndex;
        }

        @NotNull
        @Override
        public TreeElement[] getChildren() {
            return EMPTY_ARRAY;
        }

        @Nullable
        @Override
        public String getLocationString() {
            return String.format("(%s)", rowIndex + 1);
        }
    }
}