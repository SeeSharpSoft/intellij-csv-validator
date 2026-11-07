package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvDocumentReorder;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CsvShiftColumnIntentionAction extends CsvIntentionAction {
    private final int offset;

    protected CsvShiftColumnIntentionAction(String text, int offset) {
        super(text);
        this.offset = offset;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable PsiElement element) {
        return super.isAvailable(project, editor, element) && element != null &&
                element.getContainingFile() instanceof CsvFile &&
                CsvHelper.getParentFieldElement(element) != null &&
                !PsiTreeUtil.hasErrorElements(element.getContainingFile());
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (!isAvailable(project, editor, element)) return;
        CsvFile csvFile = (CsvFile) element.getContainingFile();
        int[] order = ReadAction.compute(() -> {
            int column = CsvHelper.getFieldIndex(element);
            int count = CsvHelper.getColumnCount(csvFile);
            if (column + offset < 0 || column + offset >= count) return null;
            return CsvDocumentReorder.moveOrder(count, new int[]{column}, offset);
        });
        if (order != null) CsvPsiTreeUpdater.edit(csvFile, updater -> updater.reorderColumns(order));
    }
}
