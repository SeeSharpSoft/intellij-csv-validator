package net.seesharpsoft.idea.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import net.seesharpsoft.idea.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CsvBlockLine extends CsvBlock {
    protected CsvBlockLine(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment,
                           SpacingBuilder spacingBuilder, CsvCodeStyleSettings settings) {
        super(node, wrap, alignment, spacingBuilder, settings);
    }

    @Override
    protected List<Block> buildChildren() {
        List<Block> blocks = new ArrayList<Block>();
        ASTNode child = myNode.getFirstChildNode();
        while (child != null) {
            if (child.getElementType() == CsvTypes.FIELD) {
                Block block = new CsvBlockField(child, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment(),
                        spacingBuilder, settings);
                blocks.add(block);
            } else {
                Block block = new CsvBlock(child, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment(),
                        spacingBuilder, settings);
                blocks.add(block);
            }
            child = child.getTreeNext();
        }
        return blocks;
    }
}
