package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CsvDummyBlock extends CsvBlock {

    protected CsvDummyBlock(@NotNull ASTNode node, CsvFormattingInfo formattingInfo) {
        super(node, formattingInfo);
    }

    @Override
    protected List<Block> buildChildren() {
        return Collections.EMPTY_LIST;
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block block, @NotNull Block block1) {
        return null;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }
}
