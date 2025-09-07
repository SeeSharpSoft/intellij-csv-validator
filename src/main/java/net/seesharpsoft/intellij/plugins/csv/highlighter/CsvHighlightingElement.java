package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public interface CsvHighlightingElement {
    TextAttributesKey[] getTextAttributesKeys();

    class TokenBased extends IElementType implements CsvHighlightingElement {
        public static final TokenBased COMMA = new TokenBased(CsvTypes.COMMA.toString(), CsvTextAttributeKeys.COMMA);
        public static final TokenBased QUOTE = new TokenBased(CsvTypes.QUOTE.toString(), CsvTextAttributeKeys.QUOTE);
        public static final TokenBased TEXT = new TokenBased(CsvTypes.TEXT.toString(), CsvTextAttributeKeys.TEXT);
        public static final TokenBased COMMENT = new TokenBased(CsvTypes.COMMENT.toString(), CsvTextAttributeKeys.COMMENT);
        public static final TokenBased BAD_CHARACTER = new TokenBased(HighlighterColors.BAD_CHARACTER.getExternalName(), CsvTextAttributeKeys.BAD_CHARACTER);

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
            COLUMNS = new ColumnBased[CsvTextAttributeKeys.MAX_COLUMN_COLORING_COLORS];
            for (int i = 0; i < CsvTextAttributeKeys.MAX_COLUMN_COLORING_COLORS; ++i) {
                COLUMNS[i] = new ColumnBased(String.format("Column %s", i), i);
            }
        }

        public static ColumnBased forIndex(int index) {
            return COLUMNS[index % CsvTextAttributeKeys.MAX_COLUMN_COLORING_COLORS];
        }

        private ColumnBased(@NonNls @NotNull String debugName, int index) {
            super(debugName, CsvTextAttributeKeys.getTextAttributesKeys(index));
        }
    }
}
