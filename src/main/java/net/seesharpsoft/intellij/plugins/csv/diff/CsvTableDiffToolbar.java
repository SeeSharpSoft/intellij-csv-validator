package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.diff.FrameDiffTool;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction;
import com.intellij.openapi.actionSystem.Separator;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

final class CsvTableDiffToolbar {
    private static final @Nullable Field RIGHT_TOOLBAR_ACTIONS = findRightToolbarActions();

    private CsvTableDiffToolbar() {
    }

    static FrameDiffTool.ToolbarComponents create(JComponent statusPanel, List<AnAction> viewerActions,
                                                  List<AnAction> popupActions) {
        FrameDiffTool.ToolbarComponents toolbar = new FrameDiffTool.ToolbarComponents();
        toolbar.statusPanel = statusPanel;
        toolbar.popupActions = popupActions;
        List<AnAction> actions = new ArrayList<>(viewerActions);
        toolbar.toolbarActions = actions;
        AnAction options = new OptionsActionGroup(popupActions);
        if (RIGHT_TOOLBAR_ACTIONS == null) {
            actions.add(Separator.getInstance());
            actions.add(options);
        } else {
            try {
                RIGHT_TOOLBAR_ACTIONS.set(toolbar, List.of(options));
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot configure the CSV diff options toolbar", exception);
            }
        }
        return toolbar;
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private static @Nullable Field findRightToolbarActions() {
        try {
            // The public split-toolbar field is absent from the minimum supported SDK.
            return FrameDiffTool.ToolbarComponents.class.getField("rightToolbarActions");
        } catch (NoSuchFieldException exception) {
            return null;
        }
    }

    private static final class OptionsActionGroup extends DefaultActionGroup implements RightAlignedToolbarAction {
        OptionsActionGroup(List<AnAction> actions) {
            super(CsvPluginManager.getLocalizedText("table.diff.options"), true);
            getTemplatePresentation().setIcon(AllIcons.General.GearPlain);
            addAll(actions);
        }
    }
}
