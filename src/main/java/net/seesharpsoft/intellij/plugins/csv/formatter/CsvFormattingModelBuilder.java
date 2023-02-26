package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.UnhandledSwitchCaseException;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvElementType;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvFormattingModelBuilder implements FormattingModelBuilder {

    private static ExternalFormattingModelBuilderImpl DUMMY_FORMATTING_MODEL_PROVIDER = new ExternalFormattingModelBuilderImpl(null);

    private static SpacingBuilder createSpaceBuilder(CodeStyleSettings settings) {
        CsvCodeStyleSettings csvCodeStyleSettings = settings.getCustomSettings(CsvCodeStyleSettings.class);
        SpacingBuilder builder = new SpacingBuilder(settings, CsvLanguage.INSTANCE);
        if (csvCodeStyleSettings.TRIM_LEADING_WHITE_SPACES) {
            builder
                    .after(CsvTypes.COMMA).spaceIf(csvCodeStyleSettings.SPACE_AFTER_SEPARATOR)
                    .after(CsvTypes.CRLF).spaces(0)
                    .after(CsvElementType.DOCUMENT_START).spaces(0);
        } else if (csvCodeStyleSettings.SPACE_AFTER_SEPARATOR) {
            builder.after(CsvTypes.COMMA).spaces(1);
        }

        if (csvCodeStyleSettings.TRIM_TRAILING_WHITE_SPACES) {
            builder
                    .before(CsvTypes.COMMA).spaceIf(csvCodeStyleSettings.SPACE_BEFORE_SEPARATOR)
                    .before(CsvTypes.CRLF).spaces(0);
        } else if (csvCodeStyleSettings.SPACE_BEFORE_SEPARATOR) {
            builder.before(CsvTypes.COMMA).spaces(1);
        }

        return builder;
    }

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
                        createSpaceBuilder(settings)
                );

                return FormattingModelProvider.createFormattingModelForPsiFile(
                        element.getContainingFile(),
                        new SimpleCsvBlock(root, formattingInfo),
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