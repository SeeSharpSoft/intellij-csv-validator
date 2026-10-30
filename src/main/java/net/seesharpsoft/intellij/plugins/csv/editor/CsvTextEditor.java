package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import net.seesharpsoft.intellij.plugins.csv.actions.CsvEditorViewActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The platform's PSI-aware text editor with the CSV view switch in its editor tab.
 * It extends the platform implementation instead of delegating to it, because IDE features
 * such as Reader Mode and inline debugger values only recognize the platform's text editor type.
 */
public final class CsvTextEditor extends PsiAwareTextEditorImpl implements CsvFieldView {

    public CsvTextEditor(@NotNull Project project, @NotNull VirtualFile file, @NotNull TextEditorProvider provider) {
        super(project, file, provider);
    }

    @Override
    public @Nullable ActionGroup getTabActions() {
        return CsvEditorViewActions.getEditorViewActionGroup();
    }

    @Override
    public @Nullable CsvFieldPosition getSelectedField() {
        Editor editor = getEditor();
        if (editor.isDisposed()) return null;
        int offset = editor.getCaretModel().getOffset();
        return ReadAction.compute(() -> {
            PsiFile psiFile = getCommittedPsiFile(editor);
            return psiFile == null ? null : CsvFieldPosition.atOffset(psiFile, offset);
        });
    }

    @Override
    public void selectField(@NotNull CsvFieldPosition field) {
        Editor editor = getEditor();
        if (editor.isDisposed() || field.equals(getSelectedField())) return;
        int offset = ReadAction.compute(() -> {
            PsiFile psiFile = getCommittedPsiFile(editor);
            return psiFile == null ? -1 : field.findStartOffset(psiFile);
        });
        if (offset < 0) return;
        editor.getCaretModel().removeSecondaryCarets();
        editor.getCaretModel().moveToOffset(offset);
        editor.getSelectionModel().removeSelection();
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    }

    private @Nullable PsiFile getCommittedPsiFile(@NotNull Editor editor) {
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        return documentManager.isCommitted(editor.getDocument()) ? documentManager.getPsiFile(editor.getDocument()) : null;
    }
}
