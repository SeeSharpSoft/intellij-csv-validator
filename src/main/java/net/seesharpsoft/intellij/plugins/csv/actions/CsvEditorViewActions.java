package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider;
import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
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
        event.getPresentation().setEnabledAndVisible(CsvEditorViews.find(event) != null);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
        CsvEditorViews views = CsvEditorViews.find(event);
        return views != null && views.isShowing(editorTypeId);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean state) {
        CsvEditorViews views = CsvEditorViews.find(event);
        if (state && views != null) {
            views.show(editorTypeId);
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
     * The editor tab containing both CSV editors of the action's file editor.
     * The tab is resolved via its file editor instance, because the same file can be open in several split windows.
     */
    private record CsvEditorViews(@NotNull EditorWindow window, @NotNull EditorComposite composite) {

        static @Nullable CsvEditorViews find(@NotNull AnActionEvent event) {
            Project project = event.getProject();
            FileEditor fileEditor = event.getData(PlatformCoreDataKeys.FILE_EDITOR);
            VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
            if (project == null || fileEditor == null || file == null) {
                return null;
            }
            for (EditorWindow window : FileEditorManagerEx.getInstanceEx(project).getWindows()) {
                EditorComposite composite = window.getComposite(file);
                if (composite != null && composite.getAllEditors().contains(fileEditor)) {
                    return hasTextAndTableEditor(composite) ? new CsvEditorViews(window, composite) : null;
                }
            }
            return null;
        }

        private static boolean hasTextAndTableEditor(@NotNull EditorComposite composite) {
            return hasEditor(composite, CsvFileEditorProvider.EDITOR_TYPE_ID) &&
                    hasEditor(composite, CsvTableEditorProvider.EDITOR_TYPE_ID);
        }

        private static boolean hasEditor(@NotNull EditorComposite composite, @NotNull String editorTypeId) {
            return composite.getAllProviders().stream()
                    .map(FileEditorProvider::getEditorTypeId)
                    .anyMatch(editorTypeId::equals);
        }

        boolean isShowing(@NotNull String editorTypeId) {
            FileEditorWithProvider selected = composite.getSelectedWithProvider();
            return selected != null && editorTypeId.equals(selected.getProvider().getEditorTypeId());
        }

        void show(@NotNull String editorTypeId) {
            composite.setSelectedEditor(editorTypeId);
            // Toolbar buttons do not take focus. Activating the window like a tab click focuses the shown editor
            // and lets the IDE notify the editors about the changed selection.
            window.setAsCurrentWindow(true);
        }
    }
}
