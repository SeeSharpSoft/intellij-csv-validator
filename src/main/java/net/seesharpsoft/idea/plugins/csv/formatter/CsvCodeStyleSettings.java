package net.seesharpsoft.idea.plugins.csv.formatter;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class CsvCodeStyleSettings extends CustomCodeStyleSettings {
    public CsvCodeStyleSettings(CodeStyleSettings settings) {
        super("CsvCodeStyleSettings", settings);
    }

    public boolean REMOVE_EMPTY_LINES = false;

    public boolean TRIM_LEADING_WHITE_SPACES = false;

    public boolean TRIM_TRAILING_WHITE_SPACES = false;

    public boolean TABULARIZE = false;

    public boolean LEADING_WHITE_SPACES = false;
}