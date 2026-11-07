package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

        CsvField field = psiElement == null ? null : CsvHelper.getParentFieldElement(psiElement);
        return field != null && CsvPsiTreeUpdater.canUnquote(field);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        CsvPsiTreeUpdater.edit(element.getContainingFile(),
                updater -> updater.setQuoted(List.of(CsvHelper.getParentFieldElement(element)), false));
    }

}