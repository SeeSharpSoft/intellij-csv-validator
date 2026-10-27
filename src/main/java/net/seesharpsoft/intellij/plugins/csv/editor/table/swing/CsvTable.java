package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ui.table.JBTable;
import net.seesharpsoft.intellij.ui.EditableCellFocusAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.TableUI;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

public class CsvTable extends JBTable {

    public static class CommentColumn {
    }

    private int paintCounter;
    private TableCellRenderer searchMatchRenderer;
    private int searchMatchRow;
    private int searchMatchColumn;

    public CsvTable(TableModel model) {
        super(model);
        setUI(new MultiSpanCellTableUI());
        setCellSelectionEnabled(true);
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("TAB"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("shift TAB"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("RIGHT"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("LEFT"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("UP"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("DOWN"));
    }

    @Override
    public void setUI(TableUI ui) {
        super.setUI(createTableUI());
    }

    protected TableUI createTableUI() {
        return new MultiSpanCellTableUI();
    }

    @Override
    public CsvTableModelSwing getModel() {
        return (CsvTableModelSwing) super.getModel();
    }

    public boolean isCommentRow(int row) {
        return getModel().isCommentRow(convertRowIndexToModel(row));
    }

    @Override
    public int getSelectedColumn() {
        int selectedColumn = super.getSelectedColumn();
        return Math.min(selectedColumn, getColumnCount() - 1);
    }

    @Override
    public @NotNull Rectangle getCellRect(int row, int column, boolean includeSpacing) {
        Rectangle rect = super.getCellRect(row, column, includeSpacing);
        if (getColumnCount() > 0 && isCommentRow(row)) {
            if (convertColumnIndexToModel(column) == 0) {
                rect.width = this.getWidth();
            } else {
                return new Rectangle();
            }
        }
        return rect;
    }

    // TODO this enables proper selection/handling of comment row, but leads to bad drawing issues - don't know why
    @Override
    public int columnAtPoint(@NotNull Point point) {
        if (paintCounter != 0) {
            // the original paint algorithm doesn't handle cell span well
            return super.columnAtPoint(point);
        }
        int rowAtPoint = super.rowAtPoint(point);
        return isCommentRow(rowAtPoint) ? 0 : super.columnAtPoint(point);
    }

    @Override
    public void setColumnSelectionInterval(int index0, int index1) {
        super.setColumnSelectionInterval(index0, index1);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (searchMatchRenderer != null && row == searchMatchRow && column == searchMatchColumn) {
            return searchMatchRenderer;
        }
        if (isCommentRow(row)) {
            return getDefaultRenderer(CommentColumn.class);
        }
        return super.getCellRenderer(row, column);
    }

    void setSearchMatchRenderer(int row, int column, TableCellRenderer renderer) {
        clearSearchMatchRenderer();
        searchMatchRow = row;
        searchMatchColumn = column;
        searchMatchRenderer = renderer;
        repaint(getCellRect(row, column, true));
    }

    void clearSearchMatchRenderer() {
        if (searchMatchRenderer == null) return;
        searchMatchRenderer = null;
        repaint(getCellRect(searchMatchRow, searchMatchColumn, true));
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        if (isCommentRow(row)) {
            return getDefaultEditor(CommentColumn.class);
        }
        return super.getCellEditor(row, column);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return (!isCommentRow(row) || convertColumnIndexToModel(column) == 0) && super.isCellEditable(row, column);
    }

    public class MultiSpanCellTableUI extends BasicTableUI {

        @Override
        public void paint(Graphics g, JComponent c) {
            paintTable(g, rendererPane, () -> super.paint(g, c));
        }
    }

    // Both UI delegates need the same spanning behavior for comment rows.
    protected void paintTable(Graphics g, CellRendererPane rendererPane, Runnable paintCells) {
        paintCounter++;
        try {
            paintCells.run();
            Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
            Rectangle clip = g.getClipBounds();
            if (getRowCount() <= 0 || getColumnCount() <= 0 || (clip != null && !bounds.intersects(clip))) {
                return;
            }

            Rectangle visibleBounds = clip == null ? bounds : clip.intersection(bounds);
            int firstRow = rowAtPoint(visibleBounds.getLocation());
            int lastRow = rowAtPoint(new Point(visibleBounds.x + visibleBounds.width - 1,
                    visibleBounds.y + visibleBounds.height - 1));
            if (firstRow == -1) firstRow = 0;
            if (lastRow == -1) lastRow = getRowCount() - 1;

            for (int row = firstRow; row <= lastRow; row++) {
                if (isCommentRow(row)) {
                    paintCommentRow(g, rendererPane, row);
                }
            }
        } finally {
            paintCounter--;
        }
    }

    private void paintCommentRow(Graphics g, CellRendererPane rendererPane, int row) {
        Rectangle cellRect = getCellRect(row, 0, true);
        cellRect.width = getWidth();
        int spacingHeight = getRowMargin();
        int spacingWidth = getColumnModel().getColumnMargin();

        Color color = g.getColor();
        g.setColor(getGridColor());
        if (getShowHorizontalLines()) {
            g.drawLine(cellRect.x, cellRect.y, cellRect.x + cellRect.width - 1, cellRect.y);
            g.drawLine(cellRect.x, cellRect.y + cellRect.height - 1,
                    cellRect.x + cellRect.width - 1, cellRect.y + cellRect.height - 1);
        }
        if (getShowVerticalLines()) {
            g.drawLine(cellRect.x, cellRect.y, cellRect.x, cellRect.y + cellRect.height - 1);
            g.drawLine(cellRect.x + cellRect.width - 1, cellRect.y,
                    cellRect.x + cellRect.width - 1, cellRect.y + cellRect.height - 1);
        }
        g.setColor(color);
        cellRect.setBounds(cellRect.x + spacingWidth / 2, cellRect.y + spacingHeight / 2,
                cellRect.width - spacingWidth, cellRect.height - spacingHeight);

        if (isEditing() && getEditingRow() == row && getEditingColumn() == 0) {
            getEditorComponent().setBounds(cellRect);
        } else {
            Component component = prepareRenderer(getCellRenderer(row, 0), row, 0);
            if (component.getParent() == null) {
                rendererPane.add(component);
            }
            rendererPane.paintComponent(g, component, this, cellRect.x, cellRect.y,
                    cellRect.width, cellRect.height, true);
        }
    }

}
