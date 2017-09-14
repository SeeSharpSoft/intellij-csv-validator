package net.seesharpsoft.idea.plugins.csv.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.idea.plugins.csv.CsvLanguage;
import net.seesharpsoft.idea.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvFormattingModelBuilder implements FormattingModelBuilder {
    @NotNull
    @Override
    public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {

        return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(),
                new CsvBlockRoot(
                        getRoot(element.getNode()),
                        Wrap.createWrap(WrapType.NONE, false),
                        Alignment.createAlignment(),
                        createSpaceBuilder(settings),
                        settings.getCustomSettings(CsvCodeStyleSettings.class)),
                settings);
    }

    private static ASTNode getRoot(ASTNode node) {
        ASTNode parent;
        while ((parent = node.getTreeParent()) != null) {
            node = parent;
        }
        return node;
    }

    private static SpacingBuilder createSpaceBuilder(CodeStyleSettings settings) {
        return new SpacingBuilder(settings, CsvLanguage.INSTANCE)
                .around(CsvTypes.COMMA)
                .spaceIf(settings.SPACE_AROUND_ASSIGNMENT_OPERATORS)
                .before(CsvTypes.TEXT)
                .none();
    }

    @Nullable
    @Override
    public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
        return null;
    }
}