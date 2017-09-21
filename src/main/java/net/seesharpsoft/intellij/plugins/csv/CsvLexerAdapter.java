package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.FlexAdapter;
import com.intellij.openapi.project.Project;

public class CsvLexerAdapter extends FlexAdapter {
    public CsvLexerAdapter(Project project) {
        super(new CsvLexer(null, project));
    }
}
