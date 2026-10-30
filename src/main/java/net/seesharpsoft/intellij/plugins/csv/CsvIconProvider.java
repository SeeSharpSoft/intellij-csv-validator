package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CsvIconProvider extends IconProvider {

    public static final Icon FILE = IconLoader.getIcon("/media/icons/csv-icon.svg", CsvIconProvider.class);

    public static final Icon FIELD = IconLoader.getIcon("/media/icons/csv-field-icon.svg", CsvIconProvider.class);

    public static final Icon SEPARATOR = IconLoader.getIcon("/media/icons/actions/change-separator.svg", CsvIconProvider.class);

    @Nullable
    @Override
    public Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof CsvField) {
            return FIELD;
        }
        if (element instanceof CsvFile) {
            return ((CsvFile) element).getFileType().getIcon();
        }
        if (PsiHelper.getElementType(element) == CsvTypes.COMMA) {
            return SEPARATOR;
        }

        return null;
    }
}