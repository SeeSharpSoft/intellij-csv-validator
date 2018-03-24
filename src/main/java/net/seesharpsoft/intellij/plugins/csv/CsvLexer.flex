package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.openapi.project.Project;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import static net.seesharpsoft.intellij.plugins.csv.formatter.CsvCodeStyleSettings.getCurrentSeparator;
import com.intellij.psi.TokenType;

%%

%class CsvLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%{
  private String currentSeparator;

  /**
   * Provide constructor that supports a Project as parameter.
   */
  CsvLexer(java.io.Reader in, String separator) {
    this(in);
    this.currentSeparator = separator;
  }
%}
%eof{  return;
%eof}

TEXT=[^ ,;|\t\r\n\"]+
ESCAPED_TEXT=([,;|\t\r\n]|\"\")+
QUOTE=\"
COMMA=[,;|\t]
EOL=\n
WHITE_SPACE=[ \f]+

%state AFTER_TEXT
%state ESCAPED_TEXT
%state UNESCAPED_TEXT

%%

<YYINITIAL> {QUOTE}
{
    yybegin(ESCAPED_TEXT);
    return CsvTypes.QUOTE;
}

<ESCAPED_TEXT> {QUOTE}
{
    yybegin(AFTER_TEXT);
    return CsvTypes.QUOTE;
}

<YYINITIAL> {TEXT}
{
    yybegin(UNESCAPED_TEXT);
    return CsvTypes.TEXT;
}

<UNESCAPED_TEXT, ESCAPED_TEXT> {TEXT}
{
    return CsvTypes.TEXT;
}

<ESCAPED_TEXT> {ESCAPED_TEXT}
{
    return CsvTypes.ESCAPED_TEXT;
}

<YYINITIAL, AFTER_TEXT, UNESCAPED_TEXT> {COMMA}
{
    if (currentSeparator.equals(yytext().toString())) {
        yybegin(YYINITIAL);
        return CsvTypes.COMMA;
    }
    if (yystate() != AFTER_TEXT) {
        yybegin(UNESCAPED_TEXT);
        return CsvTypes.TEXT;
    }
    return TokenType.BAD_CHARACTER;
}

<YYINITIAL, AFTER_TEXT, UNESCAPED_TEXT> {EOL}
{
    yybegin(YYINITIAL);
    return CsvTypes.CRLF;
}

{WHITE_SPACE}
{
    return TokenType.WHITE_SPACE;
}

.
{
    return TokenType.BAD_CHARACTER;
}
