package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.FlexAdapter;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorSettings;

public class CsvLexerAdapter extends FlexAdapter {
    public CsvLexerAdapter(String separator, CsvEditorSettings.EscapeCharacter escapeCharacter) {
        super(new CsvLexer(null, separator, escapeCharacter));
    }
}
