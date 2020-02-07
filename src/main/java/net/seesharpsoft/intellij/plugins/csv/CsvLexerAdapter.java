package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.FlexAdapter;

public class CsvLexerAdapter extends FlexAdapter {
    public CsvLexerAdapter(CsvValueSeparator separator, CsvEscapeCharacter escapeCharacter) {
        super(new CsvLexer(null, separator, escapeCharacter));
    }
}
