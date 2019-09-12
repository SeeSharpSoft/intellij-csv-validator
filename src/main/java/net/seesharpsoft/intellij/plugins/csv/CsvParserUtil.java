package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;

public final class CsvParserUtil {

    private CsvParserUtil() {
        // static utility class
    }

    public static boolean separator(PsiBuilder builder, int tokenType) {
        if (builder.getTokenType() == CsvTypes.COMMA) {
            PsiFile currentFile = builder.getUserDataUnprotected(FileContextUtil.CONTAINING_FILE_KEY);
            if (currentFile == null) {
                throw new UnsupportedOperationException("parser requires containing file");
            }
            return builder.getTokenText().equals(
                    CsvCodeStyleSettings.getCurrentSeparator(currentFile)
            );
        }
        return false;
    }

}
