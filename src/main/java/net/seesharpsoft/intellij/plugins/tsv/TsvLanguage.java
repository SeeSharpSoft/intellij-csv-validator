package net.seesharpsoft.intellij.plugins.tsv;

import com.intellij.lang.Language;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.CsvSeparatorHolder;
import net.seesharpsoft.intellij.plugins.csv.formatter.CsvCodeStyleSettings;

public class TsvLanguage extends Language implements CsvSeparatorHolder {
    public static final TsvLanguage INSTANCE = new TsvLanguage();

    private TsvLanguage() {
        super(CsvLanguage.INSTANCE, "tsv");
    }

    @Override
    public String getDisplayName() {
        return "TSV";
    }

    @Override
    public String getSeparator() {
        return CsvCodeStyleSettings.TAB_SEPARATOR;
    }
}