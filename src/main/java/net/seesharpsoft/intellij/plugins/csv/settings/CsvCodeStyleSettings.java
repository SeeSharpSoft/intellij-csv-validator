package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

@SuppressWarnings({"checkstyle:membername", "checkstyle:visibilitymodifier"})
public class CsvCodeStyleSettings extends CustomCodeStyleSettings {

    public boolean SPACE_BEFORE_SEPARATOR = false;
    public boolean SPACE_AFTER_SEPARATOR = false;
    public boolean TRIM_LEADING_WHITE_SPACES = false;
    public boolean TRIM_TRAILING_WHITE_SPACES = false;

    public CsvCodeStyleSettings(CodeStyleSettings settings) {
        super("CsvCodeStyleSettings", settings);
    }
}
