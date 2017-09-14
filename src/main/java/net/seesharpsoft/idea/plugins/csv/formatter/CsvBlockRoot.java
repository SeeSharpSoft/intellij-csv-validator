package net.seesharpsoft.idea.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import net.seesharpsoft.idea.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvBlockRoot extends CsvBlock {
    protected CsvBlockRoot(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment,
                           SpacingBuilder spacingBuilder, CsvCodeStyleSettings settings) {
        super(node, wrap, alignment, spacingBuilder, settings);
    }

    @Override
    protected List<Block> buildChildren() {
        List<Block> blocks = buildChildrenInternal();

        if (settings.TABULARIZE) {
            tabularize(blocks, collectTableInfo());
        }

        return blocks;
    }

    private List<Block> buildChildrenInternal() {
        List<Block> blocks = new ArrayList<Block>();
        ASTNode child = myNode.getFirstChildNode();
        while (child != null) {
            if (child.getElementType() == CsvTypes.RECORD) {
                if (this.settings.REMOVE_EMPTY_LINES || child.getTextLength() > 0) {
                    Block block = new CsvBlockLine(child, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment(),
                            spacingBuilder, settings);
                    blocks.add(block);
                } else {
                    child = removeEmptyRecord(child);
                    continue;
                }
            } else {
                Block block = new CsvBlock(child, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment(),
                        spacingBuilder, settings);
                blocks.add(block);
            }
            child = child.getTreeNext();
        }
        return blocks;
    }

    private ASTNode removeEmptyRecord(ASTNode child) {
        ASTNode otherChild = child.getTreeNext();
        myNode.removeChild(child);
        // skip the next linebreak
        child = otherChild.getTreeNext();
        myNode.removeChild(otherChild);
        return child;
    }

    private Map<Integer, Integer> collectTableInfo() {
        ASTNode child = myNode.getFirstChildNode();
        Map<Integer, Integer> columnSizeMap = new HashMap();
        while (child != null) {
            if (child.getElementType() == CsvTypes.RECORD) {
                Integer column = 0;
                ASTNode subChild = child.getFirstChildNode();
                while (subChild != null) {
                    if (subChild.getElementType() == CsvTypes.FIELD) {
                        Integer length = subChild.getTextLength();
                        if (!columnSizeMap.containsKey(column) || columnSizeMap.get(column) < length) {
                            columnSizeMap.put(column, length);
                        }
                        ++column;
                    }
                }
            }
        }
        return columnSizeMap;
    }

    private void tabularize(List<Block> blocks, Map<Integer, Integer> columnSizeMap) {
        for (Block block : blocks) {
            if (block instanceof CsvBlockLine) {
                Integer column = 0;
                List<Block> subBlocks = block.getSubBlocks();
                for (int i = 0; i < subBlocks.size(); ++i) {
                    Block columnBlock = subBlocks.get(i);
                    if (columnBlock instanceof CsvBlockField) {
                        ((CsvBlockField) columnBlock).padText(columnSizeMap.get(column));
                        column++;
                    }
                }
            }
        }
    }
}
