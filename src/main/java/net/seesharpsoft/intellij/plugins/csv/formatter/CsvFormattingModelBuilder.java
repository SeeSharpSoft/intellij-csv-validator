package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.FormattingDocumentModelImpl;
import net.seesharpsoft.UnhandledSwitchCaseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvFormattingModelBuilder implements FormattingModelBuilderEx {

    // just for legacy reasons
    private static class FormattingContext {
        FormattingMode myMode;

        PsiElement myPsiElement;

        CodeStyleSettings myCodeStyleSettings;

        public FormattingContext(@NotNull PsiElement element, @NotNull CodeStyleSettings settings, @NotNull FormattingMode mode) {
            myPsiElement = element;
            myCodeStyleSettings = settings;
            myMode = mode;
        }

        public FormattingMode getFormattingMode() {
            return myMode;
        }

        public PsiElement getPsiElement() {
            return myPsiElement;
        }

        public CodeStyleSettings getCodeStyleSettings() {
            return myCodeStyleSettings;
        }
    }

    static class DummyFormattingModel implements FormattingModel {
        private final FormattingDocumentModel myModel;
        private final Block myBlock;

        public DummyFormattingModel(FormattingDocumentModel model, Block block) {
            myModel = model;
            myBlock = block;
        }

        @NotNull
        @Override
        public Block getRootBlock() {
            return myBlock;
        }

        @NotNull
        @Override
        public FormattingDocumentModel getDocumentModel() {
            return myModel;
        }

        @Override
        public TextRange replaceWhiteSpace(TextRange textRange, String whiteSpace) {
            return TextRange.EMPTY_RANGE;
        }

        @Override
        public TextRange shiftIndentInsideRange(ASTNode node, TextRange range, int indent) {
            return TextRange.EMPTY_RANGE;
        }

        @Override
        public void commitChanges() { }
    }

    @NotNull
    public FormattingModel createModel(FormattingContext formattingContext) {
        PsiElement element = formattingContext.getPsiElement();
        switch (formattingContext.getFormattingMode()) {
            case ADJUST_INDENT:
            case ADJUST_INDENT_ON_ENTER:
                // do not care about indent during editing (baaad performance)
                return new DummyFormattingModel(FormattingDocumentModelImpl.createOn(element.getContainingFile()), new CsvDummyBlock(element.getNode(), null));
            case REFORMAT:
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

    @Override
    public @NotNull FormattingModel createModel(@NotNull PsiElement element, @NotNull CodeStyleSettings settings, @NotNull FormattingMode mode) {
        return createModel(new FormattingContext(element,settings, mode));
    }

    @Override
    public @NotNull FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
        return createModel(element, settings, FormattingMode.ADJUST_INDENT);
    }

    @Nullable
    @Override
    public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
        return null;
    }

    @Override
    public CommonCodeStyleSettings.@Nullable IndentOptions getIndentOptionsToUse(@NotNull PsiFile file, @NotNull FormatTextRanges ranges, @NotNull CodeStyleSettings settings) {
        return null;
    }
}