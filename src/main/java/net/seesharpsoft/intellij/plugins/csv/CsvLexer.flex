package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import com.intellij.psi.TokenType;

%%

%class CsvLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

TEXT=[^ \t\f,\r\n\"]+
ESCAPED_TEXT=([,\r\n]|\"\")+
QUOTE=\"
COMMA=\,
EOL=\n
WHITE_SPACE=[ \t\f]+

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
    yybegin(YYINITIAL);
    return CsvTypes.COMMA;
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
