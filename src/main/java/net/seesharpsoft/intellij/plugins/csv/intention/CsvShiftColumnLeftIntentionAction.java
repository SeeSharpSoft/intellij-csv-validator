package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import org.jetbrains.annotations.NotNull;

public class CsvShiftColumnLeftIntentionAction extends CsvShiftColumnIntentionAction {

    public CsvShiftColumnLeftIntentionAction() {
        super("Shift Column Left");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull final PsiElement psiElement) throws IncorrectOperationException {
        PsiFile containingFile = psiElement.getContainingFile();
        if (!(containingFile instanceof CsvFile)) {
            return;
        }
        CsvFile csvFile = (CsvFile) containingFile;
        if (!csvFile.isValid()) {
            return;
        }

        PsiElement element = CsvHelper.getParentFieldElement(psiElement);

        changeColumnOrder(project, csvFile, element, true);
    }
}
