package net.seesharpsoft.intellij.formatting;

import com.intellij.formatting.*;
import com.intellij.psi.codeStyle.ExternalFormatProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExternalFormattingModelBuilderImpl implements FormattingModelBuilder {
    private final @Nullable FormattingModelBuilder myDefaultBuilder;

    public ExternalFormattingModelBuilderImpl(@Nullable FormattingModelBuilder defaultBuilder) {
        myDefaultBuilder = defaultBuilder;
    }

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        if (formattingContext.getFormattingMode() == FormattingMode.REFORMAT &&
                ExternalFormatProcessor.useExternalFormatter(formattingContext.getContainingFile()) || myDefaultBuilder == null) {
            return new DummyFormattingModel(formattingContext.getPsiElement());
        }
        return myDefaultBuilder.createModel(formattingContext);
    }

}
