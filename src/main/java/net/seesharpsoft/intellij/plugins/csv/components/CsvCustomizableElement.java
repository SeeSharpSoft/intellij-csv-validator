package net.seesharpsoft.intellij.plugins.csv.components;

import java.util.Objects;
import java.util.regex.Pattern;

public class CsvCustomizableElement {
    private final String myCharacter;
    private final String myDisplay;
    private final Pattern myPattern;
    private final String myName;

    protected static final String CUSTOM_NAME = "CUSTOM";
    protected static final String CUSTOM_DISPLAY = "Custom";

    protected CsvCustomizableElement(String character, String display, String regexPattern, String name) {
        myCharacter = character;
        myDisplay = display;
        myPattern = Pattern.compile(regexPattern);
        myName = name;
    }

    public String getCharacter() {
        return myCharacter;
    }

    public String getDisplay() {
        return myDisplay;
    }

    public Pattern getPattern() { return myPattern; }

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
        if (!(obj instanceof CsvCustomizableElement otherObj) || !obj.getClass().equals(this.getClass())) {
            return false;
        }
        return Objects.equals(otherObj.getCharacter(), this.getCharacter()) && Objects.equals(otherObj.isCustom(), this.isCustom());
    }

    @Override
    public String toString() {
        return getDisplay();
    }
}
