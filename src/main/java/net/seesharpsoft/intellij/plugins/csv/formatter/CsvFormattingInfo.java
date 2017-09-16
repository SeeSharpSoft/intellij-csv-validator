package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class CsvFormattingInfo {
    private Map<Integer, ColumnInfo> infoColumnMap;

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

    public CsvFormattingInfo(CodeStyleSettings codeStyleSettings, SpacingBuilder spacingBuilder, Map<Integer, ColumnInfo> infoColumnMap) {
        this.infoColumnMap = infoColumnMap;
        this.spacingBuilder = spacingBuilder;
        this.codeStyleSettings = codeStyleSettings;
    }

    public ColumnInfo getColumnInfo(ASTNode node) {
        for (ColumnInfo columnInfo : infoColumnMap.values()) {
            if (columnInfo.nodes.contains(node)) {
                return columnInfo;
            }
        }
        return null;
    }

    public ColumnInfo getColumnInfo(int columnIndex) {
        return infoColumnMap.get(columnIndex);
    }

    public static class ColumnInfo {

        public ColumnInfo(int columnIndex, int maxLength) {
            this.columnIndex = columnIndex;
            this.maxLength = maxLength;
            this.nodes = new ArrayList<>();
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        private int columnIndex;

        private int maxLength;

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        private Collection<ASTNode> nodes;

        public boolean addNode(ASTNode node) {
            return nodes.add(node);
        }
    }
}
