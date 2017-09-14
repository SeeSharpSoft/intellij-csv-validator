package net.seesharpsoft.idea.plugins.csv;

import com.intellij.lang.Language;

public class CsvLanguage extends Language {
  public static final CsvLanguage INSTANCE = new CsvLanguage();

  private CsvLanguage() {
    super("csv");
  }

  @Override
  public String getDisplayName() {
    return "CSV";
  }
}