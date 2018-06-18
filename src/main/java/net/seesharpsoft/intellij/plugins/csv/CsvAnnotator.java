package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lang.annotation.*;
import com.intellij.openapi.editor.markup.AttributesFlyweight;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiElement;
import com.intellij.xml.util.XmlStringUtil;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CsvAnnotator implements Annotator {

    private static final TextAttributes EMPTY_TEXT_ATTRIBUTES = TextAttributes.fromFlyweight(AttributesFlyweight.create(null, null, 0, null, null, null));

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {
        if (CsvHelper.getElementType(element) != CsvTypes.FIELD) {
            return;
        }

        AnnotationSession currentAnnotationSession = holder.getCurrentAnnotationSession();
        Map<Integer, CsvColumnInfo<PsiElement>> columnInfoMap = currentAnnotationSession.getUserData(CsvHelper.COLUMN_INFO_KEY);
        if (columnInfoMap == null) {
            columnInfoMap = CsvHelper.createColumnInfoMap((CsvFile)element.getContainingFile());
            currentAnnotationSession.putUserData(CsvHelper.COLUMN_INFO_KEY, columnInfoMap);
        }

        CsvColumnInfo<PsiElement> columnInfo = getColumnInfo(element, columnInfoMap);

        if (columnInfo != null) {
            PsiElement headerElement = columnInfo.getHeaderElement();
            String message = XmlStringUtil.escapeString(headerElement == null ? "" : headerElement.getText(), true);
            String tooltip = XmlStringUtil.wrapInHtml(String.format("%s<br /><br />Column: %s<br />Index: %d", XmlStringUtil.escapeString(element.getText(), true), message, columnInfo.getColumnIndex()));

            Annotation annotation = holder.createAnnotation(HighlightSeverity.WARNING, element.getTextRange(), message, tooltip);
            annotation.setEnforcedTextAttributes(EMPTY_TEXT_ATTRIBUTES);
            annotation.setNeedsUpdateOnTyping(false);
        }
    }

    protected CsvColumnInfo<PsiElement> getColumnInfo(@NotNull final PsiElement element, @NotNull Map<Integer, CsvColumnInfo<PsiElement>> columnInfoMap) {
        for (Map.Entry<Integer, CsvColumnInfo<PsiElement>> entry : columnInfoMap.entrySet()) {
            CsvColumnInfo<PsiElement> columnInfo = entry.getValue();
            if (columnInfo.containsElement(element)) {
                return columnInfo;
            }
        }
        return null;
    }
}