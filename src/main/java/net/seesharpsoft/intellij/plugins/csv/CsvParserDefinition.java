package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import net.seesharpsoft.intellij.lang.FileParserDefinition;
import net.seesharpsoft.intellij.plugins.csv.parser.CsvParser;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFileElementType;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;

public class CsvParserDefinition implements FileParserDefinition {
    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);

    public static final TokenSet COMMENTS = TokenSet.create(CsvTypes.COMMENT);

    public static final TokenSet STRING_LITERALS = TokenSet.create(CsvTypes.TEXT, CsvTypes.ESCAPED_TEXT);

    public static final IFileElementType FILE = new CsvFileElementType(CsvLanguage.INSTANCE);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        throw new UnsupportedOperationException("use 'createLexer(PsiFile file)' instead");
    }

    @Override
    @NotNull
    public TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @Override
    @NotNull
    public TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @Override
    @NotNull
    public TokenSet getStringLiteralElements() {
        return STRING_LITERALS;
    }

    @Override
    @NotNull
    public PsiParser createParser(final Project project) {
        return new CsvParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new CsvFile(viewProvider, CsvFileType.INSTANCE);
    }

    @Override
    @NotNull
    public PsiElement createElement(ASTNode node) {
        return CsvTypes.Factory.createElement(node);
    }

    @Override
    public Lexer createLexer(@NotNull PsiFile file) {
        return CsvLexerFactory.getInstance().createLexer(file);
    }

    @Override
    public PsiParser createParser(@NotNull PsiFile file) {
        return createParser(file.getProject());
    }
}
