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
import com.intellij.xml.util.XmlStringUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.spellchecker.SpellCheckerSeveritiesProvider.TYPO;

@SuppressWarnings("MagicNumber")
public class CsvAnnotator implements Annotator {

    protected static final Integer MAX_COLUMN_HIGHLIGHT_COLORS = 10;
    protected static final Key<Integer> MAX_NO_OF_DEFINED_COLUMN_HIGHLIGHT_COLORS = Key.create("CSV_LAST_DEFINED_COLOR_INDEX_KEY");
    
    public static final ColorDescriptor[] COLOR_DESCRIPTORS;
    static {
        List<ColorDescriptor> colorDescriptorList = new ArrayList();
        for (int i = 0; i < MAX_COLUMN_HIGHLIGHT_COLORS; ++i) {
            colorDescriptorList.add(new ColorDescriptor(String.format("Column Highlighting Color %d", i + 1), ColorKey.createColorKey(String.format("CSV_COLUMN_COLOR_%d", i + 1), (Color) null), ColorDescriptor.Kind.BACKGROUND));
        }
        COLOR_DESCRIPTORS = colorDescriptorList.toArray(new ColorDescriptor[MAX_COLUMN_HIGHLIGHT_COLORS]);
    }

    public static final HighlightSeverity CSV_COLUMN_INFO_SEVERITY =
            new HighlightSeverity("CSV_COLUMN_INFO_SEVERITY", TYPO.myVal + 5);

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull final AnnotationHolder holder) {
        if (CsvHelper.getElementType(element) != CsvTypes.FIELD || !(element.getContainingFile() instanceof CsvFile)) {
            return;
        }

        CsvFile csvFile = (CsvFile) element.getContainingFile();
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

    protected TextAttributes getTextAttributes(AnnotationSession annotationSession, CsvColumnInfo<PsiElement> columnInfo) {
        EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
        Integer maxNoOfDefinedColumnHighlightColors = annotationSession.getUserData(MAX_NO_OF_DEFINED_COLUMN_HIGHLIGHT_COLORS);
        if (maxNoOfDefinedColumnHighlightColors == null) {
            maxNoOfDefinedColumnHighlightColors = 0;
            for (int colorDescriptorIndex = 0; colorDescriptorIndex < COLOR_DESCRIPTORS.length; ++colorDescriptorIndex) {
                if (editorColorsScheme.getColor(COLOR_DESCRIPTORS[colorDescriptorIndex].getKey()) != null) {
                    maxNoOfDefinedColumnHighlightColors = colorDescriptorIndex + 1;
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