package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CsvTokenType extends IElementType {
    public CsvTokenType(@NotNull @NonNls String debugName) {
        super(debugName, CsvLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        switch(super.toString()) {
            case "COMMA":
                return "Value Separator";
            case "QUOTE":
                return "Quote";
            case "TEXT":
            case "ESCAPED_TEXT":
                return "Text";
            case "COMMENT":
                return "Comment";
            case "CRLF":
                return "<Enter>";
            default:
                return super.toString();
        }
    }
}