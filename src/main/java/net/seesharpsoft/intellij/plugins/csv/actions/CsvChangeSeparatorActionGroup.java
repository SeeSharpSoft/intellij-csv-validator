package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.*;
import com.intellij.psi.PsiFile;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.CsvSeparatorHolder;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvChangeSeparatorActionGroup extends ActionGroup {

    private static final AnAction[] CSV_SEPARATOR_CHANGE_ACTIONS;

    static {
        CSV_SEPARATOR_CHANGE_ACTIONS = new AnAction[CsvCodeStyleSettings.SUPPORTED_SEPARATORS.length + 1];
        for (int i = 0; i < CSV_SEPARATOR_CHANGE_ACTIONS.length - 1; ++i) {
            CSV_SEPARATOR_CHANGE_ACTIONS[i] = new CsvChangeSeparatorAction(CsvCodeStyleSettings.SUPPORTED_SEPARATORS[i], CsvCodeStyleSettings.SUPPORTED_SEPARATORS_DISPLAY[i]);
        }
        CSV_SEPARATOR_CHANGE_ACTIONS[CSV_SEPARATOR_CHANGE_ACTIONS.length - 1] = new CsvDefaultSeparatorAction();
    }

    @Override
    public void update(AnActionEvent anActionEvent) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        Language language = psiFile == null ? null : psiFile.getLanguage();
        anActionEvent.getPresentation().setEnabledAndVisible(psiFile != null && language != null &&
                language.isKindOf(CsvLanguage.INSTANCE) && !(language instanceof CsvSeparatorHolder)
        );

        if (psiFile != null) {
            anActionEvent.getPresentation()
                    .setText(String.format("CSV Separator: %s", CsvCodeStyleSettings.getSeparatorDisplayText(CsvCodeStyleSettings.getCurrentSeparator(psiFile))));
        }
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
        return CSV_SEPARATOR_CHANGE_ACTIONS;
    }
}
