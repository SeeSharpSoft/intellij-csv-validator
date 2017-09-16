package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CsvFormattingModelBuilder implements FormattingModelBuilder {
    @NotNull
    @Override
    public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
        ASTNode root = getRoot(element.getNode());
        CsvCodeStyleSettings customSettings = settings.getCustomSettings(CsvCodeStyleSettings.class);
        CsvFormattingInfo formattingInfo = new CsvFormattingInfo(customSettings, createSpaceBuilder(customSettings), createColumnInfoMap(root, customSettings));

        return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(),
                new CsvBlock(
                        root,
                        Alignment.createAlignment(),
                        formattingInfo),
                settings);
    }

    private static ASTNode getRoot(ASTNode node) {
        ASTNode parent;
        while ((parent = node.getTreeParent()) != null) {
            node = parent;
        }
        return node;
    }

    private static SpacingBuilder createSpaceBuilder(CsvCodeStyleSettings settings) {
        SpacingBuilder builder = new SpacingBuilder(settings.getContainer(), CsvLanguage.INSTANCE);
        if (settings.TRIM_LEADING_WHITE_SPACES || settings.TABULARIZE) {
            builder
                    .after(CsvTypes.COMMA).spaces(0)
                    .before(CsvTypes.RECORD).spaces(0)
                    .before(CsvTypes.FIELD).spaces(0);
        }
        if (settings.TRIM_TRAILING_WHITE_SPACES || settings.TABULARIZE) {
            builder
                    .before(CsvTypes.COMMA).spaces(0)
                    .after(CsvTypes.RECORD).spaces(0)
                    .after(CsvTypes.FIELD).spaces(0);
        }
        return builder;
    }

    private Map<Integer, CsvFormattingInfo.ColumnInfo> createColumnInfoMap(ASTNode root, CsvCodeStyleSettings settings) {
        Map<Integer, CsvFormattingInfo.ColumnInfo> columnInfoMap = new HashMap<>();
        ASTNode child = root.getFirstChildNode();
        while (child != null) {
            if (child.getElementType() == CsvTypes.RECORD) {
                Integer column = 0;
                ASTNode subChild = child.getFirstChildNode();
                while (subChild != null) {
                    if (subChild.getElementType() == CsvTypes.FIELD) {
                        Integer length = subChild.getTextLength();
                        if (!columnInfoMap.containsKey(column)) {
                            columnInfoMap.put(column,
                                    new CsvFormattingInfo.ColumnInfo(length)
                            );
                        } else if (columnInfoMap.get(column).getMaxLength() < length) {
                            columnInfoMap.get(column).setMaxLength(length);
                        }
                        columnInfoMap.get(column).addNode(subChild);
                        ++column;
                    }
                    subChild = subChild.getTreeNext();
                }
            }
            child = child.getTreeNext();
        }
        return columnInfoMap;
    }

    @Nullable
    @Override
    public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
        return null;
    }
}