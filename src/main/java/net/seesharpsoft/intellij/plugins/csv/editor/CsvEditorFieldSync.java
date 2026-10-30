package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps the selected field when switching between the CSV text and table editor of an editor tab:
 * the newly shown editor selects the field that was selected in the previously shown one.
 */
public final class CsvEditorFieldSync implements FileEditorManagerListener {

    private final Project project;

    public CsvEditorFieldSync(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        if (!CsvEditorSettings.getInstance().isSyncSelectedField()) return;
        VirtualFile file = event.getNewFile();
        if (file == null || !file.equals(event.getOldFile()) ||
                !(event.getOldEditor() instanceof CsvFieldView source) || !(event.getNewEditor() instanceof CsvFieldView target) ||
                !isViewSwitchWithinTab(file, source, target)) {
            return;
        }
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) return;
        // Both views address fields via PSI, which lags behind recent text edits until the document is committed.
        PsiDocumentManager.getInstance(project).performForCommittedDocument(document, () -> {
            CsvFieldPosition field = source.getSelectedField();
            if (field != null) target.selectField(field);
        });
    }

    /**
     * Focusing another window that shows the same file changes the selected editor, too. That editor must keep its
     * own selection, e.g. so that a click into its table is not overridden.
     */
    private boolean isViewSwitchWithinTab(@NotNull VirtualFile file, @NotNull CsvFieldView source, @NotNull CsvFieldView target) {
        CsvEditorTab tab = CsvEditorTab.find(project, file, target);
        return tab != null && tab.contains(source);
    }
}
