package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CsvElementType extends IElementType {
    public static final IElementType DOCUMENT_START = new CsvElementType("DOCUMENT_START");

    public CsvElementType(@NotNull @NonNls String debugName) {
        super(debugName, CsvLanguage.INSTANCE);
    }
}