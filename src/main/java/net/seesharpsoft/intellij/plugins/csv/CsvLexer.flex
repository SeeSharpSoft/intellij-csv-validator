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
    private boolean mySupportComments;

    private boolean isActualValueSeparator() {
        return myValueSeparator.isValueSeparator(yycharat(0));
    }

    /**
     * Provide constructor that support parameters to customize lexer.
     */
    CsvLexer(java.io.Reader in, CsvValueSeparator valueSeparator, CsvEscapeCharacter escapeCharacter, boolean supportComments) {
      this(in);
      myValueSeparator = valueSeparator;
      myEscapeCharacter = escapeCharacter;
      mySupportComments = supportComments;
    }
%}
%eof{  return;
%eof}

WHITE_SPACE=[ \f]+
VALUE_SEPARATOR=[,:;|\t]
RECORD_SEPARATOR=\n
ESCAPED_QUOTE=\"\"|\\\"
QUOTE=\"
TEXT=[^ ,:;|\t\r\n\"\\]+
BACKSLASH=\\+
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
    if (mySupportComments) {
        return CsvTypes.COMMENT;
    }
    yypushback(yylength() - 1);
    yybegin(UNQUOTED);
    return CsvTypes.TEXT;
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
