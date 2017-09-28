package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NonNls
public class CsvQuoteValueIntentionAction extends CsvIntentionAction {

    public CsvQuoteValueIntentionAction() {
        super("Quote");
    }
    
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable PsiElement element) {
        if (!super.isAvailable(project, editor, element)) {
            return false;
        }
        
        element = CsvIntentionHelper.getParentFieldElement(element);
        return element instanceof CsvField &&
                element.getFirstChild() != null &&
                (CsvIntentionHelper.getElementType(element.getFirstChild()) != CsvTypes.QUOTE ||
                        CsvIntentionHelper.getElementType(element.getLastChild()) != CsvTypes.QUOTE);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiElement element) throws IncorrectOperationException {
        CsvIntentionHelper.quoteValue(project, CsvIntentionHelper.getParentFieldElement(element));
    }
    
}