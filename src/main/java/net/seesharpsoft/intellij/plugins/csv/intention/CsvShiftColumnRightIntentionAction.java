package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import org.jetbrains.annotations.NotNull;

public class CsvShiftColumnRightIntentionAction extends CsvShiftColumnIntentionAction {

    public CsvShiftColumnRightIntentionAction() {
        super("Shift Column Right");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        CsvFile csvFile = (CsvFile)psiElement.getContainingFile();

        psiElement = CsvHelper.getParentFieldElement(psiElement);

        CsvColumnInfoMap<PsiElement> columnInfoMap = csvFile.getMyColumnInfoMap();
        CsvColumnInfo<PsiElement> leftColumnInfo = columnInfoMap.getColumnInfo(psiElement);

        // column must be at least index 1 to be shifted left
        if (leftColumnInfo == null || leftColumnInfo.getColumnIndex() + 1 >= columnInfoMap.getColumnInfos().size()) {
            return;
        }

        CsvColumnInfo<PsiElement> rightColumnInfo = columnInfoMap.getColumnInfo(leftColumnInfo.getColumnIndex() + 1);

        changeLeftAndRightColumnOrder(project, csvFile, leftColumnInfo, rightColumnInfo);
    }
}
