package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.PlainTextLanguage;

public final class CsvLanguage extends Language {
    public static final CsvLanguage INSTANCE = new CsvLanguage();

    private CsvLanguage() {
        super(PlainTextLanguage.INSTANCE, "csv");
    }

    @Override
    public String getDisplayName() {
        return "CSV";
    }
}