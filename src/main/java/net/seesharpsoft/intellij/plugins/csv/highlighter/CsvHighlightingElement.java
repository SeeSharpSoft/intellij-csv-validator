package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Font;

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
            // Use a neutral language to avoid element type id issues across IDE versions
            super(debugName, Language.ANY);
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

    /**
     * A tab used as value separator. Its background comes from the CSV settings rather than the color scheme;
     * editor highlighters resolve it when created, and settings changes reparse open files to recreate them.
     */
    final class TabSeparator extends IElementType implements CsvHighlightingElement {
        public static final TabSeparator INSTANCE = new TabSeparator();

        private TabSeparator() {
            super("TAB_SEPARATOR", Language.ANY);
        }

        @Override
        public TextAttributesKey[] getTextAttributesKeys() {
            Color color = CsvEditorSettings.getInstance().getTabHighlightColor();
            if (color == null) return TokenBased.COMMA.getTextAttributesKeys();
            // Keys keep the defaults of their first registration, so each configured color needs its own key.
            TextAttributesKey background = TextAttributesKey.createTextAttributesKey(
                    "CSV_TAB_SEPARATOR_" + Integer.toHexString(color.getRGB()),
                    new TextAttributes(null, color, null, null, Font.PLAIN));
            return new TextAttributesKey[]{CsvTextAttributeKeys.COMMA, background};
        }
    }
}
