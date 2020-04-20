package net.seesharpsoft.intellij.plugins.csv;

import java.util.regex.Pattern;

public enum CsvValueSeparator {
    COMMA(",", "Comma (,)"),
    SEMICOLON(";", "Semicolon (;)"),
    PIPE("|", "Pipe (|)"),
    TAB("\t", "Tab (↹)"),
    COLON(":", "Colon (:)");

    private final String myCharacter;
    private final String myDisplay;
    private final Pattern myPattern;

    CsvValueSeparator(String character, String display) {
        myCharacter = character;
        myDisplay = display;
        myPattern = Pattern.compile(Pattern.quote(myCharacter));
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
}
