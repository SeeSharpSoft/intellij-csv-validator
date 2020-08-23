package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.UnhandledSwitchCaseException;
import net.seesharpsoft.commons.util.Tokenizer;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CsvSharpLexer extends LexerBase {

    private final static String NON_MATCHING_REGEX = "[^\\w\\W]";

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

    private static final Map<TokenType, LexerState> INITIAL_NEXT_STATES = new HashMap<>();
    private static final Map<TokenType, LexerState> UNQUOTED_NEXT_STATES = new HashMap<>();
    private static final Map<TokenType, LexerState> QUOTED_NEXT_STATES = new HashMap<>();

    static {
        INITIAL_NEXT_STATES.put(TokenType.WHITESPACE, LexerState.Initial);
        INITIAL_NEXT_STATES.put(TokenType.TEXT, LexerState.Unquoted);
        INITIAL_NEXT_STATES.put(TokenType.VALUE_SEPARATOR, LexerState.Unquoted);
        INITIAL_NEXT_STATES.put(TokenType.BEGIN_QUOTE, LexerState.Quoted);
        INITIAL_NEXT_STATES.put(TokenType.RECORD_SEPARATOR, LexerState.Initial);
        INITIAL_NEXT_STATES.put(TokenType.COMMENT, LexerState.Initial);

        UNQUOTED_NEXT_STATES.put(TokenType.WHITESPACE, LexerState.Unquoted);
        UNQUOTED_NEXT_STATES.put(TokenType.TEXT, LexerState.Unquoted);
        UNQUOTED_NEXT_STATES.put(TokenType.COMMENT_CHARACTER, LexerState.Unquoted);
        UNQUOTED_NEXT_STATES.put(TokenType.VALUE_SEPARATOR, LexerState.Unquoted);
        UNQUOTED_NEXT_STATES.put(TokenType.BEGIN_QUOTE, LexerState.Quoted);
        UNQUOTED_NEXT_STATES.put(TokenType.RECORD_SEPARATOR, LexerState.Initial);

        QUOTED_NEXT_STATES.put(TokenType.WHITESPACE, LexerState.Quoted);
        QUOTED_NEXT_STATES.put(TokenType.TEXT, LexerState.Quoted);
        QUOTED_NEXT_STATES.put(TokenType.COMMENT_CHARACTER, LexerState.Quoted);
        QUOTED_NEXT_STATES.put(TokenType.ESCAPED_CHARACTER, LexerState.Quoted);
        QUOTED_NEXT_STATES.put(TokenType.END_QUOTE, LexerState.Unquoted);
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
        BEGIN_QUOTE,
        END_QUOTE,
        TEXT,
        ESCAPED_CHARACTER,
        VALUE_SEPARATOR,
        RECORD_SEPARATOR,
        WHITESPACE,
        COMMENT,
        COMMENT_CHARACTER
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
            this.commentCharacter = Pattern.quote(commentCharacter);
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
        if (!configuration.commentCharacter.isEmpty()) {
            tokenizer.add(TokenType.COMMENT_CHARACTER, configuration.commentCharacter);
            tokenizer.add(TokenType.COMMENT, configuration.commentCharacter + ".*(?=(\n|$))");
        } else {
            tokenizer.add(TokenType.COMMENT_CHARACTER, NON_MATCHING_REGEX);
            tokenizer.add(TokenType.COMMENT, NON_MATCHING_REGEX);
        }

        if (configuration.escapeCharacter.equals(configuration.quoteCharacter)) {
            tokenizer.add(TokenType.END_QUOTE, String.format("%s(?!%s)", configuration.quoteCharacter, configuration.quoteCharacter));
            tokenizer.add(TokenType.ESCAPED_CHARACTER, String.format("(%s%s|%s|%s)+", configuration.quoteCharacter, configuration.quoteCharacter, configuration.valueSeparator, configuration.recordSeparator));
            if (!configuration.commentCharacter.isEmpty()) {
                tokenizer.add(TokenType.TEXT, String.format("((?!(%s|%s))[^ \f%s%s])+", configuration.commentCharacter, configuration.valueSeparator, configuration.quoteCharacter, configuration.recordSeparator));
            } else {
                tokenizer.add(TokenType.TEXT, String.format("((?!%s)[^ \f%s%s])+", configuration.valueSeparator, configuration.quoteCharacter, configuration.recordSeparator));
            }
        } else {
            tokenizer.add(TokenType.END_QUOTE, String.format("%s", configuration.quoteCharacter));
            tokenizer.add(TokenType.ESCAPED_CHARACTER, String.format("(%s%s|%s%s|%s|%s)+", configuration.escapeCharacter, configuration.quoteCharacter, configuration.escapeCharacter, configuration.escapeCharacter, configuration.valueSeparator, configuration.recordSeparator));
            if (!configuration.commentCharacter.isEmpty()) {
                tokenizer.add(TokenType.TEXT, String.format("((?!(%s|%s))[^ \f%s%s%s])+", configuration.commentCharacter, configuration.valueSeparator, configuration.escapeCharacter, configuration.quoteCharacter, configuration.recordSeparator));
            } else {
                tokenizer.add(TokenType.TEXT, String.format("((?!%s)[^ \f%s%s%s])+", configuration.valueSeparator, configuration.escapeCharacter, configuration.quoteCharacter, configuration.recordSeparator));
            }
        }

        initialNextStateTokens = LexerState.Initial.getPossibleTokens().stream()
                .map(tokenizer::getToken)
                .collect(Collectors.toList());
        unquotedNextStateTokens = LexerState.Unquoted.getPossibleTokens().stream()
                .map(tokenizer::getToken)
                .collect(Collectors.toList());
        quotedNextStateTokens = LexerState.Quoted.getPossibleTokens().stream()
                .map(tokenizer::getToken)
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
                case COMMENT_CHARACTER:
                    currentTokenType = CsvTypes.TEXT;
                    break;
                case COMMENT:
                    currentTokenType = CsvTypes.COMMENT;
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
