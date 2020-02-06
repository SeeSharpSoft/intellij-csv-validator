package net.seesharpsoft.intellij.plugins.tsv;

import com.intellij.lang.Language;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.CsvSeparatorHolder;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public final class TsvLanguage extends Language implements CsvSeparatorHolder {
    public static final TsvLanguage INSTANCE = new TsvLanguage();

    private TsvLanguage() {
        super(CsvLanguage.INSTANCE, "tsv");
    }

    @Override
    public String getDisplayName() {
        return "TSV";
    }

    @Override
    public CsvEditorSettings.ValueSeparator getSeparator() {
        return CsvEditorSettings.ValueSeparator.TAB;
    }
}
