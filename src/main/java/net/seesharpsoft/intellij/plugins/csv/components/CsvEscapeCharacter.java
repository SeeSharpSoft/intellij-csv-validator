package net.seesharpsoft.intellij.plugins.csv.components;

import com.intellij.util.xmlb.Converter;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class CsvEscapeCharacter extends CsvCustomizableElement {
    private final String myStringPattern;

    public static CsvEscapeCharacter QUOTE = new CsvEscapeCharacter("\"", "Double Quote (\")", "\"", "QUOTE");
    public static CsvEscapeCharacter BACKSLASH = new CsvEscapeCharacter("\\", "Backslash (\\)", "\\\\", "BACKSLASH");

    public static CsvEscapeCharacter[] values() {
        return new CsvEscapeCharacter[]{ QUOTE, BACKSLASH };
    }

    private static @Nullable CsvEscapeCharacter getDefaultEscapeCharacter(@NotNull String character) {
        for (CsvEscapeCharacter defaultEC : values()) {
            if (defaultEC.getCharacter().equals(character) || defaultEC.getName().equals(character)) {
                return defaultEC;
            }
        }
        return null;
    }

    public static CsvEscapeCharacter create(String character) {
        if (character == null) {
            return null;
        }
        CsvEscapeCharacter defaultEscapeCharacter = getDefaultEscapeCharacter(character);
        return defaultEscapeCharacter == null ? new CsvEscapeCharacter(character) : defaultEscapeCharacter;
    }

    public static class CsvEscapeCharacterConverter extends Converter<CsvEscapeCharacter> {
        public CsvEscapeCharacter fromString(@NotNull String value) {
            return CsvEscapeCharacter.create(XmlStringUtil.unescapeIllegalXmlChars(value));
        }

        public String toString(CsvEscapeCharacter value) {
            return XmlStringUtil.escapeIllegalXmlChars(value.getCharacter());
        }
    }

    public CsvEscapeCharacter(String myCharacter) {
        this(myCharacter, CUSTOM_DISPLAY + " (" + myCharacter + ")", Pattern.quote(myCharacter), CUSTOM_NAME);
    }

    private CsvEscapeCharacter(String character, String display, String regexPattern, String name) {
        super(character, display, Pattern.quote(character + "\""), name);
        myStringPattern = regexPattern;
    }

    public boolean isEscapedQuote(String text) {
        return getPattern().matcher(text).matches();
    }

    public String getStringPattern() {
        return myStringPattern;
    }
}
