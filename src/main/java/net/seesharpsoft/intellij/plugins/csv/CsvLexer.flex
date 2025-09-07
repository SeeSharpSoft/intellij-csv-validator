package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import com.intellij.lexer.FlexLexer;
import net.seesharpsoft.intellij.plugins.csv.components.CsvSeparatorHolder;

%%

%class CsvLexer
%implements FlexLexer
%implements CsvSeparatorHolder
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

    @Override
    public CsvValueSeparator getSeparator() {
        return myValueSeparator;
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

VALUE_SEPARATOR=[,:;|\t]
RECORD_SEPARATOR=\n
ESCAPED_QUOTE=\"\"|\\\"
OPENING_QUOTE=[ \f]*\"
CLOSING_QUOTE=\"[ \f]*
TEXT=[^,:;|\t\r\n\"\\#]+
BACKSLASH=\\+
HASHTAG=#
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

<YYINITIAL, UNQUOTED> {OPENING_QUOTE}
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

<UNQUOTED, QUOTED> {HASHTAG}
{
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

<QUOTED> {CLOSING_QUOTE}
{
    yybegin(UNQUOTED);
    return CsvTypes.QUOTE;
}

.
{
    return TokenType.BAD_CHARACTER;
}
