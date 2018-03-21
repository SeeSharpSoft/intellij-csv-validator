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

public class CsvShiftColumnRightIntentionAction extends CsvShiftColumnIntentionAction {

    public CsvShiftColumnRightIntentionAction() {
        super("Shift Column Right");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        CsvFile psiFile = (CsvFile)psiElement.getContainingFile();

        psiElement = CsvIntentionHelper.getParentFieldElement(psiElement);

        Map<Integer, CsvColumnInfo<PsiElement>> columnInfoMap = CsvHelper.createColumnInfoMap(psiFile);

        CsvColumnInfo<PsiElement> leftColumnInfo = null;
        for (CsvColumnInfo columnInfo : columnInfoMap.values()) {
            if (columnInfo.containsElement(psiElement)) {
                leftColumnInfo = columnInfo;
                break;
            }
        }

        // column must be at least index 1 to be shifted left
        if (leftColumnInfo == null || leftColumnInfo.getColumnIndex() + 1 >= columnInfoMap.size()) {
            return;
        }

        CsvColumnInfo<PsiElement> rightColumnInfo = columnInfoMap.get(leftColumnInfo.getColumnIndex() + 1);

        changeLeftAndRightColumnOrder(project, psiFile, leftColumnInfo, rightColumnInfo);
    }
}
