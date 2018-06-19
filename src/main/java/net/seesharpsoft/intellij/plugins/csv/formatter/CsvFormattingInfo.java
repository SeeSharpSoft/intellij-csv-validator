package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;

import java.util.Map;

public class CsvFormattingInfo extends CsvColumnInfoMap<ASTNode> {

    public SpacingBuilder getSpacingBuilder() {
        return spacingBuilder;
    }

    private SpacingBuilder spacingBuilder;

    public CsvCodeStyleSettings getCsvCodeStyleSettings() {
        return codeStyleSettings.getCustomSettings(CsvCodeStyleSettings.class);
    }

    public CodeStyleSettings getCodeStyleSettings() {
        return codeStyleSettings;
    }

    private CodeStyleSettings codeStyleSettings;

    public CsvFormattingInfo(CodeStyleSettings codeStyleSettings, SpacingBuilder spacingBuilder, Map<Integer, CsvColumnInfo<ASTNode>> infoColumnMap) {
        super(infoColumnMap);
        this.spacingBuilder = spacingBuilder;
        this.codeStyleSettings = codeStyleSettings;
    }
}
