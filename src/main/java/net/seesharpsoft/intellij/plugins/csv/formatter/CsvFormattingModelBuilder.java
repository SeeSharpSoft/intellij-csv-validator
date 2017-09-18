package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvFormattingModelBuilder implements FormattingModelBuilder {
    @NotNull
    @Override
    public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
        ASTNode root = CsvFormatHelper.getRoot(element.getNode());
        CsvFormattingInfo formattingInfo = new CsvFormattingInfo(
                settings,
                CsvFormatHelper.createSpaceBuilder(settings),
                CsvFormatHelper.createColumnInfoMap(root, settings)
        );

        return FormattingModelProvider.createFormattingModelForPsiFile(
                element.getContainingFile(),
                new CsvBlock(root, formattingInfo),
                settings
        );
    }

    @Nullable
    @Override
    public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
        return null;
    }
}