package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider;
import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The editor tab of an editor window that shows a file, possibly with both the CSV text and table editor.
 * The tab is resolved via one of its file editor instances, because the same file can be open in several split windows.
 */
public record CsvEditorTab(@NotNull EditorWindow window, @NotNull EditorComposite composite) {

    public static @Nullable CsvEditorTab find(@NotNull Project project, @NotNull VirtualFile file, @NotNull FileEditor fileEditor) {
        for (EditorWindow window : FileEditorManagerEx.getInstanceEx(project).getWindows()) {
            EditorComposite composite = window.getComposite(file);
            if (composite != null && composite.getAllEditors().contains(fileEditor)) {
                return new CsvEditorTab(window, composite);
            }
        }
        return null;
    }

    public boolean contains(@NotNull FileEditor fileEditor) {
        return composite.getAllEditors().contains(fileEditor);
    }

    public boolean hasTextAndTableEditor() {
        return hasEditor(CsvFileEditorProvider.EDITOR_TYPE_ID) && hasEditor(CsvTableEditorProvider.EDITOR_TYPE_ID);
    }

    private boolean hasEditor(@NotNull String editorTypeId) {
        return composite.getAllProviders().stream()
                .map(FileEditorProvider::getEditorTypeId)
                .anyMatch(editorTypeId::equals);
    }

    public boolean isShowing(@NotNull String editorTypeId) {
        FileEditorWithProvider selected = composite.getSelectedWithProvider();
        return selected != null && editorTypeId.equals(selected.getProvider().getEditorTypeId());
    }

    public void show(@NotNull String editorTypeId) {
        composite.setSelectedEditor(editorTypeId);
        // Toolbar buttons do not take focus. Activating the window like a tab click focuses the shown editor
        // and lets the IDE notify the editors about the changed selection.
        window.setAsCurrentWindow(true);
    }
}
