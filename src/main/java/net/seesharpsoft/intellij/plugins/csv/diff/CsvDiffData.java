package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.diff.contents.DocumentContent;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvLexerFactory;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacterHolder;
import net.seesharpsoft.intellij.plugins.csv.components.CsvSeparatorHolder;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import java.util.ArrayList;
import java.util.List;

record CsvDiffData(String text, long stamp, Format format, List<Row> rows, List<String> headers, boolean valid) {
    record Format(CsvValueSeparator separator, CsvEscapeCharacter escape, Settings settings) {
        record Settings(CsvValueSeparator separator, CsvEscapeCharacter escape, String comment, boolean quoting,
                        boolean autoDetect) {
            static Settings capture() {
                CsvEditorSettings settings = CsvEditorSettings.getInstance();
                return new Settings(settings.getDefaultValueSeparator(), settings.getDefaultEscapeCharacter(),
                        settings.getCommentIndicator(), settings.isQuotingEnforced(), settings.isAutoDetectValueSeparator());
            }
        }

        String comment() {
            return settings.comment();
        }

        boolean quoting() {
            return settings.quoting();
        }

        static Format forContent(Project project, DocumentContent content) {
            Settings settings = Settings.capture();
            CsvValueSeparator separator = separator(project, content, settings);
            CsvEscapeCharacter escape;
            if (content.getContentType() instanceof CsvEscapeCharacterHolder holder)
                escape = holder.getEscapeCharacter();
            else if (content.getHighlightFile() != null)
                escape = CsvHelper.getEscapeCharacter(project, content.getHighlightFile());
            else escape = settings.escape();
            return new Format(separator, escape, settings);
        }

        private static CsvValueSeparator separator(Project project, DocumentContent content, Settings settings) {
            if (content.getContentType() instanceof CsvSeparatorHolder holder) return holder.getSeparator();
            if (project != null && CsvHelper.isCsvFile(content.getHighlightFile())) {
                return CsvHelper.getValueSeparator(project, content.getHighlightFile());
            }
            return settings.autoDetect() ? CsvFileAttributes.detectValueSeparator(content.getDocument().getText()) : settings.separator();
        }

        boolean isCurrent(Project project, DocumentContent content) {
            if (!settings.equals(Settings.capture())) return false;
            if (project == null || content.getHighlightFile() == null) return true;
            var file = content.getHighlightFile();
            CsvFileAttributes attributes = CsvFileAttributes.getInstance(project);
            if (!(content.getContentType() instanceof CsvSeparatorHolder)
                    && attributes.hasValueSeparatorAttribute(project, file)
                    && !separator.equals(attributes.getValueSeparator(project, file))) return false;
            return content.getContentType() instanceof CsvEscapeCharacterHolder
                    || escape.equals(CsvHelper.getEscapeCharacter(project, file));
        }
    }

    record Cell(TextRange range, String value, int lines) {
    }

    record Row(TextRange range, List<Cell> cells, boolean comment) {
    }

    static CsvDiffData read(DocumentContent content, Format format) {
        String text = content.getDocument().getText();
        long stamp = content.getDocument().getModificationStamp();
        try {
            List<Row> rows = parse(text, format);
            return new CsvDiffData(text, stamp, format, rows, headers(rows), true);
        } catch (IllegalArgumentException exception) {
            return new CsvDiffData(text, stamp, format, List.of(), List.of(), false);
        }
    }

    private static List<String> headers(List<Row> rows) {
        for (int index = 0; index < rows.size(); index++) {
            if ((index & 1023) == 0) ProgressManager.checkCanceled();
            Row row = rows.get(index);
            if (!row.comment()) return row.cells().stream().map(cell -> cell.value().trim()).toList();
        }
        return List.of();
    }

    private static List<Row> parse(String text, Format format) {
        Lexer lexer = CsvLexerFactory.getInstance().createLexer(format.separator(), format.escape(), format.comment());
        lexer.start(text);
        List<Row> rows = new ArrayList<>();
        List<Cell> cells = new ArrayList<>();
        int rowStart = 0;
        int fieldStart = 0;
        int quoteState = 0;
        boolean hasText = false;
        boolean comment = false;
        int tokens = 0;
        while (lexer.getTokenType() != null) {
            if ((tokens++ & 1023) == 0) ProgressManager.checkCanceled();
            var type = lexer.getTokenType();
            if (type == TokenType.BAD_CHARACTER) throw new IllegalArgumentException("Invalid CSV character");
            if (type == CsvTypes.QUOTE) {
                if (quoteState == 2 || quoteState == 0 && hasText) {
                    throw new IllegalArgumentException("Unexpected CSV quote");
                }
                quoteState++;
            } else if (type == CsvTypes.TEXT || type == CsvTypes.ESCAPED_TEXT) {
                if (quoteState == 2) throw new IllegalArgumentException("Text after closing CSV quote");
                hasText = true;
            } else if (type == CsvTypes.COMMENT) {
                comment = true;
            } else if (type == CsvTypes.COMMA || type == CsvTypes.CRLF) {
                cells.add(cell(text, fieldStart, lexer.getTokenStart(), format, comment));
                if (type == CsvTypes.CRLF) {
                    rows.add(new Row(new TextRange(rowStart, lexer.getTokenStart()), List.copyOf(cells), comment));
                    cells.clear();
                    rowStart = lexer.getTokenEnd();
                    comment = false;
                }
                fieldStart = lexer.getTokenEnd();
                quoteState = 0;
                hasText = false;
            }
            lexer.advance();
        }
        if (quoteState == 1) throw new IllegalArgumentException("Unclosed CSV quote");
        if (rowStart < text.length()) {
            cells.add(cell(text, fieldStart, text.length(), format, comment));
            rows.add(new Row(new TextRange(rowStart, text.length()), List.copyOf(cells), comment));
        }
        return List.copyOf(rows);
    }

    private static Cell cell(String text, int start, int end, Format format, boolean comment) {
        String raw = text.substring(start, end);
        String value = comment ? raw.substring(format.comment().length()) : CsvHelper.unquoteCsvValue(raw, format.escape());
        int lines = 1 + (int) value.chars().filter(character -> character == '\n').count();
        return new Cell(new TextRange(start, end), value, lines);
    }
}
