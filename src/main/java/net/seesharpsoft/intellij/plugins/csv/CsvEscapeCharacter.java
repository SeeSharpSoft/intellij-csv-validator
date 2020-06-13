package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.util.xmlb.Converter;

import java.util.Objects;
import java.util.regex.Pattern;

public class CsvEscapeCharacter {
    private final String myCharacter;
    private final String myDisplay;
    private final Pattern myPattern;
    private final String myRegexPattern;
    private final String myName;

    private static final String CUSTOM_NAME = "CUSTOM";
    private static final String CUSTOM_DISPLAY = "Custom";

    public static CsvEscapeCharacter QUOTE = new CsvEscapeCharacter("\"", "Double Quote (\")", "\"", "QUOTE");
    public static CsvEscapeCharacter BACKSLASH = new CsvEscapeCharacter("\\", "Backslash (\\)", "\\\\", "BACKSLASH");

    public static CsvEscapeCharacter[] values() {
        return new CsvEscapeCharacter[]{QUOTE, BACKSLASH};
    }

    public static CsvEscapeCharacter getDefaultEscapeCharacter(String character) {
        if (character != null) {
            switch (character) {
                case "QUOTE":
                case "\"":
                    return QUOTE;
                case "BACKSLASH":
                case "\\":
                    return BACKSLASH;
                default:
                    break;
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
        public CsvEscapeCharacter fromString(String value) {
            return CsvEscapeCharacter.create(value);
        }

        public String toString(CsvEscapeCharacter value) {
            return value.getCharacter();
        }
    }

    public CsvEscapeCharacter(String myCharacter) {
        this(myCharacter, CUSTOM_DISPLAY + " (" + myCharacter + ")", Pattern.quote(myCharacter), CUSTOM_NAME);
    }

    private CsvEscapeCharacter(String character, String display, String regexPattern, String name) {
        myCharacter = character;
        myDisplay = display;
        myRegexPattern = regexPattern;
        myPattern = Pattern.compile(Pattern.quote(myCharacter + "\""));
        myName = name;
    }

    public String getCharacter() {
        return myCharacter;
    }

    public String getDisplay() {
        return myDisplay;
    }

    public String getRegexPattern() {
        return myRegexPattern;
    }

    public boolean isEscapedQuote(String text) {
        return myPattern.matcher(text).matches();
    }

    public String getName() {
        return myName;
    }

    public boolean isCustom() {
        return CUSTOM_NAME.equals(getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCharacter(), isCustom());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CsvEscapeCharacter)) {
            return false;
        }
        CsvEscapeCharacter otherObj = (CsvEscapeCharacter)obj;
        return Objects.equals(otherObj.getCharacter(), this.getCharacter()) && Objects.equals(otherObj.isCustom(), this.isCustom());
    }

    @Override
    public String toString() {
        return getDisplay();
    }
}
