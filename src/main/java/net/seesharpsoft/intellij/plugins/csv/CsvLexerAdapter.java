package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.FlexAdapter;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public class CsvLexerAdapter extends FlexAdapter {
    public CsvLexerAdapter(CsvEditorSettings.ValueSeparator separator, CsvEditorSettings.EscapeCharacter escapeCharacter) {
        super(new CsvLexer(null, separator, escapeCharacter));
    }
}
