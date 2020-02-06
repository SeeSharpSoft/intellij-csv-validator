package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;

import java.util.regex.Pattern;

%%

%class CsvLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%{
    private CsvEditorSettings.ValueSeparator myValueSeparator;
    private CsvEditorSettings.EscapeCharacter myEscapeCharacter;

    private static final Pattern ESCAPE_TEXT_PATTERN = Pattern.compile("[,;|\\t\\r\\n]");

    /**
     * Provide constructor that supports a Project as parameter.
     */
    CsvLexer(java.io.Reader in, CsvEditorSettings.ValueSeparator valueSeparator, CsvEditorSettings.EscapeCharacter escapeCharacter) {
      this(in);
      myValueSeparator = valueSeparator;
      myEscapeCharacter = escapeCharacter;
    }
%}
%eof{  return;
%eof}

TEXT=[^ ,;|\t\r\n\"\\]+
ESCAPED_TEXT=[,;|\t\r\n\\]|\"\"|\\\"
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
    String text = yytext().toString();
    if (myEscapeCharacter.isEscapedQuote(text)
        || ESCAPE_TEXT_PATTERN.matcher(text).matches()
     ) {
        return CsvTypes.ESCAPED_TEXT;
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
