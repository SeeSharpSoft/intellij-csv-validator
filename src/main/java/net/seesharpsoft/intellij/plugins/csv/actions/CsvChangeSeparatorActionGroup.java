package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiFile;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvSeparatorHolder;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvChangeSeparatorActionGroup extends ActionGroup {

    private static final AnAction[] CSV_SEPARATOR_CHANGE_ACTIONS;

    static {
        CSV_SEPARATOR_CHANGE_ACTIONS = new AnAction[CsvValueSeparator.values().length + 2];
        for (int i = 0; i < CSV_SEPARATOR_CHANGE_ACTIONS.length - 2; ++i) {
            CSV_SEPARATOR_CHANGE_ACTIONS[i] = new CsvChangeSeparatorAction(CsvValueSeparator.values()[i]);
        }
        CSV_SEPARATOR_CHANGE_ACTIONS[CSV_SEPARATOR_CHANGE_ACTIONS.length - 2] = new CsvCustomSeparatorAction();
        CSV_SEPARATOR_CHANGE_ACTIONS[CSV_SEPARATOR_CHANGE_ACTIONS.length - 1] = new CsvDefaultSeparatorAction();
    }

    @Override
    public void update(AnActionEvent anActionEvent) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        boolean isCsvFile = CsvHelper.isCsvFile(psiFile);
        Language language = psiFile == null ? null : psiFile.getLanguage();
        anActionEvent.getPresentation().setEnabledAndVisible(isCsvFile && !(language instanceof CsvSeparatorHolder));

        if (isCsvFile) {
            anActionEvent.getPresentation()
                    .setText(String.format("CSV Value Separator: %s",
                            CsvHelper.getValueSeparator(psiFile).getDisplay())
                    );
        }
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
        return CSV_SEPARATOR_CHANGE_ACTIONS;
    }
}
