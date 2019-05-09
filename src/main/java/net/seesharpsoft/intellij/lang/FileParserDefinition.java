package net.seesharpsoft.intellij.lang;

import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.psi.PsiFile;

/**
 * Support for file specific parser definition (even though .
 */
public interface FileParserDefinition extends ParserDefinition {
    default Lexer createLexer(PsiFile file) {
        return createLexer(file.getProject());
    }

    default PsiParser createParser(PsiFile file) {
        return createParser(file.getProject());
    }
}
