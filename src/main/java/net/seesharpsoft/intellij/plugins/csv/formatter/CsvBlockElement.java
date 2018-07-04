package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CsvBlockElement extends CsvBlock {

    private CsvBlockField myField;

    public CsvBlockElement(ASTNode node, CsvFormattingInfo formattingInfo) {
        this(node, formattingInfo, null);
    }

    public CsvBlockElement(ASTNode node, CsvFormattingInfo formattingInfo, CsvBlockField field) {
        super(node, formattingInfo);
        setField(field);
    }

    public CsvColumnInfo getColumnInfo() {
        return getField() == null ? null : getField().getColumnInfo();
    }

    public CsvBlockField getField() {
        return myField;
    }

    public void setField(CsvBlockField field) {
        this.myField = field;
    }

    @Override
    protected List<Block> buildChildren() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block block, @NotNull Block block1) {
        return null;
    }
}
