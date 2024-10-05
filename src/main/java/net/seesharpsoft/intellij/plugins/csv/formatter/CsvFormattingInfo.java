package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;

public class CsvFormattingInfo {

    private final CsvCodeStyleSettings csvCodeStyleSettings;
    private final PsiFile csvFile;
    private CsvColumnInfoMap<PsiElement> csvColumnInfoMap;

    public CsvFormattingInfo(CodeStyleSettings codeStyleSettings, PsiFile csvFile) {
        this.csvFile = csvFile;
        this.csvCodeStyleSettings = codeStyleSettings.getCustomSettings(CsvCodeStyleSettings.class);
    }

    private int getTextMaxLength(CsvField field) {
        return CsvFormatHelper.getTextLength(field.getText().strip(), this.csvCodeStyleSettings);
    }

    public CsvCodeStyleSettings getCsvCodeStyleSettings() {
        return csvCodeStyleSettings;
    }

    public CsvColumnInfoMap<PsiElement> getCsvColumnInfoMap() {
        if (this.csvColumnInfoMap == null) {
            this.csvColumnInfoMap = CsvHelper.createColumnInfoMap(this.csvFile, this::getTextMaxLength);
        }
        return this.csvColumnInfoMap;
    }
}
