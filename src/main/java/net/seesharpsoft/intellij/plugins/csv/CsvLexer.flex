package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import com.intellij.psi.TokenType;
import com.intellij.lexer.FlexLexer;

%%

%class CsvLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%{
    private CsvValueSeparator myValueSeparator;
    private CsvEscapeCharacter myEscapeCharacter;

    private boolean isActualValueSeparator() {
        return myValueSeparator.isValueSeparator(yytext().toString());
    }

    /**
     * Provide constructor that supports a Project as parameter.
     */
    CsvLexer(java.io.Reader in, CsvValueSeparator valueSeparator, CsvEscapeCharacter escapeCharacter) {
      this(in);
      myValueSeparator = valueSeparator;
      myEscapeCharacter = escapeCharacter;
    }
%}
%eof{  return;
%eof}

TEXT=[^ ,:;|\t\r\n\"\\]+
ESCAPED_QUOTE=\"\"|\\\"
BACKSLASH=\\+
QUOTE=\"
VALUE_SEPARATOR=[,:;|\t]
RECORD_SEPARATOR=\n
WHITE_SPACE=[ \f]+
COMMENT=\#[^\n]*

%state UNQUOTED
%state QUOTED

%%

<YYINITIAL, UNQUOTED> {TEXT}
{
    yybegin(UNQUOTED);
    return CsvTypes.TEXT;
}

<YYINITIAL, UNQUOTED> {BACKSLASH}
{
    yybegin(UNQUOTED);
    return CsvTypes.TEXT;
}

<YYINITIAL, UNQUOTED> {VALUE_SEPARATOR}
{
    yybegin(UNQUOTED);
    if (isActualValueSeparator()) {
        return CsvTypes.COMMA;
    }
    return CsvTypes.TEXT;
}

<YYINITIAL, UNQUOTED> {QUOTE}
{
    yybegin(QUOTED);
    return CsvTypes.QUOTE;
}

<YYINITIAL, UNQUOTED> {RECORD_SEPARATOR}
{
    yybegin(YYINITIAL);
    return CsvTypes.CRLF;
}

<YYINITIAL> {COMMENT}
{
    return CsvTypes.COMMENT;
}

<QUOTED> {TEXT}
{
    return CsvTypes.TEXT;
}

<QUOTED> {BACKSLASH}
{
    if (myEscapeCharacter == CsvEscapeCharacter.BACKSLASH) {
        int backslashCount = yylength();
        if (backslashCount > 1 && (backslashCount % 2 != 0)) {
            yypushback(1);
        }
    }
    return CsvTypes.TEXT;
}

<QUOTED> {RECORD_SEPARATOR}
{
    return CsvTypes.ESCAPED_TEXT;
}

<QUOTED> {VALUE_SEPARATOR}
{
    if (isActualValueSeparator()) {
        return CsvTypes.ESCAPED_TEXT;
    }
    return CsvTypes.TEXT;
}

<QUOTED> {ESCAPED_QUOTE}
{
    String text = yytext().toString();
    if (!myEscapeCharacter.isEscapedQuote(text)) {
        yypushback(1);
        return CsvTypes.TEXT;
    }
    return CsvTypes.ESCAPED_TEXT;
}

<QUOTED> {QUOTE}
{
    yybegin(UNQUOTED);
    return CsvTypes.QUOTE;
}

{WHITE_SPACE}
{
    return TokenType.WHITE_SPACE;
}

.
{
    return TokenType.BAD_CHARACTER;
}
