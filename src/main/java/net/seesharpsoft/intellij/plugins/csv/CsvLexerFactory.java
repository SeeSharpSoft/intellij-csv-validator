package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;

import static net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings.COMMENT_INDICATOR_DEFAULT;

public class CsvLexerFactory {
    protected static CsvLexerFactory INSTANCE = new CsvLexerFactory();

    public static CsvLexerFactory getInstance() {
        return INSTANCE;
    }

    protected Lexer createLexer(@NotNull CsvValueSeparator separator, @NotNull CsvEscapeCharacter escapeCharacter) {
        final String commentIndicator = CsvEditorSettings.getInstance().getCommentIndicator();
        if (separator.requiresCustomLexer() ||
                escapeCharacter.isCustom() ||
                (!commentIndicator.isEmpty() && !commentIndicator.equals(COMMENT_INDICATOR_DEFAULT))) {
            return new CsvSharpLexer(new CsvSharpLexer.Configuration(
                    separator.getCharacter(),
                    "\n",
                    escapeCharacter.getCharacter(),
                    "\"",
                    commentIndicator));
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
