package net.seesharpsoft.intellij.plugins.csv.components;

import com.intellij.util.xmlb.Converter;
import com.intellij.xml.util.XmlStringUtil;

import java.util.Objects;
import java.util.regex.Pattern;

public class CsvValueSeparator {
    private final String myCharacter;
    private final String myDisplay;
    private final Pattern myPattern;
    private final String myName;
    private final boolean myRequiresCustomLexer;

    private static final String CUSTOM_NAME = "CUSTOM";
    private static final String CUSTOM_DISPLAY = "Custom";

    public static final CsvValueSeparator COMMA = new CsvValueSeparator(",", "Comma (,)", "COMMA");
    public static final CsvValueSeparator SEMICOLON = new CsvValueSeparator(";", "Semicolon (;)", "SEMICOLON");
    public static final CsvValueSeparator PIPE = new CsvValueSeparator("|", "Pipe (|)", "PIPE");
    public static final CsvValueSeparator TAB = new CsvValueSeparator("\t", "Tab (↹)", "TAB");
    public static final CsvValueSeparator COLON = new CsvValueSeparator(":", "Colon (:)", "COLON");
    public static final CsvValueSeparator RS = new CsvValueSeparator("\u001E", "Record Separator ([RS])", "RS", true);

    public static CsvValueSeparator getDefaultValueSeparator(String character) {
        if (character != null) {
            switch (character) {
                case "COMMA":
                case ",":
                    return COMMA;
                case "SEMICOLON":
                case ";":
                    return SEMICOLON;
                case "PIPE":
                case "|":
                    return PIPE;
                case "TAB":
                case "\t":
                    return TAB;
                case "COLON":
                case ":":
                    return COLON;
                case "RS":
                case "\u001E":
                    return RS;
                default:
                    break;
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

    public static CsvValueSeparator[] values() {
        return new CsvValueSeparator[]{COMMA, SEMICOLON, PIPE, TAB, COLON, RS};
    }

    public static class CsvValueSeparatorConverter extends Converter<CsvValueSeparator> {
        public CsvValueSeparator fromString(String value) {
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
        myCharacter = character;
        myDisplay = display;
        myPattern = Pattern.compile(Pattern.quote(myCharacter));
        myName = name;
        myRequiresCustomLexer = requiresCustomLexer;
    }

    public String getCharacter() {
        return myCharacter;
    }

    public String getDisplay() {
        return myDisplay;
    }

    public boolean isValueSeparator(String text) {
        return myPattern.matcher(text).matches();
    }

    public boolean isValueSeparator(char c) {
        return myCharacter.charAt(0) == c;
    }

    public String getName() {
        return myName;
    }

    public boolean isCustom() {
        return CUSTOM_NAME.equals(getName());
    }

    public boolean requiresCustomLexer() {
        return myRequiresCustomLexer || isCustom();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCharacter(), isCustom());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CsvValueSeparator)) {
            return false;
        }
        CsvValueSeparator otherObj = (CsvValueSeparator) obj;
        return Objects.equals(otherObj.getCharacter(), this.getCharacter()) && Objects.equals(otherObj.isCustom(), this.isCustom());
    }

    @Override
    public String toString() {
        return getDisplay();
    }
}
