package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.FlexAdapter;

public class CsvLexerAdapter extends FlexAdapter {
    public CsvLexerAdapter(String separator) {
        super(new CsvLexer(null, separator));
    }
}
