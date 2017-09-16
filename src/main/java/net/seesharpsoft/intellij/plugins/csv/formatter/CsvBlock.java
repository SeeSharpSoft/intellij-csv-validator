package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.common.AbstractBlock;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CsvBlock extends AbstractBlock {
    protected CsvFormattingInfo formattingInfo;

    protected CsvBlock(@NotNull ASTNode node, @Nullable Alignment alignment, CsvFormattingInfo formattingInfo) {
        super(node, Wrap.createWrap(WrapType.NONE, false), alignment);
        this.formattingInfo = formattingInfo;
    }

    @Override
    protected List<Block> buildChildren() {
        List<Block> blocks = new ArrayList<>();
        ASTNode node = myNode.getFirstChildNode();
        while (node != null) {
            if (node.getElementType() != TokenType.WHITE_SPACE) {
                Block block = new CsvBlock(node, Alignment.createAlignment(), formattingInfo);
                blocks.add(block);
            }
            node = node.getTreeNext();
        }
        return blocks;
    }

    protected Block createBlock(ASTNode node) {
        if (node.getElementType() != TokenType.WHITE_SPACE) {
            return new CsvBlock(node, Alignment.createAlignment(), formattingInfo);
        }
        return null;
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        if (formattingInfo.getCodeStyleSettings().TABULARIZE && myNode.getElementType() == CsvTypes.RECORD) {
            ASTNode node = null;
            CsvFormattingInfo.ColumnInfo columnInfo = null;
            if (formattingInfo.getCodeStyleSettings().LEADING_WHITE_SPACES && child2 != null && (node = ((CsvBlock) child2).myNode).getElementType() == CsvTypes.FIELD) {
                columnInfo = formattingInfo.getColumnInfo(node);
            } else if (!formattingInfo.getCodeStyleSettings().LEADING_WHITE_SPACES && child1 != null && (node = ((CsvBlock) child1).myNode).getElementType() == CsvTypes.FIELD) {
                columnInfo = formattingInfo.getColumnInfo(node);
            }
            if (columnInfo == null) {
                return Spacing.createSpacing(0, 0, 0, true, 0);
            }
            int spacing = columnInfo.getMaxLength() - node.getTextLength();
            return Spacing.createSpacing(spacing, spacing, 0, true, 0);
        }
        Spacing spacing = formattingInfo.getSpacingBuilder().getSpacing(this, child1, child2);
        return spacing;
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }
}
