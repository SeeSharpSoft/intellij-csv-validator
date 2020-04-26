package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class CsvLexerFactory {
    protected static CsvLexerFactory INSTANCE = new CsvLexerFactory();

    public static CsvLexerFactory getInstance() {
        return INSTANCE;
    }

    protected Lexer createLexer(@NotNull CsvValueSeparator separator, @NotNull CsvEscapeCharacter escapeCharacter) {
        if (separator.isCustom()) {
            return new CsvSharpLexer(new CsvSharpLexer.Configuration(
                    separator.getCharacter(),
                    "\n",
                    escapeCharacter.getCharacter(),
                    "\""));
        }
        return new CsvLexerAdapter(separator, escapeCharacter);
    }

    public Lexer createLexer(Project project, VirtualFile file) {
        return createLexer(CsvHelper.getValueSeparator(project, file), CsvHelper.getEscapeCharacter(project, file));
    }

    public Lexer createLexer(@NotNull PsiFile file) {
        return createLexer(CsvHelper.getValueSeparator(file), CsvHelper.getEscapeCharacter(file));
    }
}
