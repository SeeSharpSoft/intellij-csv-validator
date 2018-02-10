package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;

import java.util.HashMap;
import java.util.Map;

public class CsvHelper {

    public static Map<Integer, CsvColumnInfo<PsiElement>> createColumnInfoMap(CsvFile csvFile) {
        Map<Integer, CsvColumnInfo<PsiElement>> columnInfoMap = new HashMap<>();
        CsvRecord[] records = PsiTreeUtil.getChildrenOfType(csvFile, CsvRecord.class);
        int row = 0;
        for (CsvRecord record : records) {
            int column = 0;
            for (CsvField field : record.getFieldList()) {
                Integer length = field.getTextLength();
                if (!columnInfoMap.containsKey(column)) {
                    columnInfoMap.put(column, new CsvColumnInfo(column, length));
                } else if (columnInfoMap.get(column).getMaxLength() < length) {
                    columnInfoMap.get(column).setMaxLength(length);
                }
                columnInfoMap.get(column).addElement(field, row);
                ++column;
            }
            ++row;
        }
        return columnInfoMap;
    }

}
