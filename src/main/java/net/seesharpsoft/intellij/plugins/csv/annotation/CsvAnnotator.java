package net.seesharpsoft.intellij.plugins.csv.annotation;

import com.intellij.lang.annotation.*;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.xml.util.XmlStringUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorSettingsExternalizable;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.spellchecker.SpellCheckerSeveritiesProvider.TYPO;

@SuppressWarnings("MagicNumber")
public class CsvAnnotator implements Annotator {

    protected static final Integer MAX_COLUMN_HIGHLIGHT_COLORS = 10;
    protected static final Key<Integer> MAX_NO_OF_DEFINED_COLUMN_HIGHLIGHT_COLORS = Key.create("CSV_LAST_DEFINED_COLOR_INDEX_KEY");
    protected static final Key<TextAttributes> TAB_SEPARATOR_HIGHLIGHT_COLOR = Key.create("CSV_TAB_SEPARATOR_HIGHLIGHT_COLOR");
    protected static final Key<Boolean> TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED = Key.create("CSV_TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED");

    public static final ColorDescriptor[] COLOR_DESCRIPTORS;

    static {
        List<ColorDescriptor> colorDescriptorList = new ArrayList();
        for (int i = 0; i < MAX_COLUMN_HIGHLIGHT_COLORS; ++i) {
            colorDescriptorList.add(new ColorDescriptor(String.format("Column Highlighting Color %d", i + 1),
                    ColorKey.createColorKey(String.format("CSV_COLUMN_COLOR_%d", i + 1), (Color) null), ColorDescriptor.Kind.BACKGROUND));
        }
        COLOR_DESCRIPTORS = colorDescriptorList.toArray(new ColorDescriptor[MAX_COLUMN_HIGHLIGHT_COLORS]);
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
            String tooltip = XmlStringUtil.wrapInHtml(
                    String.format("%s<br /><br />Header: %s<br />Index: %d",
                            XmlStringUtil.escapeString(element.getText(), true),
                            message,
                            columnInfo.getColumnIndex()
                    )
            );
            TextRange textRange = columnInfo.getRowInfo(element).getTextRange();
            if (textRange.getStartOffset() - csvFile.getTextLength() == 0 && textRange.getStartOffset() > 0) {
                textRange = TextRange.from(textRange.getStartOffset() - 1, 1);
            }

            Annotation annotation = holder.createAnnotation(CSV_COLUMN_INFO_SEVERITY, textRange, message, tooltip);
            annotation.setEnforcedTextAttributes(getTextAttributes(holder.getCurrentAnnotationSession(), columnInfo));
            annotation.setNeedsUpdateOnTyping(false);
        }
    }

    protected boolean handleSeparatorElement(@NotNull PsiElement element, @NotNull AnnotationHolder holder, IElementType elementType, CsvFile csvFile) {
        if (elementType == CsvTypes.COMMA) {
            TextAttributes textAttributes = holder.getCurrentAnnotationSession().getUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR);
            if (!Boolean.TRUE.equals(holder.getCurrentAnnotationSession().getUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED))) {
                String separator = CsvCodeStyleSettings.getCurrentSeparator(csvFile.getProject(), csvFile.getLanguage());
                if (CsvEditorSettingsExternalizable.getInstance().isHighlightTabSeparator() && separator.equals(CsvCodeStyleSettings.TAB_SEPARATOR)) {
                    textAttributes = new TextAttributes(null,
                            CsvEditorSettingsExternalizable.getInstance().getTabHighlightColor(),
                            null, null, 0);
                    holder.getCurrentAnnotationSession().putUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR, textAttributes);
                    holder.getCurrentAnnotationSession().putUserData(TAB_SEPARATOR_HIGHLIGHT_COLOR_DETERMINED, Boolean.TRUE);
                }
            }
            if (textAttributes != null) {
                Annotation annotation = holder.createAnnotation(CSV_COLUMN_INFO_SEVERITY, element.getTextRange(), "<TAB>");
                annotation.setEnforcedTextAttributes(textAttributes);
                annotation.setNeedsUpdateOnTyping(false);
            }
            return true;
        }
        return false;
    }

    protected TextAttributes getTextAttributes(AnnotationSession annotationSession, CsvColumnInfo<PsiElement> columnInfo) {
        EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
        Integer maxNoOfDefinedColumnHighlightColors = annotationSession.getUserData(MAX_NO_OF_DEFINED_COLUMN_HIGHLIGHT_COLORS);
        if (maxNoOfDefinedColumnHighlightColors == null) {
            maxNoOfDefinedColumnHighlightColors = 0;
            if (CsvEditorSettingsExternalizable.getInstance().isColumnHighlightingEnabled()) {
                for (int colorDescriptorIndex = 0; colorDescriptorIndex < COLOR_DESCRIPTORS.length; ++colorDescriptorIndex) {
                    if (editorColorsScheme.getColor(COLOR_DESCRIPTORS[colorDescriptorIndex].getKey()) != null) {
                        maxNoOfDefinedColumnHighlightColors = colorDescriptorIndex + 1;
                    }
                }
            }
            annotationSession.putUserData(MAX_NO_OF_DEFINED_COLUMN_HIGHLIGHT_COLORS, maxNoOfDefinedColumnHighlightColors);
        }
        return maxNoOfDefinedColumnHighlightColors == 0 ? null :
                new TextAttributes(null,
                        editorColorsScheme.getColor(COLOR_DESCRIPTORS[columnInfo.getColumnIndex() % maxNoOfDefinedColumnHighlightColors].getKey()),
                        null, null, 0);
    }
}