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

    protected CsvBlock(@NotNull ASTNode node, CsvFormattingInfo formattingInfo) {
        super(node, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment());
        this.formattingInfo = formattingInfo;
    }

    @Override
    protected List<Block> buildChildren() {
        List<Block> blocks = new ArrayList<>();
        ASTNode node = myNode.getFirstChildNode();
        while (node != null) {
            if (node.getElementType() != TokenType.WHITE_SPACE) {
                Block block = new CsvBlock(node, formattingInfo);
                blocks.add(block);
            }
            node = node.getTreeNext();
        }
        return blocks;
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        Spacing spacing;
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
            int spaces = columnInfo.getMaxLength() - node.getTextLength();
            spacing = Spacing.createSpacing(spaces, spaces, 0, true, 0);
        } else {
            spacing = formattingInfo.getSpacingBuilder().getSpacing(this, child1, child2);
        }
        return spacing;
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }
}
