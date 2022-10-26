package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.lexer.DelegateLexer;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvHighlightingLexer extends DelegateLexer {

    private int myCurrentColumnIndex = 0;

    public CsvHighlightingLexer(@NotNull Lexer delegate) {
        super(delegate);
    }

    @NotNull
    @Override
    public LexerPosition getCurrentPosition() {
        return new LexerPositionWithColumnIndex(getTokenStart(), getState(), myCurrentColumnIndex);
    }

    @Override
    public void restore(@NotNull LexerPosition position) {
        myCurrentColumnIndex = ((LexerPositionWithColumnIndex)position).getColumnIndex();
        super.start(getBufferSequence(), position.getOffset(), getBufferEnd(), position.getState());
    }

    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        myCurrentColumnIndex = 0;
        super.start(buffer, startOffset, endOffset, initialState);
    }

    @Override
    @Nullable
    public IElementType getTokenType() {
        IElementType tokenType = super.getTokenType();

        if (tokenType == null) return null;

        if (TokenType.BAD_CHARACTER.equals(tokenType)) {
            return CsvHighlightingElement.TokenBased.BAD_CHARACTER;
        } else if (CsvTypes.COMMA.equals(tokenType)) {
            return CsvHighlightingElement.TokenBased.COMMA;
        } else if (CsvTypes.COMMENT.equals(tokenType)) {
            return CsvHighlightingElement.TokenBased.COMMENT;
        } else if (CsvTypes.QUOTE.equals(tokenType)) {
            return CsvHighlightingElement.TokenBased.QUOTE;
        }

        return CsvEditorSettings.getInstance().getValueColoring() == CsvEditorSettings.ValueColoring.RAINBOW ?
                CsvHighlightingElement.ColumnBased.forIndex(myCurrentColumnIndex) :
                CsvHighlightingElement.TokenBased.TEXT;
    }

    @Override
    public void advance() {
        IElementType tokenType = super.getTokenType();

        if (CsvTypes.COMMA.equals(tokenType)) {
            myCurrentColumnIndex++;
        } else if (CsvTypes.CRLF.equals(tokenType)) {
            myCurrentColumnIndex = 0;
        }

        super.advance();
    }

    private static class LexerPositionWithColumnIndex implements LexerPosition {
        private final int myOffset;
        private final int myState;
        private final int myColumnIndex;

        LexerPositionWithColumnIndex(final int offset, final int state, final int columnIndex) {
            myOffset = offset;
            myState = state;
            myColumnIndex = columnIndex;
        }

        @Override
        public int getOffset() {
            return myOffset;
        }

        @Override
        public int getState() {
            return myState;
        }

        public int getColumnIndex() {
            return myColumnIndex;
        }
    }
}
