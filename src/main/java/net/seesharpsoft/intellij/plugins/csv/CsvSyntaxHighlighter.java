package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lang.Language;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.formatter.CsvCodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class CsvSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey COMMA =
            createTextAttributesKey("CSV_DEFAULT_COMMA", DefaultLanguageHighlighterColors.COMMA);
    public static final TextAttributesKey QUOTE =
            createTextAttributesKey("CSV_DEFAULT_QUOTE", DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey TEXT =
            createTextAttributesKey("CSV_DEFAULT_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey ESCAPED_TEXT =
            createTextAttributesKey("CSV_ESCAPED_STRING", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
    public static final TextAttributesKey BAD_CHARACTER =
            createTextAttributesKey("CSV_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
    private static final TextAttributesKey[] COMMA_KEYS = new TextAttributesKey[]{COMMA};
    private static final TextAttributesKey[] QUOTE_KEYS = new TextAttributesKey[]{QUOTE};
    private static final TextAttributesKey[] TEXT_KEYS = new TextAttributesKey[]{TEXT};
    private static final TextAttributesKey[] ESCAPED_TEXT_KEYS = new TextAttributesKey[]{ESCAPED_TEXT};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    private Project project;
    private Language language;

    public CsvSyntaxHighlighter(Project project, VirtualFile virtualFile) {
        this.project = project;
        if (virtualFile != null && virtualFile.getFileType() instanceof LanguageFileType) {
            this.language = ((LanguageFileType)virtualFile.getFileType()).getLanguage();
        }
    }

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new CsvLexerAdapter(CsvCodeStyleSettings.getCurrentSeparator(this.project, this.language));
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(CsvTypes.COMMA)) {
            return COMMA_KEYS;
        } else if (tokenType.equals(CsvTypes.QUOTE)) {
            return QUOTE_KEYS;
        } else if (tokenType.equals(CsvTypes.TEXT)) {
            return TEXT_KEYS;
        } else if (tokenType.equals(CsvTypes.ESCAPED_TEXT)) {
            return ESCAPED_TEXT_KEYS;
        } else if (tokenType.equals(TokenType.BAD_CHARACTER)) {
            return BAD_CHAR_KEYS;
        } else {
            return EMPTY_KEYS;
        }
    }
}