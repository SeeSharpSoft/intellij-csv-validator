package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvFormattingInfo {

    private final CsvCodeStyleSettings csvCodeStyleSettings;
    private final PsiFile csvFile;
    private Map<PsiElement, Integer> columnWidths;

    public CsvFormattingInfo(CodeStyleSettings codeStyleSettings, PsiFile csvFile) {
        this.csvFile = csvFile;
        this.csvCodeStyleSettings = codeStyleSettings.getCustomSettings(CsvCodeStyleSettings.class);
    }

    public CsvCodeStyleSettings getCsvCodeStyleSettings() {
        return csvCodeStyleSettings;
    }

    public @Nullable Integer getColumnWidth(PsiElement field) {
        if (columnWidths == null) {
            columnWidths = calculateColumnWidths();
        }
        return columnWidths.get(field);
    }

    private Map<PsiElement, Integer> calculateColumnWidths() {
        List<List<CsvField>> columns = new ArrayList<>();
        for (PsiElement child = csvFile.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof CsvRecord record)) continue;
            int column = 0;
            for (CsvField field : record.getFieldList()) {
                if (column == columns.size()) columns.add(new ArrayList<>());
                columns.get(column++).add(field);
            }
        }

        Map<PsiElement, Integer> widths = new HashMap<>();
        for (List<CsvField> column : columns) {
            Integer maxWidth = column.stream()
                    .mapToInt(field -> CsvFormatHelper.getTextLength(field, csvCodeStyleSettings))
                    .max().orElseThrow();
            for (CsvField field : column) widths.put(field, maxWidth);
        }
        return widths;
    }
}
