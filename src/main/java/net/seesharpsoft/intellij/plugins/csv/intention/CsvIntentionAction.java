package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CsvIntentionAction extends PsiElementBaseIntentionAction implements IntentionAction {

    protected CsvIntentionAction(String text) {
        setText(text);
    }

    @NotNull
    public String getFamilyName() {
        return getText();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable PsiElement element) {
        final PsiFile containingFile = element == null ? null : element.getContainingFile();
        return containingFile != null
                && containingFile.getLanguage().isKindOf(CsvLanguage.INSTANCE)
                && PsiDocumentManager.getInstance(project).getDocument(containingFile) != null;
    }
}
