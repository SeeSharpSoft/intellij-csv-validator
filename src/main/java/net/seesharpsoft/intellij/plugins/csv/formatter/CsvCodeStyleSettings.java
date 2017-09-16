package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class CsvCodeStyleSettings extends CustomCodeStyleSettings {
    public CsvCodeStyleSettings(CodeStyleSettings settings) {
        super("CsvCodeStyleSettings", settings);
    }

    public boolean TRIM_LEADING_WHITE_SPACES = false;

    public boolean TRIM_TRAILING_WHITE_SPACES = false;

    public boolean TABULARIZE = true;

    public boolean LEADING_WHITE_SPACES = false;
}