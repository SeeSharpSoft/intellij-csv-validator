package net.seesharpsoft.intellij.plugins.csv;

import java.util.regex.Pattern;

public enum CsvEscapeCharacter {
    QUOTE("\"", "Double Quote (\")", "\""),
    BACKSLASH("\\", "Backslash (\\)", "\\\\");

    private final String myCharacter;
    private final String myDisplay;
    private final Pattern myPattern;
    private final String myRegexPattern;

    CsvEscapeCharacter(String character, String display, String regexPattern) {
        myCharacter = character;
        myDisplay = display;
        myRegexPattern = regexPattern;
        myPattern = Pattern.compile(Pattern.quote(myCharacter + "\""));
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
}
