package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.actions.CsvEditorViewActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The platform's PSI-aware text editor with the CSV view switch in its editor tab.
 * It extends the platform implementation instead of delegating to it, because IDE features
 * such as Reader Mode and inline debugger values only recognize the platform's text editor type.
 */
public final class CsvTextEditor extends PsiAwareTextEditorImpl {

    public CsvTextEditor(@NotNull Project project, @NotNull VirtualFile file, @NotNull TextEditorProvider provider) {
        super(project, file, provider);
    }

    @Override
    public @Nullable ActionGroup getTabActions() {
        return CsvEditorViewActions.getEditorViewActionGroup();
    }
}
