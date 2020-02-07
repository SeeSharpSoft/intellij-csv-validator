package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

@SuppressWarnings({"checkstyle:membername", "checkstyle:visibilitymodifier"})
public class CsvCodeStyleSettings extends CustomCodeStyleSettings {

    public boolean SPACE_BEFORE_SEPARATOR = false;
    public boolean SPACE_AFTER_SEPARATOR = false;
    public boolean TRIM_LEADING_WHITE_SPACES = false;
    public boolean TRIM_TRAILING_WHITE_SPACES = false;
    public boolean TABULARIZE = true;
    public boolean WHITE_SPACES_OUTSIDE_QUOTES = true;
    public boolean LEADING_WHITE_SPACES = false;
    public boolean ENABLE_WIDE_CHARACTER_DETECTION = false;
    public boolean TREAT_AMBIGUOUS_CHARACTERS_AS_WIDE = false;

    public CsvCodeStyleSettings(CodeStyleSettings settings) {
        super("CsvCodeStyleSettings", settings);
    }
}
