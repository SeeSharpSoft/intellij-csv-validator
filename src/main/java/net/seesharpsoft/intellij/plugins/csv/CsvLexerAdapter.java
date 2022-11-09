package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.FlexAdapter;

public class CsvLexerAdapter extends FlexAdapter implements CsvSeparatorHolder {


    public CsvLexerAdapter(CsvValueSeparator separator, CsvEscapeCharacter escapeCharacter, boolean supportComments) {
        super(new CsvLexer(null, separator, escapeCharacter, supportComments));
    }

    @Override
    public CsvLexer getFlex() {
        return (CsvLexer) super.getFlex();
    }

    @Override
    public CsvValueSeparator getSeparator() {
        return getFlex().getSeparator();
    }
}
