package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NonNls
public class CsvUnquoteAllIntentionAction extends CsvIntentionAction {

    public CsvUnquoteAllIntentionAction() {
        super("Unquote All");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable PsiElement element) {
        if (!super.isAvailable(project, editor, element)) {
            return false;
        }

        return !CsvIntentionHelper.getAllElements(element.getContainingFile()).stream()
                .anyMatch(psiElement -> PsiHelper.getElementType(psiElement) == TokenType.ERROR_ELEMENT);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiElement element) throws IncorrectOperationException {
        CsvIntentionHelper.unquoteAll(project, element.getContainingFile());
    }

}