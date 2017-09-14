package net.seesharpsoft.idea.plugins.csv;

import com.intellij.lexer.FlexAdapter;

import java.io.Reader;

public class CsvLexerAdapter extends FlexAdapter {
  public CsvLexerAdapter() {
    super(new CsvLexer((Reader) null));
  }
}
