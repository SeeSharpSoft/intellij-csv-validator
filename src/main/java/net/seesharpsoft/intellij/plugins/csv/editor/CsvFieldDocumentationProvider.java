package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vcs.ui.FontUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.xml.util.XmlStringUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides the field information tooltip on demand. Persistent per-field annotations created one
 * highlight per value, which overwhelmed the highlighting infrastructure for large files.
 */
public class CsvFieldDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public @Nullable PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file,
                                                              @Nullable PsiElement contextElement, int targetOffset) {
        if (!(file instanceof CsvFile) || !isEnabled()) return null;
        return PsiTreeUtil.getParentOfType(contextElement, CsvField.class, false);
    }

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (!(element instanceof CsvField field) || !(field.getContainingFile() instanceof CsvFile file) || !isEnabled()) {
            return null;
        }
        int fieldIndex = CsvHelper.getFieldIndex(field);
        return XmlStringUtil.wrapInHtml(String.format("%s<br /><br />Header: %s<br />Index: %d",
                toHtml(field.getText()),
                toHtml(getHeaderText(file, fieldIndex)),
                fieldIndex + (CsvEditorSettings.getInstance().isZeroBasedColumnNumbering() ? 0 : 1)));
    }

    private static boolean isEnabled() {
        return CsvEditorSettings.getInstance().isShowInfoBalloon();
    }

    private static String getHeaderText(@NotNull CsvFile file, int index) {
        PsiElement header = PsiHelper.getNthChildOfType(file.getFirstChild(), index, CsvField.class);
        return header instanceof CsvField ? header.getText() : "";
    }

    private static String toHtml(@NotNull String text) {
        return FontUtil.getHtmlWithFonts(XmlStringUtil.escapeString(text, true));
    }
}
