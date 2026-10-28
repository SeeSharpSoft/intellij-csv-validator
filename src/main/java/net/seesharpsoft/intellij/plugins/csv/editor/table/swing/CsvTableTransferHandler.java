package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.lexer.Lexer;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.TokenType;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvLexerFactory;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class CsvTableTransferHandler extends TransferHandler {
    private record Cells(List<List<String>> values, Set<Integer> commentRows) {
    }

    private static final DataFlavor CELLS_FLAVOR = createCellsFlavor();

    private static DataFlavor createCellsFlavor() {
        try {
            return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + Cells.class.getName() + "\"",
                    "CSV table cells", Cells.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final CsvTableEditorSwing editor;
    private final TransferHandler copyHandler;

    CsvTableTransferHandler(CsvTableEditorSwing editor, TransferHandler copyHandler) {
        this.editor = editor;
        this.copyHandler = copyHandler;
    }

    @Override
    public int getSourceActions(JComponent component) {
        return editor.isReordering() ? NONE : COPY;
    }

    @Override
    protected Transferable createTransferable(JComponent component) {
        CsvTable table = editor.getTable();
        if (table.getSelectedRowCount() == 0 || table.getSelectedColumnCount() == 0) return null;
        Clipboard clipboard = new Clipboard("CSV selection");
        copyHandler.exportToClipboard(component, clipboard, COPY);
        Transferable original = clipboard.getContents(null);
        List<List<String>> values = new ArrayList<>();
        Set<Integer> commentRows = new HashSet<>();
        for (int row : table.getSelectedRows()) {
            int modelRow = table.convertRowIndexToModel(row);
            if (table.getModel().isCommentRow(modelRow)) {
                commentRows.add(values.size());
                values.add(List.of(table.getModel().getValue(modelRow, 0)));
                continue;
            }
            List<String> cells = new ArrayList<>();
            for (int column : table.getSelectedColumns()) cells.add((String) table.getValueAt(row, column));
            values.add(List.copyOf(cells));
        }
        Cells cells = new Cells(List.copyOf(values), Set.copyOf(commentRows));
        String csv = toCsv(cells);
        String html = commentRows.isEmpty() ? null : toHtml(cells, table.getSelectedColumnCount());
        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                DataFlavor[] flavors = original.getTransferDataFlavors();
                DataFlavor[] result = Arrays.copyOf(flavors, flavors.length + 1);
                result[flavors.length] = CELLS_FLAVOR;
                return result;
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return CELLS_FLAVOR.equals(flavor) || original.isDataFlavorSupported(flavor);
            }

            @Override
            public @NotNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                if (CELLS_FLAVOR.equals(flavor)) return cells;
                if (!original.isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
                String text = isPlainTextFlavor(flavor) ? csv : flavor.isMimeTypeEqual("text/html") ? html : null;
                if (text != null) {
                    if (String.class.equals(flavor.getRepresentationClass())) return text;
                    if (Reader.class.equals(flavor.getRepresentationClass())) return new StringReader(text);
                    if (InputStream.class.equals(flavor.getRepresentationClass())) {
                        String charset = flavor.getParameter("charset");
                        if (charset != null) return new ByteArrayInputStream(text.getBytes(charset));
                    }
                    throw new UnsupportedFlavorException(flavor);
                }
                return original.getTransferData(flavor);
            }
        };
    }

    private String toCsv(Cells selection) {
        var separator = CsvHelper.getValueSeparator(editor.getProject(), editor.getFile());
        var escape = CsvHelper.getEscapeCharacter(editor.getProject(), editor.getFile());
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        String comment = settings.getCommentIndicator();
        StringBuilder csv = new StringBuilder();
        for (int row = 0; row < selection.values().size(); row++) {
            if (row > 0) csv.append('\n');
            List<String> cells = selection.values().get(row);
            if (selection.commentRows().contains(row)) {
                csv.append(comment).append(cells.getFirst());
                continue;
            }
            for (int column = 0; column < cells.size(); column++) {
                if (column > 0) csv.append(separator.getCharacter());
                String value = cells.get(column);
                csv.append(CsvHelper.quoteCsvField(value, escape, separator,
                        settings.isQuotingEnforced() || column == 0 && !comment.isEmpty() && value.trim().startsWith(comment)));
            }
        }
        return csv.toString();
    }

    private static String toHtml(Cells selection, int columnCount) {
        String comment = CsvEditorSettings.getInstance().getCommentIndicator();
        StringBuilder html = new StringBuilder("<html>\n<body>\n<table>\n");
        for (int row = 0; row < selection.values().size(); row++) {
            html.append("<tr>");
            List<String> values = selection.values().get(row);
            if (selection.commentRows().contains(row)) {
                html.append("<td colspan=\"").append(columnCount).append("\">")
                        .append(StringUtil.escapeXmlEntities(comment + values.getFirst())).append("</td>");
            } else {
                for (String value : values) html.append("<td>").append(StringUtil.escapeXmlEntities(value)).append("</td>");
            }
            html.append("</tr>\n");
        }
        return html.append("</table>\n</body>\n</html>").toString();
    }

    @Override
    public boolean canImport(JComponent component, DataFlavor[] flavors) {
        CsvTable table = editor.getTable();
        int row = table.getSelectionModel().getLeadSelectionIndex();
        int column = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
        return component == table && table.isEnabled() && !table.isEditing() && editor.isEditable()
                && !editor.getTableModel().hasErrors() && editor.getDocument() != null && editor.getDocument().isWritable()
                && row >= 0 && row < table.getRowCount() && table.isRowSelected(row)
                && column >= 0 && column < table.getColumnCount() && table.isColumnSelected(column)
                && (Arrays.asList(flavors).contains(CELLS_FLAVOR) || getTextFlavor(flavors) != null);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return !support.isDrop() && super.canImport(support);
    }

    @Override
    public boolean importData(TransferSupport support) {
        return canImport(support) && super.importData(support);
    }

    @Override
    public boolean importData(JComponent component, Transferable transferable) {
        if (!canImport(component, transferable.getTransferDataFlavors())) return false;
        try {
            Cells cells;
            if (transferable.isDataFlavorSupported(CELLS_FLAVOR)) {
                cells = (Cells) transferable.getTransferData(CELLS_FLAVOR);
            } else {
                DataFlavor flavor = getTextFlavor(transferable.getTransferDataFlavors());
                if (flavor == null) throw new UnsupportedFlavorException(DataFlavor.stringFlavor);
                StringWriter buffer = new StringWriter();
                try (Reader reader = flavor.getReaderForText(transferable)) {
                    reader.transferTo(buffer);
                }
                String text = StringUtil.convertLineSeparators(buffer.toString());
                var separator = CsvHelper.getValueSeparator(editor.getPsiFile());
                var escape = CsvHelper.getEscapeCharacter(editor.getPsiFile());
                if (!separator.equals(CsvValueSeparator.TAB) && text.indexOf('\t') >= 0) {
                    Cells tsv = parse(text, CsvValueSeparator.TAB, escape);
                    cells = tsv.values().stream().anyMatch(row -> row.size() > 1) ? tsv : parse(text, separator, escape);
                } else {
                    cells = parse(text, separator, escape);
                }
            }
            CsvTable table = editor.getTable();
            int row = table.convertRowIndexToModel(table.getSelectionModel().getLeadSelectionIndex());
            int column = table.convertColumnIndexToModel(table.getColumnModel().getSelectionModel().getLeadSelectionIndex());
            table.getModel().setValuesAt(cells.values(), cells.commentRows(), row, column);
            return true;
        } catch (UnsupportedFlavorException | IOException exception) {
            notifyFailure("table.paste.clipboard.error");
        } catch (IllegalArgumentException exception) {
            notifyFailure("table.paste.invalid");
        }
        return false;
    }

    private void notifyFailure(String key) {
        NotificationGroupManager.getInstance().getNotificationGroup("net.seesharpsoft.intellij.plugins.csv")
                .createNotification(CsvPluginManager.getLocalizedText(key), NotificationType.WARNING).notify(editor.getProject());
    }

    private static @Nullable DataFlavor getTextFlavor(DataFlavor[] flavors) {
        return DataFlavor.selectBestTextFlavor(Arrays.stream(flavors)
                .filter(CsvTableTransferHandler::isPlainTextFlavor)
                .toArray(DataFlavor[]::new));
    }

    private static boolean isPlainTextFlavor(DataFlavor flavor) {
        return DataFlavor.stringFlavor.equals(flavor) || flavor.isMimeTypeEqual("text/plain")
                || flavor.isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType)
                && String.class.equals(flavor.getRepresentationClass());
    }

    private static Cells parse(String text, CsvValueSeparator separator, CsvEscapeCharacter escape) {
        String comment = CsvEditorSettings.getInstance().getCommentIndicator();
        Lexer lexer = CsvLexerFactory.getInstance().createLexer(separator, escape, comment);
        lexer.start(text);
        List<List<String>> rows = new ArrayList<>();
        Set<Integer> commentRows = new HashSet<>();
        List<String> row = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        int recordStart = 0;
        while (lexer.getTokenType() != null) {
            var type = lexer.getTokenType();
            if (type == TokenType.BAD_CHARACTER) throw new IllegalArgumentException("Invalid clipboard CSV");
            if (type == CsvTypes.QUOTE) {
                quoted = !quoted;
            } else if (type == CsvTypes.COMMA || type == CsvTypes.CRLF) {
                row.add(value.toString());
                value.setLength(0);
                if (type == CsvTypes.CRLF) {
                    rows.add(List.copyOf(row));
                    row.clear();
                    recordStart = lexer.getTokenEnd();
                }
            } else if (type == CsvTypes.COMMENT) {
                commentRows.add(rows.size());
                value.append(lexer.getTokenText().substring(comment.length()));
            } else if (type == CsvTypes.TEXT || type == CsvTypes.ESCAPED_TEXT) {
                String token = lexer.getTokenText();
                value.append(type == CsvTypes.ESCAPED_TEXT && escape.isEscapedQuote(token) ? "\"" : token);
            }
            lexer.advance();
        }
        if (quoted) throw new IllegalArgumentException("Unterminated clipboard CSV field");
        if (recordStart < text.length() || rows.isEmpty()) {
            row.add(value.toString());
            rows.add(List.copyOf(row));
        }
        return new Cells(List.copyOf(rows), Set.copyOf(commentRows));
    }
}
