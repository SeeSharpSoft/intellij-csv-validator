package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvColorSettings;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public interface CsvHighlightingElement {
    TextAttributesKey[] getTextAttributesKeys();

    class TokenBased extends IElementType implements CsvHighlightingElement {
        public static final TokenBased COMMA = new TokenBased(CsvTypes.COMMA.toString(), CsvColorSettings.COMMA);
        public static final TokenBased QUOTE = new TokenBased(CsvTypes.QUOTE.toString(), CsvColorSettings.QUOTE);
        public static final TokenBased TEXT = new TokenBased(CsvTypes.TEXT.toString(), CsvColorSettings.TEXT);
        public static final TokenBased COMMENT = new TokenBased(CsvTypes.COMMENT.toString(), CsvColorSettings.COMMENT);
        public static final TokenBased BAD_CHARACTER = new TokenBased(HighlighterColors.BAD_CHARACTER.getExternalName(), CsvColorSettings.BAD_CHARACTER);

        private final TextAttributesKey[] myTextAttributesKeys;

        private TokenBased(@NonNls @NotNull String debugName, TextAttributesKey textAttributesKey) {
            super(debugName, CsvLanguage.INSTANCE);
            myTextAttributesKeys = new TextAttributesKey[]{textAttributesKey};
        }

        @Override
        public TextAttributesKey[] getTextAttributesKeys() {
            return myTextAttributesKeys;
        }
    }

    class ColumnBased extends TokenBased {
        private static final ColumnBased[] COLUMNS;

        static {
            COLUMNS = new ColumnBased[CsvColorSettings.MAX_COLUMN_COLORING_COLORS];
            for (int i = 0; i < CsvColorSettings.MAX_COLUMN_COLORING_COLORS; ++i) {
                COLUMNS[i] = new ColumnBased(String.format("Column %s", i), i);
            }
        }

        public static ColumnBased forIndex(int index) {
            return COLUMNS[index % CsvColorSettings.MAX_COLUMN_COLORING_COLORS];
        }

        private ColumnBased(@NonNls @NotNull String debugName, int index) {
            super(debugName, CsvColorSettings.getTextAttributesKeys(index));
        }
    }
}
