package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import com.intellij.psi.TokenType;
import com.intellij.lexer.FlexLexer;import org.intellij.grammar.livePreview.LivePreviewElementType;

import java.util.regex.Pattern;

%%

%class CsvLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%{
    private CsvValueSeparator myValueSeparator;
    private CsvEscapeCharacter myEscapeCharacter;

    private static final Pattern ESCAPE_TEXT_PATTERN = Pattern.compile("[,;|\\t\\r\\n]");

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
ESCAPED_TEXT=[,:;|\t\r\n]|\"\"|\\\"
ESCAPE_CHAR=\\
QUOTE=\"
COMMA=[,:;|\t]
EOL=\n
WHITE_SPACE=[ \f]+

%state AFTER_TEXT
%state ESCAPED_TEXT
%state UNESCAPED_TEXT
%state ESCAPING

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

<YYINITIAL, UNESCAPED_TEXT> {ESCAPE_CHAR}
{
    String text = yytext().toString();
    if (myEscapeCharacter.getCharacter().equals(text)) {
        return TokenType.BAD_CHARACTER;
    }
    yybegin(UNESCAPED_TEXT);
    return CsvTypes.TEXT;
}

<ESCAPED_TEXT, ESCAPING> {ESCAPE_CHAR} {
    String text = yytext().toString();
    if (myEscapeCharacter.getCharacter().equals(text)) {
        switch (yystate()) {
            case ESCAPED_TEXT:
                yybegin(ESCAPING);
                break;
            case ESCAPING:
                yybegin(ESCAPED_TEXT);
                break;
            default:
                throw new RuntimeException("unhandled state: " + yystate());
        }
        return CsvTypes.ESCAPED_TEXT;
    }
    return CsvTypes.TEXT;
}

<ESCAPED_TEXT> {ESCAPED_TEXT}
{
    String text = yytext().toString();
    if (myEscapeCharacter.isEscapedQuote(text)
        || ESCAPE_TEXT_PATTERN.matcher(text).matches()
     ) {
        return CsvTypes.ESCAPED_TEXT;
    }
    if (!text.startsWith(CsvEscapeCharacter.QUOTE.getCharacter())) {
        yypushback(1);
        return CsvTypes.TEXT;
    }

    return TokenType.BAD_CHARACTER;
}

<YYINITIAL, AFTER_TEXT, UNESCAPED_TEXT> {COMMA}
{
    if (myValueSeparator.isValueSeparator(yytext().toString())) {
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
