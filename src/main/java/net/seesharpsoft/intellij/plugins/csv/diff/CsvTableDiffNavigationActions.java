package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public abstract class CsvTableDiffNavigationActions extends DumbAwareAction {
    static final String GROUP_ID = "CsvTableDiffNavigation";
    static final DataKey<CsvTableDiffViewer> VIEWER = DataKey.create("csv.table.diff.viewer");

    private final boolean forward;

    protected CsvTableDiffNavigationActions(boolean forward) {
        super(CsvPluginManager.getLocalizedText(forward ? "table.diff.next" : "table.diff.previous"));
        this.forward = forward;
        var nativeAction = ActionManager.getInstance()
                .getAction(forward ? IdeActions.ACTION_NEXT_DIFF : IdeActions.ACTION_PREVIOUS_DIFF);
        getTemplatePresentation().setIcon(nativeAction.getTemplatePresentation().getIcon());
        setEnabledInModalContext(true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        CsvTableDiffViewer viewer = event.getData(VIEWER);
        event.getPresentation().setVisible(viewer != null);
        event.getPresentation().setEnabled(viewer != null && viewer.canNavigate(forward, event));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        CsvTableDiffViewer viewer = event.getData(VIEWER);
        if (viewer != null) viewer.navigate(forward, event);
    }

    static void prependToToolbar(CsvTableDiffViewer viewer) {
        for (Container parent = viewer.getComponent().getParent(); parent instanceof JComponent ancestor; parent = parent.getParent()) {
            for (JComponent child : UIUtil.findComponentsOfType(ancestor, JComponent.class)) {
                if (!(child instanceof ActionToolbar toolbar)
                        || !ActionPlaces.DIFF_TOOLBAR.equals(toolbar.getPlace())
                        || toolbar.getToolbarDataContext().getData(VIEWER) != viewer) continue;
                if (!(toolbar.getActionGroup() instanceof DefaultActionGroup actions)) {
                    throw new IllegalStateException("Cannot prepend CSV navigation to the diff toolbar");
                }
                actions.add(ActionManager.getInstance().getAction(GROUP_ID), Constraints.FIRST);
                toolbar.updateActionsAsync();
                return;
            }
        }
    }

    public static final class Previous extends CsvTableDiffNavigationActions {
        public Previous() {
            super(false);
        }
    }

    public static final class Next extends CsvTableDiffNavigationActions {
        public Next() {
            super(true);
        }
    }
}
