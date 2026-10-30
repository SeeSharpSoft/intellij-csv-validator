package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorTab;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvFileEditorProvider;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Switches between the CSV text and table editors from the editor tab bar.
 */
public abstract class CsvEditorViewActions extends ToggleAction implements DumbAware {

    public static final String GROUP_ID = "CsvEditorViewActions";

    private final String editorTypeId;

    protected CsvEditorViewActions(@NotNull String editorTypeId) {
        this.editorTypeId = editorTypeId;
    }

    public static @Nullable ActionGroup getEditorViewActionGroup() {
        return (ActionGroup) ActionManager.getInstance().getAction(GROUP_ID);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // editor windows and their composites are backed by Swing components
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        event.getPresentation().setEnabledAndVisible(findSwitchableTab(event) != null);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
        CsvEditorTab tab = findSwitchableTab(event);
        return tab != null && tab.isShowing(editorTypeId);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean state) {
        CsvEditorTab tab = findSwitchableTab(event);
        if (state && tab != null) {
            tab.show(editorTypeId);
        }
    }

    public static final class ShowTextEditor extends CsvEditorViewActions {
        public ShowTextEditor() {
            super(CsvFileEditorProvider.EDITOR_TYPE_ID);
        }
    }

    public static final class ShowTableEditor extends CsvEditorViewActions {
        public ShowTableEditor() {
            super(CsvTableEditorProvider.EDITOR_TYPE_ID);
        }
    }

    /**
     * Returns the tab of the action's file editor if it can switch between the CSV text and table editor.
     */
    private static @Nullable CsvEditorTab findSwitchableTab(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        FileEditor fileEditor = event.getData(PlatformCoreDataKeys.FILE_EDITOR);
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || fileEditor == null || file == null) {
            return null;
        }
        CsvEditorTab tab = CsvEditorTab.find(project, file, fileEditor);
        return tab != null && tab.hasTextAndTableEditor() ? tab : null;
    }
}
