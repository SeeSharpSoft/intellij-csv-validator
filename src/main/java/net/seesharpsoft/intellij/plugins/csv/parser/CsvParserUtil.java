package net.seesharpsoft.intellij.plugins.csv.parser;

import com.intellij.lang.PsiBuilder;
import net.seesharpsoft.intellij.plugins.csv.formatter.CsvCodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;

public class CsvParserUtil {

    public static boolean separator(PsiBuilder builder, int tokenType) {
        return builder.getTokenType() == CsvTypes.COMMA && builder.getTokenText().equals(CsvCodeStyleSettings.getCurrentSeparator(builder.getProject()));
    }

}
