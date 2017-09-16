package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
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
        CsvFormattingInfo formattingInfo = new CsvFormattingInfo(
                settings,
                createSpaceBuilder(settings),
                createColumnInfoMap(root)
        );

        return FormattingModelProvider.createFormattingModelForPsiFile(
                element.getContainingFile(),
                new CsvBlock(root, formattingInfo),
                settings
        );
    }

    private static ASTNode getRoot(ASTNode node) {
        ASTNode parent;
        while ((parent = node.getTreeParent()) != null) {
            node = parent;
        }
        return node;
    }

    private static SpacingBuilder createSpaceBuilder(CodeStyleSettings settings) {
        CsvCodeStyleSettings csvCodeStyleSettings = settings.getCustomSettings(CsvCodeStyleSettings.class);
        SpacingBuilder builder = new SpacingBuilder(settings, CsvLanguage.INSTANCE);
        if (csvCodeStyleSettings.TRIM_LEADING_WHITE_SPACES || csvCodeStyleSettings.TABULARIZE) {
            builder
                    .after(CsvTypes.COMMA).spaceIf(settings.SPACE_AFTER_COMMA)
                    .before(CsvTypes.RECORD).spaces(0)
                    .before(CsvTypes.FIELD).spaces(0);
        } else if (settings.SPACE_AFTER_COMMA) {
            builder.after(CsvTypes.COMMA).spaces(1);
        }

        if (csvCodeStyleSettings.TRIM_TRAILING_WHITE_SPACES || csvCodeStyleSettings.TABULARIZE) {
            builder
                    .before(CsvTypes.COMMA).spaceIf(settings.SPACE_BEFORE_COMMA)
                    .after(CsvTypes.RECORD).spaces(0)
                    .after(CsvTypes.FIELD).spaces(0);
        } else if (settings.SPACE_BEFORE_COMMA) {
            builder.before(CsvTypes.COMMA).spaces(1);
        }

        return builder;
    }

    private Map<Integer, CsvColumnInfo<ASTNode>> createColumnInfoMap(ASTNode root) {
        Map<Integer, CsvColumnInfo<ASTNode>> columnInfoMap = new HashMap<>();
        ASTNode child = root.getFirstChildNode();
        while (child != null) {
            if (child.getElementType() == CsvTypes.RECORD) {
                Integer column = 0;
                ASTNode subChild = child.getFirstChildNode();
                while (subChild != null) {
                    if (subChild.getElementType() == CsvTypes.FIELD) {
                        Integer length = subChild.getTextLength();
                        if (!columnInfoMap.containsKey(column)) {
                            columnInfoMap.put(column, new CsvColumnInfo(column, length));
                        } else if (columnInfoMap.get(column).getMaxLength() < length) {
                            columnInfoMap.get(column).setMaxLength(length);
                        }
                        columnInfoMap.get(column).addElement(subChild);
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