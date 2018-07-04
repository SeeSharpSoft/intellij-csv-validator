package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NonNls
public class CsvUnquoteValueIntentionAction extends CsvIntentionAction {

    public CsvUnquoteValueIntentionAction() {
        super("Unquote");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable final PsiElement psiElement) {
        if (!super.isAvailable(project, editor, psiElement)) {
            return false;
        }

        PsiElement element = psiElement == null ? null : CsvHelper.getParentFieldElement(psiElement);
        return element instanceof CsvField &&
                element.getFirstChild() != null &&
                (CsvHelper.getElementType(element.getFirstChild()) == CsvTypes.QUOTE ||
                        CsvHelper.getElementType(element.getLastChild()) == CsvTypes.QUOTE) &&
                CsvIntentionHelper.getChildren(element).stream().allMatch(childElement -> CsvHelper.getElementType(childElement) != CsvTypes.ESCAPED_TEXT);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        CsvIntentionHelper.unquoteValue(project, CsvHelper.getParentFieldElement(element));
    }

}