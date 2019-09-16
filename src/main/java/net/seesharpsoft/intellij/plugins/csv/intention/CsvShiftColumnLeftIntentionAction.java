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

public class CsvShiftColumnLeftIntentionAction extends CsvShiftColumnIntentionAction {

    public CsvShiftColumnLeftIntentionAction() {
        super("Shift Column Left");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull final PsiElement psiElement) throws IncorrectOperationException {
        CsvFile csvFile = (CsvFile) psiElement.getContainingFile();

        PsiElement element = CsvHelper.getParentFieldElement(psiElement);

        CsvColumnInfoMap<PsiElement> columnInfoMap = csvFile.getColumnInfoMap();
        CsvColumnInfo<PsiElement> rightColumnInfo = columnInfoMap.getColumnInfo(element);

        // column must be at least index 1 to be shifted left
        if (rightColumnInfo == null || rightColumnInfo.getColumnIndex() < 1) {
            return;
        }

        CsvColumnInfo<PsiElement> leftColumnInfo = columnInfoMap.getColumnInfo(rightColumnInfo.getColumnIndex() - 1);

        changeLeftAndRightColumnOrder(project, csvFile, leftColumnInfo, rightColumnInfo);
    }
}
