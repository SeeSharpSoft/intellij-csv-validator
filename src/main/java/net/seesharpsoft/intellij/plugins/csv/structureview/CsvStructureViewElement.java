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
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvIconProvider;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorSettingsExternalizable;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.intellij.openapi.util.Iconable.ICON_FLAG_VISIBILITY;

public abstract class CsvStructureViewElement implements StructureViewTreeElement, SortableTreeElement, ItemPresentation {
    protected PsiElement myElement;

    public CsvStructureViewElement(PsiElement element) {
        this.myElement = element;
    }

    @Override
    public Object getValue() {
        return myElement;
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (myElement instanceof NavigationItem) {
            ((NavigationItem) myElement).navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        return myElement instanceof NavigationItem &&
                ((NavigationItem) myElement).canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return myElement instanceof NavigationItem &&
                ((NavigationItem) myElement).canNavigateToSource();
    }

    @Override
    public String getAlphaSortKey() {
        return myElement instanceof PsiNamedElement ? ((PsiNamedElement) myElement).getName() : null;
    }

    @Override
    public ItemPresentation getPresentation() {
        ItemPresentation presentation = myElement instanceof NavigationItem ?
                ((NavigationItem) myElement).getPresentation() : this;
        return presentation == null ? this : presentation;
    }

    @Nullable
    @Override
    public String getPresentableText() {
        return myElement.getText();
    }

    @Nullable
    @Override
    public String getLocationString() {
        return null;
    }

    @Nullable
    @Override
    public Icon getIcon(boolean unused) {
        return myElement.getIcon(ICON_FLAG_VISIBILITY);
    }

    public static class File extends CsvStructureViewElement {
        public File(PsiFile element) {
            super(element);
        }

        protected List<PsiElement> getElements(CsvColumnInfo<PsiElement> columnInfo, int maxRowNumbers) {
            return columnInfo.getElements().stream().limit(maxRowNumbers).collect(Collectors.toList());
        }

        @Override
        public TreeElement[] getChildren() {
            if (myElement instanceof CsvFile) {
                CsvFile csvFile = (CsvFile) myElement;
                CsvColumnInfoMap csvColumnInfoMap = csvFile.getMyColumnInfoMap();
                int maxRowNumbers = csvColumnInfoMap.getColumnInfo(0).getSize();
                if (csvColumnInfoMap.hasEmptyLastLine() && CsvEditorSettingsExternalizable.getInstance().isFileEndLineBreak()) {
                    --maxRowNumbers;
                }
                Map<Integer, CsvColumnInfo<PsiElement>> columnInfoMap = csvColumnInfoMap.getColumnInfos();
                TreeElement[] children = new TreeElement[columnInfoMap.size()];
                for (Map.Entry<Integer, CsvColumnInfo<PsiElement>> entry : columnInfoMap.entrySet()) {
                    CsvColumnInfo<PsiElement> columnInfo = entry.getValue();
                    PsiElement psiElement = columnInfo.getHeaderElement();
                    if (psiElement == null) {
                        psiElement = CsvHelper.createEmptyCsvField(myElement.getProject());
                    }
                    children[entry.getKey()] = new Header(psiElement, getElements(columnInfo, maxRowNumbers));
                }
                return children;
            } else {
                return EMPTY_ARRAY;
            }
        }
    }

    public static class Header extends CsvStructureViewElement {
        private List<PsiElement> myElements;

        public Header(PsiElement element, List<PsiElement> elements) {
            super(element);
            this.myElements = elements;
        }

        @NotNull
        @Override
        public TreeElement[] getChildren() {
            int rowIndex = 0;
            TreeElement[] children = new TreeElement[Math.max(0, this.myElements.size() - 1)];
            for (PsiElement element : this.myElements) {
                if (rowIndex > 0) {
                    children[rowIndex - 1] = new Field(element == null ? CsvHelper.createEmptyCsvField(this.myElement.getProject()) : element, rowIndex - 1);
                }
                ++rowIndex;
            }
            return children;
        }

        @Nullable
        @Override
        public String getLocationString() {
            return String.format("Header (%s entries)", this.myElements.size() - 1);
        }

        @Nullable
        @Override
        public Icon getIcon(boolean unused) {
            return CsvIconProvider.HEADER;
        }
    }

    public static class Field extends CsvStructureViewElement {
        private int myRowIndex;

        public Field(PsiElement element, int rowIndex) {
            super(element);
            this.myRowIndex = rowIndex;
        }

        @NotNull
        @Override
        public TreeElement[] getChildren() {
            return EMPTY_ARRAY;
        }

        @Nullable
        @Override
        public String getLocationString() {
            return String.format("(%s)", myRowIndex + 1);
        }
    }
}