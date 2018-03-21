package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CsvShiftColumnLeftIntentionAction extends CsvShiftColumnIntentionAction {

    public CsvShiftColumnLeftIntentionAction() {
        super("Shift Column Left");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        CsvFile psiFile = (CsvFile)psiElement.getContainingFile();

        psiElement = CsvIntentionHelper.getParentFieldElement(psiElement);

        Map<Integer, CsvColumnInfo<PsiElement>> columnInfoMap = CsvHelper.createColumnInfoMap(psiFile);

        CsvColumnInfo<PsiElement> rightColumnInfo = null;
        for (CsvColumnInfo columnInfo : columnInfoMap.values()) {
            if (columnInfo.containsElement(psiElement)) {
                rightColumnInfo = columnInfo;
                break;
            }
        }

        // column must be at least index 1 to be shifted left
        if (rightColumnInfo == null || rightColumnInfo.getColumnIndex() < 1) {
            return;
        }

        CsvColumnInfo<PsiElement> leftColumnInfo = columnInfoMap.get(rightColumnInfo.getColumnIndex() - 1);

        changeLeftAndRightColumnOrder(project, psiFile, leftColumnInfo, rightColumnInfo);
    }
}
