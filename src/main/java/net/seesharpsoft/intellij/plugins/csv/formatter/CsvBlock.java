package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
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
        List<CsvBlock> blocks = buildChildrenRecursive(getNode().getFirstChildNode());
        List<Block> result = new ArrayList<>();
        CsvColumnInfo currentColumnInfo = null;
        CsvBlockField currentField = null;
        for (CsvBlock block : blocks) {
            IElementType elementType = block.getElementType();
            CsvBlockElement blockElement = (CsvBlockElement)block;
            if (elementType == TokenType.WHITE_SPACE || elementType == CsvTypes.RECORD) {
                continue;
            } else if (elementType == CsvTypes.FIELD) {
                currentField = (CsvBlockField)block;
                currentColumnInfo = formattingInfo.getColumnInfo(block.getNode());
            }
            blockElement.setField(currentField);
            blockElement.setColumnInfo(currentColumnInfo);
            if (block.getTextLength() == 0) {
                continue;
            }
            result.add(block);
        }
        return result;
    }
    
    private List<CsvBlock> buildChildrenRecursive(ASTNode node) {
        List<CsvBlock> blocks = new ArrayList<>();
        while (node != null) {
            if (node.getElementType() == CsvTypes.FIELD) {
                blocks.add(new CsvBlockField(node, formattingInfo));
            } else {
                CsvBlockElement block = new CsvBlockElement(node, formattingInfo);
                blocks.add(block);
                blocks.addAll(buildChildrenRecursive(node.getFirstChildNode()));
            }
            node = node.getTreeNext();
        }
        return blocks;
    }
    
    @Override
    public Indent getIndent() {
        return null;
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        Spacing spacing = null;
        if (child1 != null) {
            CsvBlockElement block1 = (CsvBlockElement) child1;
            CsvBlockElement block2 = (CsvBlockElement) child2;
            if (formattingInfo.getCsvCodeStyleSettings().TABULARIZE && isTabularizeSpacingRequired(block1, block2)) {
                int spaces = block2.getColumnInfo().getMaxLength() - block2.getField().getTextLength() + getAdditionalSpaces(block1, block2);
                spacing = Spacing.createSpacing(spaces, spaces, 0, true, 0);
            } else {
                spacing = formattingInfo.getSpacingBuilder().getSpacing(this, child1, child2);
            }
        }
        return spacing;
    }

    private boolean isTabularizeSpacingRequired(@NotNull CsvBlockElement block1, @NotNull CsvBlockElement block2) {
        return isAnyBlockASpacingSeparator(block1, block2) &&
                (formattingInfo.getCsvCodeStyleSettings().WHITE_SPACES_OUTSIDE_QUOTES ||
                (!CsvFormatHelper.isQuotedField(block1) && !CsvFormatHelper.isQuotedField(block2)));
    }

    private boolean isAnyBlockASpacingSeparator(@NotNull CsvBlockElement block1, @NotNull CsvBlockElement block2) {
        return (block2.getElementType() == CsvTypes.COMMA && !formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES) ||
                        (block2.getElementType() != CsvTypes.CRLF && (block1.getElementType() == CsvTypes.COMMA || block1.getElementType() == CsvTypes.CRLF)&& formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES);
    }

    protected int getAdditionalSpaces(@Nullable CsvBlock child1, @NotNull CsvBlock child2) {
        if ((formattingInfo.getCsvCodeStyleSettings().SPACE_AFTER_SEPARATOR && child1 != null && child1.getNode().getElementType() == CsvTypes.COMMA && child2.getElementType() != CsvTypes.CRLF)
                || (formattingInfo.getCsvCodeStyleSettings().SPACE_BEFORE_SEPARATOR && child2 != null && child2.getNode().getElementType() == CsvTypes.COMMA)) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean isLeaf() {
        return getNode().getFirstChildNode() == null;
    }

    public final IElementType getElementType() {
        return getNode().getElementType();
    }

    public int getTextLength() {
        return getTextRange().getLength();
    }
    
}
