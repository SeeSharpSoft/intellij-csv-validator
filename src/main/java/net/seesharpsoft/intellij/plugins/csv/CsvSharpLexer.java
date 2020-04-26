package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.UnhandledSwitchCaseException;
import net.seesharpsoft.commons.util.Tokenizer;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CsvSharpLexer extends LexerBase {

    private final Tokenizer<TokenType> tokenizer;
    private final List<Tokenizer.Token<TokenType>> unquotedNextStateTokens;
    private final List<Tokenizer.Token<TokenType>> quotedNextStateTokens;

    private CharSequence buffer;
    private int bufferEnd;
    private int tokenStart;
    private int tokenEnd;
    private LexerState currentState;
    private IElementType currentTokenType;
    private boolean failed;

    private static final Map<TokenType, LexerState> UNQUOTED_NEXT_STATES = new HashMap<>();
    private static final Map<TokenType, LexerState> QUOTED_NEXT_STATES = new HashMap<>();

    static {
        UNQUOTED_NEXT_STATES.put(TokenType.WHITESPACE, LexerState.Unquoted);
        UNQUOTED_NEXT_STATES.put(TokenType.TEXT, LexerState.Unquoted);
        UNQUOTED_NEXT_STATES.put(TokenType.VALUE_SEPARATOR, LexerState.Unquoted);
        UNQUOTED_NEXT_STATES.put(TokenType.RECORD_SEPARATOR, LexerState.Unquoted);
        UNQUOTED_NEXT_STATES.put(TokenType.BEGIN_QUOTE, LexerState.Quoted);

        QUOTED_NEXT_STATES.put(TokenType.WHITESPACE, LexerState.Quoted);
        QUOTED_NEXT_STATES.put(TokenType.TEXT, LexerState.Quoted);
        QUOTED_NEXT_STATES.put(TokenType.ESCAPED_CHARACTER, LexerState.Quoted);
        QUOTED_NEXT_STATES.put(TokenType.END_QUOTE, LexerState.Unquoted);
    }

    enum LexerState {
        Unquoted(UNQUOTED_NEXT_STATES),
        Quoted(QUOTED_NEXT_STATES);

        private final Map<TokenType, LexerState> stateMap;

        LexerState(Map<TokenType, LexerState> stateMap) {
            this.stateMap = stateMap;
        }

        public Collection<TokenType> getPossibleTokens() {
            return stateMap.keySet();
        }

        public LexerState getNextState(TokenType tokenType) {
            return stateMap.get(tokenType);
        }
    }

    enum TokenType {
        BEGIN_QUOTE,
        END_QUOTE,
        TEXT,
        ESCAPED_CHARACTER,
        VALUE_SEPARATOR,
        RECORD_SEPARATOR,
        WHITESPACE
    }

    public static class Configuration {
        public static final Configuration DEFAULT = new Configuration(",", "\n", "\"", "\"");

        public String valueSeparator;
        public String recordSeparator;
        public String escapeCharacter;
        public String quoteCharacter;

        public Configuration(String valueSeparator, String recordSeparator, String escapeCharacter, String quoteCharacter) {
            this.valueSeparator = Pattern.quote(valueSeparator);
            this.recordSeparator = Pattern.quote(recordSeparator);
            this.escapeCharacter = Pattern.quote(escapeCharacter);
            this.quoteCharacter = Pattern.quote(quoteCharacter);
        }
    }

    public CsvSharpLexer() {
        this(Configuration.DEFAULT);
    }

    public CsvSharpLexer(Configuration configuration) {
        super();

        tokenizer = new Tokenizer<>();
        tokenizer.add(TokenType.WHITESPACE, "[ \f]+");
        tokenizer.add(TokenType.BEGIN_QUOTE, String.format("%s", configuration.quoteCharacter));
        tokenizer.add(TokenType.VALUE_SEPARATOR, configuration.valueSeparator);
        tokenizer.add(TokenType.RECORD_SEPARATOR, configuration.recordSeparator);

        if (configuration.escapeCharacter.equals(configuration.quoteCharacter)) {
            tokenizer.add(TokenType.END_QUOTE, String.format("%s(?!%s)", configuration.quoteCharacter, configuration.quoteCharacter));
            tokenizer.add(TokenType.ESCAPED_CHARACTER, String.format("(%s%s|%s|%s)+", configuration.quoteCharacter, configuration.quoteCharacter, configuration.valueSeparator, configuration.recordSeparator));
            tokenizer.add(TokenType.TEXT, String.format("[^ \f%s%s%s]+",configuration.quoteCharacter, configuration.valueSeparator, configuration.recordSeparator));
        } else {
            tokenizer.add(TokenType.END_QUOTE, String.format("%s", configuration.quoteCharacter));
            tokenizer.add(TokenType.ESCAPED_CHARACTER, String.format("(%s%s|%s%s|%s|%s)+", configuration.escapeCharacter, configuration.quoteCharacter, configuration.escapeCharacter, configuration.escapeCharacter, configuration.valueSeparator, configuration.recordSeparator));
            tokenizer.add(TokenType.TEXT, String.format("[^ \f%s%s%s%s]+", configuration.escapeCharacter, configuration.quoteCharacter, configuration.valueSeparator, configuration.recordSeparator));
        }

        unquotedNextStateTokens = LexerState.Unquoted.getPossibleTokens().stream()
                .map(tokenType -> tokenizer.getToken(tokenType))
                .collect(Collectors.toList());
        quotedNextStateTokens = LexerState.Quoted.getPossibleTokens().stream()
                .map(tokenType -> tokenizer.getToken(tokenType))
                .collect(Collectors.toList());
    }

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.tokenStart = this.tokenEnd = startOffset;
        this.bufferEnd = endOffset;
        this.currentState = initialState == 0 ? LexerState.Unquoted : LexerState.Quoted;
        this.currentTokenType = null;
    }

    @Override
    public int getState() {
        locateToken();
        return currentState == LexerState.Unquoted ? 0 : 1;
    }

    @Nullable
    @Override
    public IElementType getTokenType() {
        locateToken();
        return currentTokenType;
    }

    @Override
    public int getTokenStart() {
        locateToken();
        return this.tokenStart;
    }

    @Override
    public int getTokenEnd() {
        locateToken();
        return this.tokenEnd;
    }

    @Override
    public void advance() {
        locateToken();
        this.currentTokenType = null;
    }

    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return this.bufferEnd;
    }

    protected void raiseFailure() {
        failed = true;
        currentTokenType = com.intellij.psi.TokenType.BAD_CHARACTER;
        tokenEnd = bufferEnd;
    }

    protected synchronized void locateToken() {
        if (currentTokenType != null) {
            return;
        }

        this.tokenStart = this.tokenEnd;
        if (failed) {
            return;
        }

        try {
            Tokenizer.TokenInfo<TokenType> tokenInfo =
                    tokenizer.findToken(buffer,
                            tokenStart,
                            bufferEnd,
                            currentState == LexerState.Unquoted ? unquotedNextStateTokens : quotedNextStateTokens,
                            null
                    );

            if (tokenInfo == null) {
                if (tokenStart < bufferEnd) {
                    raiseFailure();
                }
                return;
            }

            this.currentState = currentState.getNextState(tokenInfo.token());
            this.tokenEnd = tokenInfo.textRange().end();

            switch(tokenInfo.token()) {
                case BEGIN_QUOTE:
                case END_QUOTE:
                    currentTokenType = CsvTypes.QUOTE;
                    break;
                case RECORD_SEPARATOR:
                    currentTokenType = CsvTypes.CRLF;
                    break;
                case ESCAPED_CHARACTER:
                    currentTokenType = CsvTypes.ESCAPED_TEXT;
                    break;
                case VALUE_SEPARATOR:
                    currentTokenType = CsvTypes.COMMA;
                    break;
                case TEXT:
                    currentTokenType = CsvTypes.TEXT;
                    break;
                case WHITESPACE:
                    currentTokenType = com.intellij.psi.TokenType.WHITE_SPACE;
                    break;
                default:
                    throw new UnhandledSwitchCaseException(tokenInfo.token());
            }
        } catch (Throwable e) {
            raiseFailure();
        }
    }
}
