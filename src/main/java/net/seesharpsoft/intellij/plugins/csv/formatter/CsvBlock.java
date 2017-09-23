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

    private CsvBlock emptySibling;

    protected CsvBlock(@NotNull ASTNode node, CsvFormattingInfo formattingInfo) {
        super(node, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment());
        this.formattingInfo = formattingInfo;
    }

    protected void setEmptySibling(CsvBlock block) {
        this.emptySibling = block;
    }

    protected CsvBlock getEmptySibling() {
        return emptySibling;
    }

    protected boolean hasEmptySibling() {
        return emptySibling != null;
    }
    
    @Override
    protected List<Block> buildChildren() {
        List<Block> blocks = new ArrayList<>();
        ASTNode node = getNode().getFirstChildNode();
        while (node != null) {
            if (node.getElementType() != TokenType.WHITE_SPACE) {
                CsvBlock block = new CsvBlock(node, formattingInfo);
                // in older version of idea, blocks with length 0 lead to an assertion -> workaround this 
                if (node.getTextLength() > 0) {
                    blocks.add(block);
                } else if (blocks.size() > 0) {
                    ((CsvBlock) blocks.get(blocks.size() - 1)).setEmptySibling(block);
                }
            }
            node = node.getTreeNext();
        }
        return blocks;
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        Spacing spacing = null;
        CsvBlock block1 = child1 == null ? null : (CsvBlock) child1;
        CsvBlock block2 = child2 == null ? null : (CsvBlock) child2;
        if (formattingInfo.getCsvCodeStyleSettings().TABULARIZE) {
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
        } else if (getNode().getTreeParent() == null) {
            spacing = getSpacingForRecords(child1, child2);
        }
        return spacing;
    }

    private Spacing getTabularizeInsideQuoteSpacing(@Nullable CsvBlock child1, @NotNull CsvBlock child2) {
        Spacing spacing = null;
        if (getNode().getElementType() == CsvTypes.RECORD && !CsvFormatHelper.isQuotedField(child1) && !CsvFormatHelper.isQuotedField(child2)) {
            spacing = getSpacingForFields(child1, child2);
        } else if (getNode().getTreeParent() == null &&
                !CsvFormatHelper.isFirstFieldOfRecordQuoted(child1) && !CsvFormatHelper.isFirstFieldOfRecordQuoted(child2)) {
            spacing = getSpacingForRecords(child1, child2);
        } else if (CsvFormatHelper.isQuotedField(this)) {
            if (child1 != null && child1.getNode().getElementType() == CsvTypes.QUOTE && child2 != null && child2.getNode().getElementType() == CsvTypes.QUOTE) {
                spacing = getSpacingOfFieldNode();
            } else if (child1 != null && child1.getNode().getElementType() == CsvTypes.QUOTE && child2 != null) {
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
        int spaces = 0;
        if (child2 != null && child2.getNode().getElementType() == CsvTypes.RECORD) {
            CsvBlock fieldBlock = null;
            CsvColumnInfo columnInfo = null;
            columnInfo = formattingInfo.getColumnInfo(0);
            List<Block> subBlocks = child2.getSubBlocks();
            fieldBlock = (CsvBlock)subBlocks.get(0);
            if (fieldBlock.getNode().getElementType() != CsvTypes.FIELD) {
                spaces = (formattingInfo.getCsvCodeStyleSettings().TABULARIZE ? columnInfo.getMaxLength() : 0)
                        + (formattingInfo.getCsvCodeStyleSettings().SPACE_BEFORE_SEPARATOR ? 1 : 0);
            } else if(!formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES || !formattingInfo.getCsvCodeStyleSettings().TABULARIZE) {
                return null;
            } else {
                spaces = columnInfo.getMaxLength() - fieldBlock.getTextRange().getLength();
            }
        }
        spacing = Spacing.createSpacing(spaces, spaces, 0, true, formattingInfo.getCodeStyleSettings().KEEP_BLANK_LINES_IN_CODE);
        return spacing;
    }

    private Spacing getSpacingForFields(@Nullable CsvBlock child1, @NotNull CsvBlock child2) {
        Spacing spacing;
        int spaces = getColumnSpacing(child1, child2) + getAdditionalSpaces(child1, child2);
        spacing = Spacing.createSpacing(spaces, spaces, 0, true, formattingInfo.getCodeStyleSettings().KEEP_BLANK_LINES_IN_CODE);
        return spacing;
    }

    private int getColumnSpacing(@Nullable CsvBlock child1, @NotNull CsvBlock child2) {
        ASTNode node = null;
        CsvColumnInfo columnInfo = null;
        if (child1 == null && child2 != null && child2.getNode().getElementType() == CsvTypes.COMMA) {
            columnInfo = formattingInfo.getColumnInfo(0);
        } else if (child1 != null && child1.getNode().getElementType() == CsvTypes.COMMA && child2 != null && child2.getNode().getElementType() == CsvTypes.COMMA && child1.hasEmptySibling()) {
            columnInfo = formattingInfo.getColumnInfo(child1.getEmptySibling().getNode());
        } else if (formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES && child2 != null && (node = child2.getNode()).getElementType() == CsvTypes.FIELD) {
            columnInfo = formattingInfo.getColumnInfo(node);
        } else if (!formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES && child1 != null && (node = child1.getNode()).getElementType() == CsvTypes.FIELD) {
            columnInfo = formattingInfo.getColumnInfo(node);
        }

        return columnInfo == null ? 0 : (columnInfo.getMaxLength() - (node == null ? 0 : node.getTextLength()));
    }

    private int getAdditionalSpaces(@Nullable CsvBlock child1, @NotNull CsvBlock child2) {
        if ((formattingInfo.getCsvCodeStyleSettings().SPACE_AFTER_SEPARATOR && child1 != null && child1.getNode().getElementType() == CsvTypes.COMMA)
                || (formattingInfo.getCsvCodeStyleSettings().SPACE_BEFORE_SEPARATOR && child2 != null && child2.getNode().getElementType() == CsvTypes.COMMA)) {
            return 1;
        }
        return 0;
    }

    @Override
    public Indent getIndent() {
        if (formattingInfo.getCsvCodeStyleSettings().TABULARIZE
                && formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES
                && getNode().getElementType() == CsvTypes.RECORD
                && (formattingInfo.getCsvCodeStyleSettings().WHITE_SPACES_OUTSIDE_QUOTES || !CsvFormatHelper.isFirstFieldOfRecordQuoted(this))) {
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
