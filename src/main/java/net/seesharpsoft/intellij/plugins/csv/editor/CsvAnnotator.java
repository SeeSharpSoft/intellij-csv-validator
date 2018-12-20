package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.lang.annotation.*;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.xml.util.XmlStringUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;
import static com.intellij.spellchecker.SpellCheckerSeveritiesProvider.TYPO;

@SuppressWarnings("MagicNumber")
public class CsvAnnotator implements Annotator {

    protected static final Integer MAX_COLUMN_HIGHLIGHT_COLORS = 10;
    protected static final Key<List<TextAttributes>> COLUMN_HIGHLIGHT_TEXT_ATTRIBUTES_KEY = Key.create("CSV_PLUGIN_COLUMN_HIGHLIGHT_ATTRIBUTES");
    protected static final Key<TextAttributes> TAB_SEPARATOR_HIGHLIGHT_COLOR_KEY = Key.create("CSV_PLUGIN_TAB_SEPARATOR_HIGHLIGHT_COLOR");
    protected static final Key<Boolean> TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED_KEY = Key.create("CSV_PLUGIN_TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED");
    protected static final Key<Boolean> SHOW_INFO_BALLOON_KEY = Key.create("CSV_PLUGIN_SHOW_INFO_BALLOON");

    public static final List<TextAttributesKey> COLUMN_HIGHLIGHT_ATTRIBUTES;

    static {
        COLUMN_HIGHLIGHT_ATTRIBUTES = new ArrayList<>();
        for (int i = 0; i < MAX_COLUMN_HIGHLIGHT_COLORS; ++i) {
            TextAttributesKey textAttributesKey = createTextAttributesKey(String.format("CSV_COLUMN_HIGHLIGHT_ATTRIBUTE_%d", i + 1), DefaultLanguageHighlighterColors.STRING);
            COLUMN_HIGHLIGHT_ATTRIBUTES.add(textAttributesKey);
        }
    }

    public static final HighlightSeverity CSV_COLUMN_INFO_SEVERITY =
            new HighlightSeverity("CSV_COLUMN_INFO_SEVERITY", TYPO.myVal + 5);

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

        CsvColumnInfo<PsiElement> columnInfo = csvFile.getMyColumnInfoMap().getColumnInfo(element);

        if (columnInfo != null) {
            PsiElement headerElement = columnInfo.getHeaderElement();
            String message = XmlStringUtil.escapeString(headerElement == null ? "" : headerElement.getText(), true);
            String tooltip = null;
            if (showInfoBalloon(holder.getCurrentAnnotationSession())) {
                tooltip = XmlStringUtil.wrapInHtml(
                        String.format("%s<br /><br />Header: %s<br />Index: %d",
                                XmlStringUtil.escapeString(element.getText(), true),
                                message,
                                columnInfo.getColumnIndex()
                        )
                );
            }
            TextRange textRange = columnInfo.getRowInfo(element).getTextRange();
            if (textRange.getStartOffset() - csvFile.getTextLength() == 0 && textRange.getStartOffset() > 0) {
                textRange = TextRange.from(textRange.getStartOffset() - 1, 1);
            }

            Annotation annotation = holder.createAnnotation(CSV_COLUMN_INFO_SEVERITY, textRange, message, tooltip);
            annotation.setEnforcedTextAttributes(getTextAttributes(holder.getCurrentAnnotationSession(), columnInfo));
            annotation.setNeedsUpdateOnTyping(false);
        }
    }

    protected boolean showInfoBalloon(@NotNull AnnotationSession annotationSession) {
        Boolean showInfoBalloon = annotationSession.getUserData(SHOW_INFO_BALLOON_KEY);
        if (showInfoBalloon == null) {
            showInfoBalloon = CsvEditorSettingsExternalizable.getInstance().isShowInfoBalloon();
            annotationSession.putUserData(SHOW_INFO_BALLOON_KEY, showInfoBalloon);
        }
        return showInfoBalloon;
    }

    protected boolean handleSeparatorElement(@NotNull PsiElement element, @NotNull AnnotationHolder holder, IElementType elementType, CsvFile csvFile) {
        if (elementType == CsvTypes.COMMA) {
            TextAttributes textAttributes = holder.getCurrentAnnotationSession().getUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR_KEY);
            if (!Boolean.TRUE.equals(holder.getCurrentAnnotationSession().getUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED_KEY))) {
                String separator = CsvCodeStyleSettings.getCurrentSeparator(csvFile.getProject(), csvFile.getLanguage());
                if (CsvEditorSettingsExternalizable.getInstance().isHighlightTabSeparator() && separator.equals(CsvCodeStyleSettings.TAB_SEPARATOR)) {
                    textAttributes = new TextAttributes(null,
                            CsvEditorSettingsExternalizable.getInstance().getTabHighlightColor(),
                            null, null, 0);
                    holder.getCurrentAnnotationSession().putUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR_KEY, textAttributes);
                    holder.getCurrentAnnotationSession().putUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED_KEY, Boolean.TRUE);
                }
            }
            if (textAttributes != null) {
                Annotation annotation = holder.createAnnotation(
                        CSV_COLUMN_INFO_SEVERITY,
                        element.getTextRange(),
                        showInfoBalloon(holder.getCurrentAnnotationSession()) ? "↹" : null
                );
                annotation.setEnforcedTextAttributes(textAttributes);
                annotation.setNeedsUpdateOnTyping(false);
            }
            return true;
        }
        return false;
    }

    public static TextAttributes getTextAttributes(UserDataHolder userDataHolder, CsvColumnInfo<PsiElement> columnInfo) {
        EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
        List<TextAttributes> textAttributeList = userDataHolder.getUserData(COLUMN_HIGHLIGHT_TEXT_ATTRIBUTES_KEY);
        if (textAttributeList == null) {
            textAttributeList = new ArrayList<>();
            int maxIndex = 0;
            if (CsvEditorSettingsExternalizable.getInstance().isColumnHighlightingEnabled()) {
                for (int colorDescriptorIndex = 0; colorDescriptorIndex < MAX_COLUMN_HIGHLIGHT_COLORS; ++colorDescriptorIndex) {
                    TextAttributesKey textAttributesKey = COLUMN_HIGHLIGHT_ATTRIBUTES.get(colorDescriptorIndex);
                    TextAttributes textAttributes = editorColorsScheme.getAttributes(textAttributesKey);
                    textAttributeList.add(textAttributes);
                    if (!textAttributesKey.getDefaultAttributes().equals(textAttributes)) {
                        maxIndex = colorDescriptorIndex;
                    }
                }
            }
            userDataHolder.putUserData(COLUMN_HIGHLIGHT_TEXT_ATTRIBUTES_KEY, textAttributeList.subList(0, maxIndex + 1));
        }
        return textAttributeList.isEmpty() ? null : textAttributeList.get(columnInfo.getColumnIndex() % textAttributeList.size());
    }
}