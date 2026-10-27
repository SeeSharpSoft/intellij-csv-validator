package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableSearchSessionBase;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.beans.PropertyChangeListener;

final class CsvTableSearchSession extends CsvTableSearchSessionBase<CsvTableEditorSwing> {
    private MatchCellRenderer cellMatchRenderer;
    private MatchCellRenderer commentMatchRenderer;
    private final ListSelectionListener selectionListener = event -> clearMatchHighlight();
    private final PropertyChangeListener editingListener = event -> clearMatchHighlight();

    CsvTableSearchSession(CsvTableEditorSwing editor) {
        super(editor);
        editor.getTable().getSelectionModel().addListSelectionListener(selectionListener);
        editor.getTable().getColumnModel().getSelectionModel().addListSelectionListener(selectionListener);
        editor.getTable().addPropertyChangeListener("tableCellEditor", editingListener);
    }

    @Override
    protected int getSelectedRow() {
        return editor.getTable().getSelectedRow();
    }

    @Override
    protected int getSelectedColumn() {
        return editor.getTable().getSelectedColumn();
    }

    @Override
    protected int[] getViewRowIndices() {
        JTable table = editor.getTable();
        // Avoid an O(rows) mapping pass on the EDT when there is no sorter.
        if (table.getRowSorter() == null) return null;
        int[] rows = new int[table.getModel().getRowCount()];
        for (int row = 0; row < rows.length; row++) rows[row] = table.convertRowIndexToView(row);
        return rows;
    }

    @Override
    protected int convertColumnIndexToView(int column) {
        return editor.getTable().convertColumnIndexToView(column);
    }

    @Override
    protected int convertRowIndexToModel(int row) {
        return editor.getTable().convertRowIndexToModel(row);
    }

    @Override
    protected int convertColumnIndexToModel(int column) {
        return editor.getTable().convertColumnIndexToModel(column);
    }

    @Override
    protected boolean stopEditing() {
        if (editor.isReordering()) return false;
        JTable table = editor.getTable();
        return !table.isEditing() || table.getCellEditor().stopCellEditing();
    }

    @Override
    protected void selectMatch(Match match) {
        if (editor.isReordering()) return;
        JTable table = editor.getTable();
        if (table.isEditing()) return;
        table.changeSelection(match.row(), match.column(), false, false);
        editor.getTable().scrollCellToVisible(match.row(), match.column());
        boolean comment = editor.getTable().isCommentRow(match.row());
        MatchCellRenderer renderer;
        if (comment) {
            if (commentMatchRenderer == null) commentMatchRenderer = new MatchCellRenderer(true);
            renderer = commentMatchRenderer;
        } else {
            if (cellMatchRenderer == null) cellMatchRenderer = new MatchCellRenderer(false);
            renderer = cellMatchRenderer;
        }
        renderer.start = match.start();
        renderer.end = match.end();
        editor.getTable().setSearchMatchRenderer(match.row(), match.column(), renderer);
    }

    @Override
    protected void clearMatchHighlight() {
        editor.getTable().clearSearchMatchRenderer();
    }

    @Override
    public void dispose() {
        super.dispose();
        editor.getTable().getSelectionModel().removeListSelectionListener(selectionListener);
        editor.getTable().getColumnModel().getSelectionModel().removeListSelectionListener(selectionListener);
        editor.getTable().removePropertyChangeListener("tableCellEditor", editingListener);
    }

    // Only the active cell uses this renderer; normal cell rendering never touches search results or text selections.
    private final class MatchCellRenderer implements TableCellRenderer {
        private final CsvMultiLineCellRenderer delegate;
        private final JTextArea textArea;
        private int start;
        private int end;

        private MatchCellRenderer(boolean comment) {
            delegate = editor.createCellRenderer(comment);
            textArea = (JTextArea) delegate.getViewport().getView();
            textArea.setCaret(new DefaultCaret() {
                @Override
                protected void adjustVisibility(Rectangle location) {
                    // Renderer carets must not enqueue scrolls after the component has been painted and resized.
                }
            });
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            TextAttributes attributes = scheme.getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES);
            textArea.setSelectionColor(attributes == null || attributes.getBackgroundColor() == null
                    ? JBColor.YELLOW : attributes.getBackgroundColor());
            textArea.setSelectedTextColor(attributes == null || attributes.getForegroundColor() == null
                    ? scheme.getDefaultForeground() : attributes.getForegroundColor());
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                        int row, int column) {
            Component rendered = delegate.getTableCellRendererComponent(table, value, selected, focused, row, column);
            textArea.select(start, end);
            textArea.getCaret().setSelectionVisible(true);
            delegate.doLayout();
            delegate.getViewport().doLayout();
            try {
                textArea.scrollRectToVisible(textArea.modelToView2D(start).getBounds());
            } catch (BadLocationException exception) {
                throw new IllegalStateException("Search match is outside the displayed cell text", exception);
            }
            return rendered;
        }
    }
}
