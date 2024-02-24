package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import org.jetbrains.annotations.NotNull;

public abstract class CsvActionGroup extends ActionGroup {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
