package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.common.AbstractBlock;
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
        List<Block> blocks = new ArrayList<>();
        ASTNode node = getNode().getFirstChildNode();
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
        Spacing spacing = null;
        if (formattingInfo.getCsvCodeStyleSettings().TABULARIZE) {
            CsvBlock block1 = child1 == null ? null : (CsvBlock) child1;
            CsvBlock block2 = child2 == null ? null : (CsvBlock) child2;
            if (!formattingInfo.getCsvCodeStyleSettings().WHITE_SPACES_OUTSIDE_QUOTES) {
                spacing = getTabularizeInsideQuoteSpacing(block1, block2);
            } else {
                spacing = getTabularizeOutsideQuoteSpacing(block1, block2);
            }
        }
        if (spacing == null) {
            spacing = formattingInfo.getSpacingBuilder().getSpacing(this, child1, child2);
        }
        return spacing;
    }

    private Spacing getTabularizeOutsideQuoteSpacing(@Nullable CsvBlock child1, @NotNull CsvBlock child2) {
        Spacing spacing = null;
        if (getNode().getElementType() == CsvTypes.RECORD) {
            spacing = getSpacingForFields(child1, child2);
        } else if (formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES && getNode().getTreeParent() == null) {
            spacing = getSpacingForRecords(child1, child2);
        }
        return spacing;
    }

    private Spacing getTabularizeInsideQuoteSpacing(@Nullable CsvBlock child1, @NotNull CsvBlock child2) {
        Spacing spacing = null;
        if (getNode().getElementType() == CsvTypes.RECORD && !CsvFormatHelper.isFieldQuoted(child1) && !CsvFormatHelper.isFieldQuoted(child2)) {
            spacing = getSpacingForFields(child1, child2);
        } else if (getNode().getTreeParent() == null && formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES &&
                !CsvFormatHelper.isFirstFieldOfRecordQuoted(child1) && !CsvFormatHelper.isFirstFieldOfRecordQuoted(child2)) {
            spacing = getSpacingForRecords(child1, child2);
        } else if (CsvFormatHelper.isFieldQuoted(this)) {
            if (child1 != null && child1.getNode().getElementType() == CsvTypes.QUOTE && child2 != null) {
                if (formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES) {
                    // add spaces at the beginning
                    spacing = getSpacingOfFieldNode();
                } else {
                    // trim spaces at the beginning
                    spacing = Spacing.createSpacing(0, 0, 0, true, formattingInfo.getCodeStyleSettings().KEEP_BLANK_LINES_IN_CODE);
                }
            } else if (child2 != null && child2.getNode().getElementType() == CsvTypes.QUOTE && child1 != null) {
                if (!formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES) {
                    // add spaces at the end
                    spacing = getSpacingOfFieldNode();
                } else {
                    // trim spaces at the end
                    spacing = Spacing.createSpacing(0, 0, 0, true, formattingInfo.getCodeStyleSettings().KEEP_BLANK_LINES_IN_CODE);
                }
            }
        }
        return spacing;
    }

    private Spacing getSpacingOfFieldNode() {
        CsvColumnInfo columnInfo = formattingInfo.getColumnInfo(this.getNode());
        int textLength = CsvFormatHelper.getTextLength(getNode(), formattingInfo.getCodeStyleSettings());
        int spaces = columnInfo.getMaxLength() - textLength;
        return Spacing.createSpacing(spaces, spaces, 0, true, formattingInfo.getCodeStyleSettings().KEEP_BLANK_LINES_IN_CODE);
    }

    private Spacing getSpacingForRecords(@Nullable CsvBlock child1, @Nullable CsvBlock child2) {
        Spacing spacing;
        Block fieldBlock = null;
        CsvColumnInfo columnInfo = null;
        if (child2 != null && child2.getNode().getElementType() == CsvTypes.RECORD) {
            columnInfo = formattingInfo.getColumnInfo(0);
            fieldBlock = child2.getSubBlocks().get(0);
        }
        int spaces = 0;
        if (columnInfo != null) {
            spaces += columnInfo.getMaxLength() - fieldBlock.getTextRange().getLength();
        }
        spacing = Spacing.createSpacing(spaces, spaces, 0, true, formattingInfo.getCodeStyleSettings().KEEP_BLANK_LINES_IN_CODE);
        return spacing;
    }

    private Spacing getSpacingForFields(@Nullable CsvBlock child1, @NotNull CsvBlock child2) {
        Spacing spacing;
        ASTNode node = null;
        CsvColumnInfo columnInfo = null;
        if (formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES && child2 != null && (node = child2.getNode()).getElementType() == CsvTypes.FIELD) {
            columnInfo = formattingInfo.getColumnInfo(node);
        } else if (!formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES && child1 != null && (node = child1.getNode()).getElementType() == CsvTypes.FIELD) {
            columnInfo = formattingInfo.getColumnInfo(node);
        }
        int spaces = getAdditionalSpaces(child1, child2);
        if (columnInfo != null) {
            spaces += columnInfo.getMaxLength() - node.getTextLength();
        }
        spacing = Spacing.createSpacing(spaces, spaces, 0, true, formattingInfo.getCodeStyleSettings().KEEP_BLANK_LINES_IN_CODE);
        return spacing;
    }

    private int getAdditionalSpaces(@Nullable CsvBlock child1, @NotNull CsvBlock child2) {
        if ((formattingInfo.getCodeStyleSettings().SPACE_AFTER_COMMA && child1 != null && child1.getNode().getElementType() == CsvTypes.COMMA)
                || (formattingInfo.getCodeStyleSettings().SPACE_BEFORE_COMMA && child2 != null && child2.getNode().getElementType() == CsvTypes.COMMA)) {
            return 1;
        }
        return 0;
    }

    @Override
    public Indent getIndent() {
        if (formattingInfo.getCsvCodeStyleSettings().TABULARIZE && formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES && getNode().getElementType() == CsvTypes.RECORD) {
            CsvColumnInfo columnInfo = formattingInfo.getColumnInfo(0);
            Block fieldBlock = getSubBlocks().get(0);
            return Indent.getSpaceIndent(columnInfo.getMaxLength() - fieldBlock.getTextRange().getLength());
        }
        return null;
    }

    @Override
    public boolean isLeaf() {
        return getNode().getFirstChildNode() == null;
    }
}
