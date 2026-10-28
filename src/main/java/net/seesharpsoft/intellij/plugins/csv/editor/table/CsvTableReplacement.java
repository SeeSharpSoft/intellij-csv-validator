package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
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
                String text = value.trim().startsWith(comment) ? value : comment + value;
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

    private CsvTableReplacement() {
    }

    static Change prepare(CharSequence snapshot, List<Match> matches, int[] rows, int[] columns,
                          FindModel query, FindManager manager, Settings settings)
            throws FindManager.MalformedReplacementStringException {
        Map<Cell, List<Match>> targets = new HashMap<>();
        for (Match match : matches) {
            ProgressManager.checkCanceled();
            targets.computeIfAbsent(new Cell(match.row(), match.column()), cell -> new ArrayList<>()).add(match);
        }
        Lexer lexer = CsvLexerFactory.getInstance().createLexer(settings.separator, settings.escape, settings.comment);
        lexer.start(snapshot);
        StringBuilder value = new StringBuilder();
        StringBuilder result = new StringBuilder();
        int row = 0;
        int column = 0;
        int fieldStart = 0;
        int changeStart = -1;
        int changeEnd = 0;
        boolean comment = false;
        while (true) {
            ProgressManager.checkCanceled();
            var type = lexer.getTokenType();
            if (type == TokenType.BAD_CHARACTER) throw new IllegalArgumentException("Invalid CSV character");
            if (type == null || type == CsvTypes.COMMA || type == CsvTypes.CRLF) {
                int fieldEnd = type == null ? snapshot.length() : lexer.getTokenStart();
                int viewRow = rows == null ? row : row < rows.length ? rows[row] : -1;
                int viewColumn = column < columns.length ? columns[column] : -1;
                List<Match> cellMatches = targets.get(new Cell(viewRow, viewColumn));
                if (cellMatches != null) {
                    String original = value.toString();
                    StringBuilder replaced = new StringBuilder();
                    int offset = 0;
                    for (Match match : cellMatches) {
                        ProgressManager.checkCanceled();
                        replaced.append(original, offset, match.start());
                        replaced.append(manager.getStringToReplace(original.substring(match.start(), match.end()),
                                query, match.start(), original));
                        offset = match.end();
                    }
                    replaced.append(original, offset, original.length());
                    String replacement = settings.serialize(replaced.toString(), comment, column);
                    if (replacement != null && !replacement.contentEquals(snapshot.subSequence(fieldStart, fieldEnd))) {
                        if (changeStart < 0) changeStart = fieldStart;
                        else result.append(snapshot, changeEnd, fieldStart);
                        result.append(replacement);
                        changeEnd = fieldEnd;
                    }
                }
                if (type == null) break;
                fieldStart = lexer.getTokenEnd();
                if (type == CsvTypes.CRLF) {
                    row++;
                    column = 0;
                } else {
                    column++;
                }
                value.setLength(0);
                comment = false;
            } else if (type == CsvTypes.COMMENT) {
                value.append(snapshot, lexer.getTokenStart() + settings.comment.length(), lexer.getTokenEnd());
                comment = true;
            } else if (type == CsvTypes.TEXT || type == CsvTypes.ESCAPED_TEXT) {
                String token = lexer.getTokenText();
                value.append(type == CsvTypes.ESCAPED_TEXT && settings.escape.isEscapedQuote(token) ? "\"" : token);
            }
            lexer.advance();
        }
        ProgressManager.checkCanceled();
        return changeStart < 0 ? null : new Change(new TextRange(changeStart, changeEnd), result.toString());
    }
}
