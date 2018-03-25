package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;

import java.util.HashMap;
import java.util.Map;

public class CsvFormattingInfo {
    private Map<Integer, CsvColumnInfo<ASTNode>> infoColumnMap;
    
    private Map<ASTNode, CsvColumnInfo<ASTNode>> reverseInfoColumnMap;

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
        buildReverseMap();
    }
    
    private void buildReverseMap() {
        reverseInfoColumnMap = new HashMap<>();
        for (CsvColumnInfo<ASTNode> columnInfo : infoColumnMap.values()) {
            for (ASTNode node : columnInfo.getElements()) {
                reverseInfoColumnMap.put(node, columnInfo);
            }
        }
    }

    public CsvColumnInfo getColumnInfo(ASTNode node) {
        return reverseInfoColumnMap.get(node);
    }

    public CsvColumnInfo getColumnInfo(int columnIndex) {
        return infoColumnMap.get(columnIndex);
    }

}
