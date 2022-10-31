package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.common.AbstractBlock;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SimpleCsvBlock extends AbstractBlock {

    protected CsvFormattingInfo myFormattingInfo;

    protected SimpleCsvBlock(@NotNull ASTNode node,  CsvFormattingInfo formattingInfo) {
        this(node, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment(), formattingInfo);
    }

    protected SimpleCsvBlock(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment,
                             CsvFormattingInfo formattingInfo) {
        super(node, wrap, alignment);
        this.myFormattingInfo = formattingInfo;
    }

    @Override
    protected List<Block> buildChildren() {
        List<Block> blocks = new ArrayList<>();
        if (myNode.getElementType() == CsvTypes.FIELD) return blocks;

        ASTNode child = myNode.getFirstChildNode();
        while (child != null) {
            if (child.getElementType() != TokenType.WHITE_SPACE) {
                Block block = new SimpleCsvBlock(child, myFormattingInfo);
                blocks.add(block);
            }
            child = child.getTreeNext();
        }
        return blocks;
    }

    private boolean stripLeading() {
        CsvCodeStyleSettings settings = myFormattingInfo.getCsvCodeStyleSettings();
        return settings.TRIM_LEADING_WHITE_SPACES
                || settings.SPACE_AFTER_SEPARATOR;
    }

    private boolean stripTrailing() {
        CsvCodeStyleSettings settings = myFormattingInfo.getCsvCodeStyleSettings();
        return settings.TRIM_TRAILING_WHITE_SPACES
                || settings.SPACE_BEFORE_SEPARATOR;
    }

    @Override
    public TextRange getTextRange() {
        TextRange textRange = super.getTextRange();
        String originalText = myNode.getText();
        String trimmedText = stripLeading() ? originalText.stripLeading() : originalText;
        trimmedText = stripTrailing() ? trimmedText.stripTrailing() : trimmedText;
        return TextRange.from(textRange.getStartOffset() + originalText.indexOf(trimmedText), trimmedText.length());
    }

    @Override
    public Indent getIndent() {
        return Indent.getNoneIndent();
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return myFormattingInfo.getSpacingBuilder().getSpacing(this, child1, child2);
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

}
