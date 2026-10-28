package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import net.seesharpsoft.intellij.ui.EditableCellFocusAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.TableUI;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.function.Consumer;

public class CsvTable extends JBTable {

    private static final Color FROZEN_COLUMN_SEPARATOR_COLOR = new JBColor(0x8C9199, 0x737882);

    public static class CommentColumn {
    }

    private int paintCounter;
    private TableCellRenderer searchMatchRenderer;
    private int searchMatchRow;
    private int searchMatchColumn;
    private int frozenColumnCount;

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
    protected @NotNull JTableHeader createDefaultTableHeader() {
        return new CsvTableHeader(columnModel);
    }

    public int getFrozenColumnCount() {
        return Math.min(frozenColumnCount, getColumnCount());
    }

    public void setFrozenColumnCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Frozen column count must not be negative");
        }
        frozenColumnCount = count;
        int scrollMode = count > 0 ? JViewport.SIMPLE_SCROLL_MODE : JViewport.BLIT_SCROLL_MODE;
        if (getParent() instanceof JViewport viewport) {
            viewport.setScrollMode(scrollMode);
        }
        if (getTableHeader().getParent() instanceof JViewport viewport) {
            viewport.setScrollMode(scrollMode);
        }
        updateFrozenColumnLayout();
    }

    int getFrozenColumnOffset() {
        return getFrozenColumnCount() == 0 ? 0 : getVisibleRect().x;
    }

    int getFrozenColumnWidth() {
        int width = 0;
        for (int column = 0; column < getFrozenColumnCount(); column++) {
            width += getColumnModel().getColumn(column).getWidth();
        }
        return width;
    }

    void paintFrozenColumnSeparator(Graphics g, int height) {
        if (getFrozenColumnCount() == 0) return;
        int width = JBUI.scale(2);
        int boundary = getFrozenColumnOffset() + getFrozenColumnWidth();
        Color color = g.getColor();
        g.setColor(FROZEN_COLUMN_SEPARATOR_COLOR);
        g.fillRect(boundary - width, 0, width, height);
        g.setColor(color);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        // Paint above spanning comment rows and cell editors as well as ordinary cells.
        paintFrozenColumnSeparator(g, getHeight());
        if (getClientProperty(CsvTableRowDragListener.class) instanceof CsvTableRowDragListener drag) {
            drag.paintFeedback(g, this);
        }
    }

    void cancelRowDragging() {
        if (getClientProperty(CsvTableRowDragListener.class) instanceof CsvTableRowDragListener drag) drag.cancelDrag();
    }

    void updateFrozenColumnLayout() {
        positionCellEditor();
        repaint();
        getTableHeader().repaint();
    }

    private void positionCellEditor() {
        if (isEditing()) {
            Rectangle rect = getCellRect(getEditingRow(), getEditingColumn(), false);
            if (isCommentRow(getEditingRow())) {
                rect.width -= getColumnModel().getColumnMargin();
            }
            getEditorComponent().setBounds(rect);
        }
    }

    @Override
    public void changeSelection(int row, int column, boolean toggle, boolean extend) {
        if (getFrozenColumnCount() == 0) {
            super.changeSelection(row, column, toggle, extend);
            return;
        }
        boolean autoscrolls = getAutoscrolls();
        setAutoscrolls(false);
        try {
            super.changeSelection(row, column, toggle, extend);
        } finally {
            setAutoscrolls(autoscrolls);
        }
        if (autoscrolls) scrollCellToVisible(row, column);
    }

    public void scrollCellToVisible(int row, int column) {
        Rectangle rect = getCellRect(row, column, true);
        if (getFrozenColumnCount() > 0) {
            Rectangle visible = getVisibleRect();
            int x = visible.x;
            if (column >= getFrozenColumnCount() && !isCommentRow(row)) {
                int frozenWidth = getFrozenColumnWidth();
                if (rect.x < visible.x + frozenWidth || rect.width > visible.width - frozenWidth) {
                    x = Math.max(0, rect.x - frozenWidth);
                } else if (rect.x + rect.width > visible.x + visible.width) {
                    x = rect.x + rect.width - visible.width;
                }
            }
            rect.x = x;
            rect.width = visible.width;
        }
        super.scrollRectToVisible(rect);
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
            if (column == 0) {
                rect.width = this.getWidth();
            } else {
                return new Rectangle();
            }
        }
        if (paintCounter == 0 && column >= 0 && column < getFrozenColumnCount()) {
            rect.x += getFrozenColumnOffset();
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
        if (isCommentRow(rowAtPoint)) return 0;
        Point location = new Point(point);
        int offset = getFrozenColumnOffset();
        if (location.x >= offset && location.x < offset + getFrozenColumnWidth()) {
            location.x -= offset;
        }
        return super.columnAtPoint(location);
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
        // Read-only cells open a non-editable text component for selection and copying.
        return isEnabled() && (!isCommentRow(row) || column == 0);
    }

    public class MultiSpanCellTableUI extends BasicTableUI {

        @Override
        public void paint(Graphics g, JComponent c) {
            paintTable(g, rendererPane, graphics -> super.paint(graphics, c));
        }
    }

    // Both UI delegates need the same spanning behavior for comment rows.
    protected void paintTable(Graphics g, CellRendererPane rendererPane, Consumer<Graphics> paintCells) {
        paintCounter++;
        try {
            paintCells.accept(g);
            if (getFrozenColumnCount() > 0) {
                // Keep one selection and column model; only the leading columns' painted position changes.
                Graphics frozen = g.create();
                try {
                    frozen.translate(getFrozenColumnOffset(), 0);
                    frozen.clipRect(0, 0, getFrozenColumnWidth(), getHeight());
                    frozen.setColor(getBackground());
                    frozen.fillRect(0, 0, getFrozenColumnWidth(), getHeight());
                    paintCells.accept(frozen);
                } finally {
                    frozen.dispose();
                }
            }
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
            positionCellEditor();
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        if (isEditing() && getFrozenColumnCount() > 0 && !isCommentRow(getEditingRow())) {
            Graphics clipped = g.create();
            try {
                Rectangle visible = getVisibleRect();
                int frozenWidth = Math.min(getFrozenColumnWidth(), visible.width);
                if (getEditingColumn() < getFrozenColumnCount()) {
                    clipped.clipRect(visible.x, visible.y, frozenWidth, visible.height);
                } else {
                    clipped.clipRect(visible.x + frozenWidth, visible.y, visible.width - frozenWidth, visible.height);
                }
                super.paintChildren(clipped);
            } finally {
                clipped.dispose();
            }
        } else {
            super.paintChildren(g);
        }
    }

    private void paintCommentRow(Graphics g, CellRendererPane rendererPane, int row) {
        Rectangle cellRect = getCellRect(row, 0, true);
        cellRect.x = getFrozenColumnOffset();
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
