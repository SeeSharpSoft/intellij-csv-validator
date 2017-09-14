package net.seesharpsoft.idea.plugins.csv.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvBlockText extends CsvBlock {
    protected CsvBlockText(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment,
                           SpacingBuilder spacingBuilder, CsvCodeStyleSettings settings) {
        super(node, wrap, alignment, spacingBuilder, settings);
    }


}
