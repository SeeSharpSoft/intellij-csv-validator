package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.UnhandledSwitchCaseException;
import net.seesharpsoft.intellij.formatting.ExternalFormattingModelBuilderImpl;
import org.jetbrains.annotations.NotNull;

public class CsvFormattingModelBuilder implements FormattingModelBuilder {

    private static final ExternalFormattingModelBuilderImpl DUMMY_FORMATTING_MODEL_PROVIDER = new ExternalFormattingModelBuilderImpl(null);

    @Override
    @NotNull
    public FormattingModel createModel(FormattingContext formattingContext) {
        switch (formattingContext.getFormattingMode()) {
            case ADJUST_INDENT:
            case ADJUST_INDENT_ON_ENTER:
                // do not care about indent during editing (bad performance)
                // NOTE: Formatter.getInstance().createDummyFormattingModel(formattingContext.getPsiElement()); <-- marked as internal, but this DummyFormattingModel is all I want
                return DUMMY_FORMATTING_MODEL_PROVIDER.createModel(formattingContext);
            case REFORMAT:
                PsiElement element = formattingContext.getPsiElement();
                CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
                ASTNode root = element.getNode();
                CsvFormattingInfo formattingInfo = new CsvFormattingInfo(
                        settings,
                        CsvFormatHelper.createSpaceBuilder(settings),
                        element.getContainingFile()
                );

                return FormattingModelProvider.createFormattingModelForPsiFile(
                        element.getContainingFile(),
                        new CsvFormattingBlock(root, formattingInfo),
                        settings
                );
            default:
                throw new UnhandledSwitchCaseException(formattingContext.getFormattingMode());
        }
    }
}
