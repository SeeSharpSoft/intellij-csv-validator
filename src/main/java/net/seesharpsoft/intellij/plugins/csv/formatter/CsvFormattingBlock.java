package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.formatter.common.AbstractBlock;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CsvFormattingBlock extends AbstractBlock {
    protected CsvFormattingInfo myFormattingInfo;

    protected CsvFormattingBlock(@NotNull ASTNode node, CsvFormattingInfo formattingInfo) {
        this(node, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment(), formattingInfo);
    }

    protected CsvFormattingBlock(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment,
                                 CsvFormattingInfo formattingInfo) {
        super(node, wrap, alignment);
        this.myFormattingInfo = formattingInfo;
    }

    private boolean stripLeading() {
        CsvCodeStyleSettings settings = myFormattingInfo.getCsvCodeStyleSettings();
        ASTNode prev = myNode.getTreePrev();
        return settings.TRIM_LEADING_WHITE_SPACES || settings.TABULARIZE
                || (settings.SPACE_AFTER_SEPARATOR && prev != null && !CsvFormatHelper.isCRLFNode(prev));
    }

    private boolean stripTrailing() {
        CsvCodeStyleSettings settings = myFormattingInfo.getCsvCodeStyleSettings();
        ASTNode next = myNode.getTreeNext();
        return settings.TRIM_TRAILING_WHITE_SPACES || settings.TABULARIZE
                || (settings.SPACE_BEFORE_SEPARATOR && next != null && !CsvFormatHelper.isCRLFNode(next));
    }

    private boolean requiresTabularization(ASTNode node1, ASTNode node2) {
        CsvCodeStyleSettings settings = myFormattingInfo.getCsvCodeStyleSettings();
        if (!settings.TABULARIZE) return false;
        if (settings.LEADING_WHITE_SPACES) {
            return CsvFormatHelper.isFieldNode(node2)
                    || (CsvFormatHelper.isQuoteNode(node1) && !CsvFormatHelper.isSeparatorNode(node2))
                    || (CsvFormatHelper.isRecordNode(node2) && (settings.WHITE_SPACES_OUTSIDE_QUOTES || !CsvFormatHelper.isQuotedField(node2.getFirstChildNode())));
        }
        return (!CsvFormatHelper.isQuoteNode(node1) && CsvFormatHelper.isSeparatorNode(node2))
                || (!CsvFormatHelper.isSeparatorNode(node1) && CsvFormatHelper.isQuoteNode(node2));
    }

    private int getDefaultSpacing(@Nullable ASTNode node1, @NotNull ASTNode node2) {
        CsvCodeStyleSettings settings = myFormattingInfo.getCsvCodeStyleSettings();
        return (CsvFormatHelper.isSeparatorNode(node1) && settings.SPACE_AFTER_SEPARATOR ||
                CsvFormatHelper.isSeparatorNode(node2) && settings.SPACE_BEFORE_SEPARATOR) ? 1 : 0;
    }

    protected int getRequiredTabularizationSpacing(PsiElement psiElement) {
        CsvColumnInfoMap<PsiElement> columnInfoMap = myFormattingInfo.getCsvColumnInfoMap();
        CsvColumnInfo<PsiElement> columnInfo = columnInfoMap.getColumnInfo(psiElement);
        if (columnInfo == null) {
            return -1;
        }

        int targetLength = columnInfo.getMaxLength();
        int currentLength = CsvFormatHelper.getTextLength(psiElement, myFormattingInfo.getCsvCodeStyleSettings());

        if (currentLength == 0 && psiElement.getNextSibling() == null) {
            return 0;
        }

        return targetLength - currentLength;
    }

    private List<Block> buildQuotedFieldChildren(ASTNode node) {
        List<Block> blocks = new ArrayList<>();
        ASTNode child = node.getFirstChildNode();
        while (child != null) {
            Block block = new CsvFieldFormattingBlock(child, this.getWrap(), this.getAlignment(), myFormattingInfo);
            blocks.add(block);
            child = child.getTreeNext();
        }
        return blocks;
    }

    private List<Block> getBlocksForNode(ASTNode node) {
        CsvCodeStyleSettings settings = myFormattingInfo.getCsvCodeStyleSettings();
        if (settings.TABULARIZE
                && !settings.WHITE_SPACES_OUTSIDE_QUOTES
                && CsvFormatHelper.isQuotedField(node)
        ) {
            return this.buildQuotedFieldChildren(node);
        }
        return Collections.singletonList(new CsvFormattingBlock(node, this.getWrap(), this.getAlignment(), myFormattingInfo));
    }

    @Override
    protected List<Block> buildChildren() {
        List<Block> blocks = new ArrayList<>();
        if (CsvFormatHelper.isFieldNode(this.myNode)) {
            return blocks;
        }

        ASTNode child = myNode.getFirstChildNode();
        while (child != null) {
            blocks.addAll(getBlocksForNode(child));
            child = child.getTreeNext();
        }
        return blocks;
    }

    @Override
    public @NotNull TextRange getTextRange() {
        TextRange textRange = super.getTextRange();
        if (!CsvFormatHelper.isFieldNode(this.myNode)) return textRange;

        String originalText = myNode.getText();
        String trimmedText = stripLeading() ? originalText.stripLeading() : originalText;
        trimmedText = stripTrailing() ? trimmedText.stripTrailing() : trimmedText;
        return TextRange.from(textRange.getStartOffset() + originalText.indexOf(trimmedText), trimmedText.length());
    }

    @Override
    public @Nullable Indent getIndent() {
        if (!this.requiresTabularization(null, this.myNode)) {
            return null;
        }
        return Indent.getSpaceIndent(this.getRequiredTabularizationSpacing(this.myNode.getPsi()));
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        ASTNode childNode1 = child1 == null ? null : ((CsvFormattingBlock)child1).myNode;
        ASTNode childNode2 = ((CsvFormattingBlock)child2).myNode;
        int spacing = this.getDefaultSpacing(childNode1, childNode2);
        if (this.requiresTabularization(childNode1, childNode2)) {
            PsiElement targetPsiElement = CsvFormatHelper.isFieldNode(childNode1) ? childNode1.getPsi() : (
                    CsvFormatHelper.isRecordNode(childNode2) ? childNode2.getFirstChildNode().getPsi() : (
                            CsvFormatHelper.isFieldNode(childNode2) ? childNode2.getPsi() : childNode2.getTreeParent().getPsi()
                    )
            );
            spacing += this.getRequiredTabularizationSpacing(targetPsiElement);
        }
        return Spacing.createSpacing(spacing, spacing, 0, true, 0);
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    private static class CsvFieldFormattingBlock extends CsvFormattingBlock {
        protected CsvFieldFormattingBlock(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment, CsvFormattingInfo formattingInfo) {
            super(node, wrap, alignment, formattingInfo);
        }

        @Override
        public @NotNull TextRange getTextRange() {
            TextRange textRange = super.getTextRange();
            String originalText = myNode.getText();
            String trimmedText = originalText;
            boolean isQuote = CsvFormatHelper.isQuoteNode(this.myNode);
            if (isQuote || CsvFormatHelper.isQuoteNode(myNode.getTreePrev())) {
                trimmedText = trimmedText.stripLeading();
            }
            if (isQuote || CsvFormatHelper.isQuoteNode(myNode.getTreeNext())) {
                trimmedText = trimmedText.stripTrailing();
            }
            return TextRange.from(textRange.getStartOffset() + originalText.indexOf(trimmedText), trimmedText.length());
        }
    }
}
