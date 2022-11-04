package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;

import java.util.Map;

public class CsvFormattingInfo {

    private SpacingBuilder mySpacingBuilder;
    private CodeStyleSettings myCodeStyleSettings;

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
