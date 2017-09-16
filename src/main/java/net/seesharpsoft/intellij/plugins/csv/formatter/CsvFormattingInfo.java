package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;

import java.util.Map;

public class CsvFormattingInfo {
    private Map<Integer, CsvColumnInfo<ASTNode>> infoColumnMap;

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
        this.infoColumnMap = infoColumnMap;
        this.spacingBuilder = spacingBuilder;
        this.codeStyleSettings = codeStyleSettings;
    }

    public CsvColumnInfo getColumnInfo(ASTNode node) {
        for (CsvColumnInfo columnInfo : infoColumnMap.values()) {
            if (columnInfo.containsElement(node)) {
                return columnInfo;
            }
        }
        return null;
    }

    public CsvColumnInfo getColumnInfo(int columnIndex) {
        return infoColumnMap.get(columnIndex);
    }

}
