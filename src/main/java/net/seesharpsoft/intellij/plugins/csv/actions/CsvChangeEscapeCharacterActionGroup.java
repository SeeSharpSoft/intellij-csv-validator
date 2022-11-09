package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiFile;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvChangeEscapeCharacterActionGroup extends ActionGroup {

    private static final AnAction[] CSV_ESCAPE_CHARACTER_CHANGE_ACTIONS;

    static {
        CSV_ESCAPE_CHARACTER_CHANGE_ACTIONS = new AnAction[CsvEscapeCharacter.values().length + 1];
        for (int i = 0; i < CSV_ESCAPE_CHARACTER_CHANGE_ACTIONS.length - 1; ++i) {
            CSV_ESCAPE_CHARACTER_CHANGE_ACTIONS[i] = new CsvChangeEscapeCharacterAction(CsvEscapeCharacter.values()[i]);
        }
        CSV_ESCAPE_CHARACTER_CHANGE_ACTIONS[CSV_ESCAPE_CHARACTER_CHANGE_ACTIONS.length - 1] = new CsvDefaultEscapeCharacterAction();
    }

    @Override
    public void update(AnActionEvent anActionEvent) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        boolean canChangeEscapeCharacter = CsvHelper.isCsvFile(psiFile) && psiFile.getLanguage().isKindOf(CsvLanguage.INSTANCE);

        anActionEvent.getPresentation().setEnabledAndVisible(canChangeEscapeCharacter);
        if (canChangeEscapeCharacter) {
            CsvEscapeCharacter escapeCharacter = CsvHelper.getEscapeCharacter(psiFile);
            anActionEvent.getPresentation().setText(String.format("CSV Escape Character: %s", escapeCharacter.getDisplay()));
        }
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
        return CSV_ESCAPE_CHARACTER_CHANGE_ACTIONS;
    }
}
