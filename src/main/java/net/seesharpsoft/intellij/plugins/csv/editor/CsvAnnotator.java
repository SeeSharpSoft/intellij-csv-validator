package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.lang.annotation.*;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vcs.ui.FontUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.xml.util.XmlStringUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvColorSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.annotation.HighlightSeverity.INFORMATION;

@SuppressWarnings("MagicNumber")
public class CsvAnnotator implements Annotator {

    protected static final Key<TextAttributes> TAB_SEPARATOR_HIGHLIGHT_COLOR_KEY = Key.create("CSV_PLUGIN_TAB_SEPARATOR_HIGHLIGHT_COLOR");
    protected static final Key<Boolean> TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED_KEY = Key.create("CSV_PLUGIN_TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED");
    protected static final Key<Boolean> SHOW_INFO_BALLOON_KEY = Key.create("CSV_PLUGIN_SHOW_INFO_BALLOON");

    public static final HighlightSeverity CSV_COLUMN_INFO_SEVERITY =
            new HighlightSeverity("CSV_COLUMN_INFO_SEVERITY", INFORMATION.myVal);

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull final AnnotationHolder holder) {
        IElementType elementType = CsvHelper.getElementType(element);
        if ((elementType != CsvTypes.FIELD && elementType != CsvTypes.COMMA) || !(element.getContainingFile() instanceof CsvFile)) {
            return;
        }

        CsvFile csvFile = (CsvFile) element.getContainingFile();
        if (handleSeparatorElement(element, holder, elementType, csvFile)) {
            return;
        }

        CsvColumnInfo<PsiElement> columnInfo = csvFile.getColumnInfoMap().getColumnInfo(element);

        if (columnInfo != null) {
            PsiElement headerElement = columnInfo.getHeaderElement();
            String message = FontUtil.getHtmlWithFonts(
                    XmlStringUtil.escapeString(headerElement == null ? "" : headerElement.getText(), true)
            );
            String tooltip = null;
            if (showInfoBalloon(holder.getCurrentAnnotationSession())) {
                tooltip = XmlStringUtil.wrapInHtml(
                        String.format("%s<br /><br />Header: %s<br />Index: %d",
                                FontUtil.getHtmlWithFonts(
                                        XmlStringUtil.escapeString(element.getText(), true)
                                ),
                                message,
                                columnInfo.getColumnIndex() + (CsvEditorSettings.getInstance().isZeroBasedColumnNumbering() ? 0 : 1)
                        )
                );
            }
            TextRange textRange = columnInfo.getRowInfo(element).getTextRange();
            if (textRange.getStartOffset() - csvFile.getTextLength() == 0 && textRange.getStartOffset() > 0) {
                textRange = TextRange.from(textRange.getStartOffset() - 1, 1);
            }

            final Annotation annotation = holder.createAnnotation(CSV_COLUMN_INFO_SEVERITY, textRange, message, tooltip);
            annotation.setEnforcedTextAttributes(
                    CsvEditorSettings.getInstance().getValueColoring() == CsvEditorSettings.ValueColoring.RAINBOW ?
                            CsvColorSettings.getTextAttributesOfColumn(columnInfo.getColumnIndex(), holder.getCurrentAnnotationSession()) :
                            null
            );
            annotation.setNeedsUpdateOnTyping(false);
        }
    }

    protected boolean showInfoBalloon(@NotNull AnnotationSession annotationSession) {
        Boolean showInfoBalloon = annotationSession.getUserData(SHOW_INFO_BALLOON_KEY);
        if (showInfoBalloon == null) {
            showInfoBalloon = CsvEditorSettings.getInstance().isShowInfoBalloon();
            annotationSession.putUserData(SHOW_INFO_BALLOON_KEY, showInfoBalloon);
        }
        return showInfoBalloon;
    }

    protected boolean handleSeparatorElement(@NotNull PsiElement element, @NotNull AnnotationHolder holder, IElementType elementType, CsvFile csvFile) {
        if (elementType == CsvTypes.COMMA) {
            TextAttributes textAttributes = holder.getCurrentAnnotationSession().getUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR_KEY);
            if (!Boolean.TRUE.equals(holder.getCurrentAnnotationSession().getUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED_KEY))) {
                CsvValueSeparator separator = CsvHelper.getValueSeparator(csvFile);
                if (CsvEditorSettings.getInstance().isHighlightTabSeparator() && separator.equals(CsvValueSeparator.TAB)) {
                    textAttributes = new TextAttributes(null,
                            CsvEditorSettings.getInstance().getTabHighlightColor(),
                            null, null, 0);
                    holder.getCurrentAnnotationSession().putUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR_KEY, textAttributes);
                    holder.getCurrentAnnotationSession().putUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED_KEY, Boolean.TRUE);
                }
            }
            if (textAttributes != null) {
                final TextRange textRange = element.getTextRange();
                final Annotation annotation = holder.createAnnotation(CSV_COLUMN_INFO_SEVERITY, textRange, showInfoBalloon(holder.getCurrentAnnotationSession()) ? "↹" : null);
                annotation.setEnforcedTextAttributes(textAttributes);
                annotation.setNeedsUpdateOnTyping(false);
            }
            return true;
        }
        return false;
    }
}
