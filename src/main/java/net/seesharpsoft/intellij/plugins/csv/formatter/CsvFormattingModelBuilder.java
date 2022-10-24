package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.UnhandledSwitchCaseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvFormattingModelBuilder implements FormattingModelBuilder {

    @Override
    @NotNull
    public FormattingModel createModel(FormattingContext formattingContext) {
        switch (formattingContext.getFormattingMode()) {
            case ADJUST_INDENT:
            case ADJUST_INDENT_ON_ENTER:
                // do not care about indent during editing (baaad performance)
                return FormatterImpl.getInstance().createDummyFormattingModel(formattingContext.getPsiElement());
            case REFORMAT:
                PsiElement element = formattingContext.getPsiElement();
                CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
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
            default:
                throw new UnhandledSwitchCaseException(formattingContext.getFormattingMode());
        }
    }

    @Nullable
    @Override
    public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
        return null;
    }
}