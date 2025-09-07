package net.seesharpsoft.intellij.plugins.csv.components;

import com.intellij.util.xmlb.Converter;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class CsvValueSeparator extends CsvCustomizableElement {
    private final boolean myRequiresCustomLexer;

    public static final CsvValueSeparator COMMA = new CsvValueSeparator(",", "Comma (,)", "COMMA");
    public static final CsvValueSeparator SEMICOLON = new CsvValueSeparator(";", "Semicolon (;)", "SEMICOLON");
    public static final CsvValueSeparator PIPE = new CsvValueSeparator("|", "Pipe (|)", "PIPE");
    public static final CsvValueSeparator TAB = new CsvValueSeparator("\t", "Tab (↹)", "TAB");
    public static final CsvValueSeparator COLON = new CsvValueSeparator(":", "Colon (:)", "COLON");
    public static final CsvValueSeparator RS = new CsvValueSeparator("\u001E", "Record Separator ([RS])", "RS", true);

    public static CsvValueSeparator @NotNull [] values() {
        return new CsvValueSeparator[]{ COMMA, SEMICOLON, PIPE, TAB, COLON, RS };
    }

    private static @Nullable CsvValueSeparator getDefaultValueSeparator(@NotNull String character) {
        for (CsvValueSeparator defaultVS : values()) {
            if (defaultVS.getCharacter().equals(character) || defaultVS.getName().equals(character)) {
                return defaultVS;
            }
        }
        return null;
    }

    public static CsvValueSeparator create(String character) {
        if (character == null) {
            return null;
        }
        CsvValueSeparator defaultValueSeparator = getDefaultValueSeparator(character);
        return defaultValueSeparator == null ? new CsvValueSeparator(character) : defaultValueSeparator;
    }

    public static class CsvValueSeparatorConverter extends Converter<CsvValueSeparator> {
        public CsvValueSeparator fromString(@NotNull String value) {
            return CsvValueSeparator.create(XmlStringUtil.unescapeIllegalXmlChars(value));
        }

        public String toString(CsvValueSeparator value) {
            return XmlStringUtil.escapeIllegalXmlChars(value.getCharacter());
        }
    }

    public CsvValueSeparator(String myCharacter) {
        this(myCharacter, CUSTOM_DISPLAY + " (" + myCharacter + ")", CUSTOM_NAME, true);
    }

    private CsvValueSeparator(String character, String display, String name) {
        this(character, display, name, false);
    }

    private CsvValueSeparator(String character, String display, String name, boolean requiresCustomLexer) {
        super(character, display, Pattern.quote(character), name);
        myRequiresCustomLexer = requiresCustomLexer;
    }

    public boolean isValueSeparator(String text) {
        return getPattern().matcher(text).matches();
    }

    public boolean isValueSeparator(char c) {
        return getCharacter().charAt(0) == c;
    }

    public boolean requiresCustomLexer() {
        return myRequiresCustomLexer || isCustom();
    }
}
