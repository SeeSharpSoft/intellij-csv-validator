package net.seesharpsoft.idea.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import net.seesharpsoft.idea.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CsvBlockField extends CsvBlock {
    protected CsvBlockField(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment,
                            SpacingBuilder spacingBuilder, CsvCodeStyleSettings settings) {
        super(node, wrap, alignment, spacingBuilder, settings);
    }

    @Override
    protected List<Block> buildChildren() {
        applyCodeStyleSettings(myNode);

        List<Block> blocks = new ArrayList<>();
        ASTNode child = myNode.getFirstChildNode();
        while (child != null) {
            Block block = new CsvBlockText(child, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment(),
                    spacingBuilder, settings);
            blocks.add(block);
            child = child.getTreeNext();
        }

        return blocks;
    }

    protected void applyCodeStyleSettings(ASTNode currentNode) {
        if (settings.TRIM_LEADING_WHITE_SPACES) {
            ASTNode node = currentNode.getFirstChildNode();
            if (node.getElementType() == CsvTypes.QUOTE) {
                node = node.getTreeNext();
            }
            if (node.getElementType() != CsvTypes.QUOTE) {
                String text = node.getText();
                text = text.replaceAll("\\s*", "");
                ASTNode newNode = ASTFactory.leaf(CsvTypes.TEXT, text);
                currentNode.replaceChild(node, newNode);
            }
        }

        if (settings.TRIM_TRAILING_WHITE_SPACES) {
            ASTNode node = currentNode.getLastChildNode();
            if (node.getElementType() == CsvTypes.QUOTE) {
                node = node.getTreePrev();
            }
            if (node.getElementType() != CsvTypes.QUOTE) {
                String text = node.getText();
                text = text.replaceAll("\\s*$", "");
                ASTNode newNode = ASTFactory.leaf(CsvTypes.TEXT, text);
                currentNode.replaceChild(node, newNode);
            }
        }
    }

    public boolean isEscaped() {
        return myNode.getFirstChildNode().getElementType() == CsvTypes.QUOTE;
    }

    protected void padText(int padSize) {
        padSize -= myNode.getTextLength();
        if (padSize > 0) {
            ASTNode node = settings.LEADING_WHITE_SPACES ? myNode.getFirstChildNode() : myNode.getLastChildNode();
            if (node.getElementType() == CsvTypes.QUOTE) {
                node = settings.LEADING_WHITE_SPACES ? node.getTreeNext() : node.getTreePrev();
            }
            if (node.getElementType() != CsvTypes.QUOTE) {
                String text = node.getText();
                text = String.format("%0$" + (settings.LEADING_WHITE_SPACES ? "" : "-") + (text.length() + padSize) + "s", text);
                ASTNode newNode = ASTFactory.leaf(CsvTypes.TEXT, text);
                myNode.replaceChild(node, newNode);
            }
        }
    }

}
