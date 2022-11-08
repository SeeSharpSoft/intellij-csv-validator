package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.SpacingBuilder;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;

public class CsvFormattingInfo {

    private final SpacingBuilder mySpacingBuilder;
    private final CodeStyleSettings myCodeStyleSettings;

    public SpacingBuilder getSpacingBuilder() {
        return mySpacingBuilder;
    }

    public CsvCodeStyleSettings getCsvCodeStyleSettings() {
        return myCodeStyleSettings.getCustomSettings(CsvCodeStyleSettings.class);
    }

    public CsvFormattingInfo(CodeStyleSettings codeStyleSettings, SpacingBuilder spacingBuilder) {
        this.mySpacingBuilder = spacingBuilder;
        this.myCodeStyleSettings = codeStyleSettings;
    }
}
