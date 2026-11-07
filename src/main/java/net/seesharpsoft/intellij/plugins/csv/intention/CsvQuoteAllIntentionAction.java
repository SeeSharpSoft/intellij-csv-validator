package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NonNls
public class CsvQuoteAllIntentionAction extends CsvIntentionAction {

    public CsvQuoteAllIntentionAction() {
        super("Quote All");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable PsiElement element) {
        if (!super.isAvailable(project, editor, element)) {
            return false;
        }

        return !PsiTreeUtil.hasErrorElements(element.getContainingFile());
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiElement element) throws IncorrectOperationException {
        CsvPsiTreeUpdater.edit(element.getContainingFile(), updater -> updater.setQuoted(true));
    }

}