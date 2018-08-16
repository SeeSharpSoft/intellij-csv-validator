package net.seesharpsoft.intellij.plugins.csv.annotation;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.markup.AttributesFlyweight;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.xml.util.XmlStringUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static com.intellij.spellchecker.SpellCheckerSeveritiesProvider.TYPO;

public class CsvAnnotator implements Annotator {

    public static final TextAttributes[] COLUMNS = new TextAttributes[] {
            TextAttributes.fromFlyweight(AttributesFlyweight.create(null, null, 0, null, null, null)),
            TextAttributes.fromFlyweight(AttributesFlyweight.create(null, new Color(240, 240, 240), 0, null, null, null)),
            TextAttributes.fromFlyweight(AttributesFlyweight.create(null, new Color(255, 240, 240), 0, null, null, null)),
            TextAttributes.fromFlyweight(AttributesFlyweight.create(null, new Color(240, 255, 240), 0, null, null, null)),
            TextAttributes.fromFlyweight(AttributesFlyweight.create(null, new Color(240, 240, 255), 0, null, null, null)),
            TextAttributes.fromFlyweight(AttributesFlyweight.create(null, new Color(255, 255, 240), 0, null, null, null)),
            TextAttributes.fromFlyweight(AttributesFlyweight.create(null, new Color(255, 240, 255), 0, null, null, null)),
            TextAttributes.fromFlyweight(AttributesFlyweight.create(null, new Color(240, 255, 255), 0, null, null, null)),
            TextAttributes.fromFlyweight(AttributesFlyweight.create(null, new Color(255, 255, 255), 0, null, null, null)),
    };

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
            annotation.setEnforcedTextAttributes(getTextAttributes(columnInfo));
            annotation.setNeedsUpdateOnTyping(false);
        }
    }

    protected TextAttributes getTextAttributes(CsvColumnInfo<PsiElement> columnInfo) {
        return COLUMNS[columnInfo.getColumnIndex() % COLUMNS.length];
    }
}