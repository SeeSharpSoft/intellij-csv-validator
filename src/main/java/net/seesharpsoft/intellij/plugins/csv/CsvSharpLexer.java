package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.UnhandledSwitchCaseException;
import net.seesharpsoft.commons.util.Tokenizer;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CsvSharpLexer extends LexerBase {

    private final Tokenizer<TokenType> tokenizer;
    private final List<Tokenizer.Token<TokenType>> initialNextStateTokens;
    private final List<Tokenizer.Token<TokenType>> unquotedNextStateTokens;
    private final List<Tokenizer.Token<TokenType>> quotedNextStateTokens;

    private CharSequence buffer;
    private int bufferEnd;
    private int tokenStart;
    private int tokenEnd;
    private LexerState currentState;
    private IElementType currentTokenType;
    private boolean failed;

    private static final Map<TokenType, LexerState> INITIAL_NEXT_STATES = new LinkedHashMap<>();
    private static final Map<TokenType, LexerState> UNQUOTED_NEXT_STATES = new LinkedHashMap<>();
    private static final Map<TokenType, LexerState> QUOTED_NEXT_STATES = new LinkedHashMap<>();

    static {
        INITIAL_NEXT_STATES.put(TokenType.COMMENT, LexerState.Initial);
        INITIAL_NEXT_STATES.put(TokenType.VALUE_SEPARATOR, LexerState.Unquoted);
        INITIAL_NEXT_STATES.put(TokenType.RECORD_SEPARATOR, LexerState.Initial);
        INITIAL_NEXT_STATES.put(TokenType.OPENING_QUOTE, LexerState.Quoted);
        INITIAL_NEXT_STATES.put(TokenType.TEXT, LexerState.Unquoted);
        INITIAL_NEXT_STATES.put(TokenType.BACKSLASH, LexerState.Unquoted);

        UNQUOTED_NEXT_STATES.put(TokenType.VALUE_SEPARATOR, LexerState.Unquoted);
        UNQUOTED_NEXT_STATES.put(TokenType.RECORD_SEPARATOR, LexerState.Initial);
        UNQUOTED_NEXT_STATES.put(TokenType.OPENING_QUOTE, LexerState.Quoted);
        UNQUOTED_NEXT_STATES.put(TokenType.TEXT, LexerState.Unquoted);
        UNQUOTED_NEXT_STATES.put(TokenType.BACKSLASH, LexerState.Unquoted);

        QUOTED_NEXT_STATES.put(TokenType.RECORD_SEPARATOR, LexerState.Quoted);
        QUOTED_NEXT_STATES.put(TokenType.VALUE_SEPARATOR, LexerState.Quoted);
        QUOTED_NEXT_STATES.put(TokenType.ESCAPED_QUOTE, LexerState.Quoted);
        QUOTED_NEXT_STATES.put(TokenType.CLOSING_QUOTE, LexerState.Unquoted);
        QUOTED_NEXT_STATES.put(TokenType.TEXT, LexerState.Quoted);
        QUOTED_NEXT_STATES.put(TokenType.BACKSLASH, LexerState.Quoted);
    }

    enum LexerState {
        Initial(INITIAL_NEXT_STATES),
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
        BACKSLASH,
        OPENING_QUOTE,
        CLOSING_QUOTE,
        TEXT,
        ESCAPED_QUOTE,
        VALUE_SEPARATOR,
        RECORD_SEPARATOR,
        COMMENT
    }

    public static class Configuration {
        public static final Configuration DEFAULT = new Configuration(",", "\n", "\"", "\"", "#");

        public String valueSeparator;
        public String recordSeparator;
        public String escapeCharacter;
        public String quoteCharacter;
        public String commentCharacter;

        public Configuration(String valueSeparator, String recordSeparator, String escapeCharacter, String quoteCharacter, String commentCharacter) {
            this.valueSeparator = Pattern.quote(valueSeparator);
            this.recordSeparator = Pattern.quote(recordSeparator);
            this.escapeCharacter = Pattern.quote(escapeCharacter);
            this.quoteCharacter = Pattern.quote(quoteCharacter);
            this.commentCharacter = commentCharacter.isEmpty() ? "" : Pattern.quote(commentCharacter);
        }
    }

    public CsvSharpLexer() {
        this(Configuration.DEFAULT);
    }

    public CsvSharpLexer(Configuration configuration) {
        super();

        tokenizer = new Tokenizer<>();
        tokenizer.add(TokenType.VALUE_SEPARATOR, configuration.valueSeparator);
        tokenizer.add(TokenType.RECORD_SEPARATOR, configuration.recordSeparator);
        tokenizer.add(TokenType.ESCAPED_QUOTE, String.format("%s%s", configuration.escapeCharacter, configuration.quoteCharacter));
        tokenizer.add(TokenType.OPENING_QUOTE, String.format("[ \f]*%s", configuration.quoteCharacter));
        tokenizer.add(TokenType.CLOSING_QUOTE, String.format("%s[ \f]*", configuration.quoteCharacter));
        if (configuration.escapeCharacter.equals(configuration.quoteCharacter)) {
            tokenizer.add(TokenType.TEXT, String.format("((?!%s)[^%s%s])+", configuration.valueSeparator, configuration.quoteCharacter, configuration.recordSeparator));
        } else {
            tokenizer.add(TokenType.TEXT,
                    String.format("((?!%s)[^%s%s%s])+|%s%s",
                    configuration.valueSeparator,
                    configuration.escapeCharacter,
                    configuration.quoteCharacter,
                    configuration.recordSeparator,
                    configuration.escapeCharacter,
                    configuration.escapeCharacter));
            tokenizer.add(TokenType.BACKSLASH, String.format("%s", configuration.escapeCharacter));
        }
        if (!configuration.commentCharacter.isEmpty()) {
            tokenizer.add(TokenType.COMMENT, configuration.commentCharacter + "[^\\n]*");
        }

        initialNextStateTokens = LexerState.Initial.getPossibleTokens().stream()
                .map(tokenizer::getToken)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        unquotedNextStateTokens = LexerState.Unquoted.getPossibleTokens().stream()
                .map(tokenizer::getToken)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        quotedNextStateTokens = LexerState.Quoted.getPossibleTokens().stream()
                .map(tokenizer::getToken)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.tokenStart = this.tokenEnd = startOffset;
        this.bufferEnd = endOffset;
        this.currentState = LexerState.values()[initialState];
        this.currentTokenType = null;
    }

    @Override
    public int getState() {
        locateToken();
        return currentState.ordinal();
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

    protected Collection<Tokenizer.Token<TokenType>> getCurrentTokenCollection() {
        switch(this.currentState) {
            case Initial:
                return initialNextStateTokens;
            case Unquoted:
                return unquotedNextStateTokens;
            case Quoted:
                return quotedNextStateTokens;
            default:
                throw new UnhandledSwitchCaseException(this.currentState);
        }
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
                            getCurrentTokenCollection(),
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
                case OPENING_QUOTE:
                case CLOSING_QUOTE:_QUOTE:
                    currentTokenType = CsvTypes.QUOTE;
                    break;
                case ESCAPED_QUOTE:
                    currentTokenType = CsvTypes.ESCAPED_TEXT;
                    break;
                case RECORD_SEPARATOR:
                    currentTokenType = currentState == LexerState.Quoted ? CsvTypes.ESCAPED_TEXT : CsvTypes.CRLF;
                    break;
                case VALUE_SEPARATOR:
                    currentTokenType = currentState == LexerState.Quoted ? CsvTypes.ESCAPED_TEXT : CsvTypes.COMMA;
                    break;
                case TEXT:
                case BACKSLASH:
                    currentTokenType = CsvTypes.TEXT;
                    break;
                case COMMENT:
                    currentTokenType = CsvTypes.COMMENT;
                    break;
                default:
                    throw new UnhandledSwitchCaseException(tokenInfo.token());
            }
        } catch (Throwable e) {
            raiseFailure();
        }
    }
}
