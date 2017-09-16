package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CsvIconPovider extends IconProvider {

    public static final Icon FILE = IconLoader.getIcon("/media/icons/csv-icon.png");

    public static final Icon HEADER = IconLoader.getIcon("/media/icons/csv-header-icon.png");

    public static final Icon FIELD = IconLoader.getIcon("/media/icons/csv-field-icon.png");

    @Nullable
    @Override
    public Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof CsvField) {
            return FIELD;
        }
        if (element instanceof CsvFile) {
            return FILE;
        }
        return null;
    }
}