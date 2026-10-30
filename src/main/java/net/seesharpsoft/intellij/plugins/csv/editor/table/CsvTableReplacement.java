package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvLexerFactory;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableSearchSessionBase.Match;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Prepares one document edit in source order, without looking up cells or parsing replacement PSI. */
final class CsvTableReplacement {
    private final CharSequence snapshot;
    private final Map<Cell, List<Match>> targets;
    private final int[] rows;
    private final int[] columns;
    private final FindModel query;
    private final FindManager manager;
    private final Settings settings;
    private final StringBuilder value = new StringBuilder();
    private final StringBuilder result = new StringBuilder();
    private int row;
    private int column;
    private int fieldStart;
    private int changeStart = -1;
    private int changeEnd;
    private boolean comment;

    record Change(TextRange range, String text) {
    }

    record Settings(CsvValueSeparator separator, CsvEscapeCharacter escape, String comment, boolean quoting) {
        static Settings capture(PsiFile file) {
            CsvEditorSettings settings = CsvEditorSettings.getInstance();
            return new Settings(CsvHelper.getValueSeparator(file), CsvHelper.getEscapeCharacter(file),
                    settings.getCommentIndicator(), settings.isQuotingEnforced());
        }

        boolean isCurrent(PsiFile file) {
            Settings current = capture(file);
            return separator.getCharacter().equals(current.separator.getCharacter())
                    && escape.getCharacter().equals(current.escape.getCharacter())
                    && comment.equals(current.comment) && quoting == current.quoting;
        }

        private String serialize(String value, boolean isComment, int column) {
            if (isComment) {
                String text = comment + value;
                Lexer lexer = CsvLexerFactory.getInstance().createLexer(separator, escape, comment);
                lexer.start(text);
                while (lexer.getTokenType() != null) {
                    ProgressManager.checkCanceled();
                    if (lexer.getTokenType() == CsvTypes.COMMENT) return lexer.getTokenText();
                    lexer.advance();
                }
                return null;
            }
            return CsvHelper.quoteCsvField(value, escape, separator,
                    quoting || column == 0 && value.trim().startsWith(comment));
        }
    }

    private record Cell(int row, int column) {
    }

    private CsvTableReplacement(CharSequence snapshot, Map<Cell, List<Match>> targets, int[] rows, int[] columns,
                                FindModel query, FindManager manager, Settings settings) {
        this.snapshot = snapshot;
        this.targets = targets;
        this.rows = rows;
        this.columns = columns;
        this.query = query;
        this.manager = manager;
        this.settings = settings;
    }

    static Change prepare(CharSequence snapshot, List<Match> matches, int[] rows, int[] columns,
                          FindModel query, FindManager manager, Settings settings)
            throws FindManager.MalformedReplacementStringException {
        return new CsvTableReplacement(snapshot, groupByCell(matches), rows, columns, query, manager, settings).prepare();
    }

    private static Map<Cell, List<Match>> groupByCell(List<Match> matches) {
        Map<Cell, List<Match>> targets = new HashMap<>();
        for (Match match : matches) {
            ProgressManager.checkCanceled();
            targets.computeIfAbsent(new Cell(match.row(), match.column()), cell -> new ArrayList<>()).add(match);
        }
        return targets;
    }

    private Change prepare() throws FindManager.MalformedReplacementStringException {
        Lexer lexer = CsvLexerFactory.getInstance().createLexer(settings.separator, settings.escape, settings.comment);
        lexer.start(snapshot);
        for (IElementType type = lexer.getTokenType(); type != null; type = lexer.getTokenType()) {
            ProgressManager.checkCanceled();
            if (type == TokenType.BAD_CHARACTER) throw new IllegalArgumentException("Invalid CSV character");
            if (type == CsvTypes.COMMA || type == CsvTypes.CRLF) {
                replaceField(lexer.getTokenStart());
                startField(lexer.getTokenEnd(), type == CsvTypes.CRLF);
            } else {
                appendValue(type, lexer);
            }
            lexer.advance();
        }
        ProgressManager.checkCanceled();
        replaceField(snapshot.length());
        ProgressManager.checkCanceled();
        return changeStart < 0 ? null : new Change(new TextRange(changeStart, changeEnd), result.toString());
    }

    private void appendValue(IElementType type, Lexer lexer) {
        if (type == CsvTypes.COMMENT) {
            value.append(snapshot, lexer.getTokenStart() + settings.comment.length(), lexer.getTokenEnd());
            comment = true;
        } else if (type == CsvTypes.TEXT || type == CsvTypes.ESCAPED_TEXT) {
            String token = lexer.getTokenText();
            value.append(type == CsvTypes.ESCAPED_TEXT && settings.escape.isEscapedQuote(token) ? "\"" : token);
        }
    }

    private void startField(int start, boolean newRow) {
        fieldStart = start;
        if (newRow) {
            row++;
            column = 0;
        } else {
            column++;
        }
        value.setLength(0);
        comment = false;
    }

    private void replaceField(int fieldEnd) throws FindManager.MalformedReplacementStringException {
        List<Match> cellMatches = targets.get(new Cell(getViewRow(), getViewColumn()));
        if (cellMatches == null) return;
        String replacement = settings.serialize(replaceMatches(value.toString(), cellMatches), comment, column);
        if (replacement == null || replacement.contentEquals(snapshot.subSequence(fieldStart, fieldEnd))) return;
        if (changeStart < 0) changeStart = fieldStart;
        else result.append(snapshot, changeEnd, fieldStart);
        result.append(replacement);
        changeEnd = fieldEnd;
    }

    private int getViewRow() {
        if (rows == null) return row;
        return row < rows.length ? rows[row] : -1;
    }

    private int getViewColumn() {
        return column < columns.length ? columns[column] : -1;
    }

    private String replaceMatches(String original, List<Match> cellMatches)
            throws FindManager.MalformedReplacementStringException {
        StringBuilder replaced = new StringBuilder();
        int offset = 0;
        for (Match match : cellMatches) {
            ProgressManager.checkCanceled();
            replaced.append(original, offset, match.start());
            replaced.append(manager.getStringToReplace(original.substring(match.start(), match.end()),
                    query, match.start(), original));
            offset = match.end();
        }
        return replaced.append(original, offset, original.length()).toString();
    }
}
