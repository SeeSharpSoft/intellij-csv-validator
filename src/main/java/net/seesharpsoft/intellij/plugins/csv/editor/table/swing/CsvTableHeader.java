package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;

public class CsvTableHeader extends JTableHeader {
    private boolean painting;
    private Runnable columnReorderingHandler;

    public CsvTableHeader(TableColumnModel columnModel) {
        super(columnModel);
    }

    void setColumnReorderingHandler(Runnable handler) {
        columnReorderingHandler = handler;
    }

    void cancelColumnReordering() {
        super.setDraggedColumn(null);
        setDraggedDistance(0);
    }

    @Override
    public void setDraggedColumn(TableColumn column) {
        boolean finished = getDraggedColumn() != null && column == null;
        super.setDraggedColumn(column);
        if (finished && columnReorderingHandler != null) columnReorderingHandler.run();
    }

    @Override
    protected void processMouseEvent(MouseEvent event) {
        if (event.getID() == MouseEvent.MOUSE_PRESSED && SwingUtilities.isLeftMouseButton(event) &&
                getTable() instanceof CsvTable csvTable && csvTable.isEditing()) {
            if (!csvTable.getCellEditor().stopCellEditing()) return;
            // Flush the delayed cell update before the UI captures a dragged TableColumn.
            csvTable.getModel().updateTableImmediately();
        }
        super.processMouseEvent(event);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent event) {
        if (event.getID() == MouseEvent.MOUSE_DRAGGED && getDraggedColumn() != null &&
                getReorderingAllowed() && getTable() instanceof CsvTable csvTable && csvTable.getFrozenColumnCount() > 0) {
            // Native header dragging assumes contiguous, unscrolled column rectangles.
            // Frozen columns instead need hit testing in their painted positions.
            int from = csvTable.convertColumnIndexToView(getDraggedColumn().getModelIndex());
            int to = columnAtPoint(event.getPoint());
            if (from >= 0 && to >= 0 && from != to) getColumnModel().moveColumn(from, to);
            setDraggedDistance(0);
            repaint();
            return;
        }
        super.processMouseMotionEvent(event);
    }

    @Override
    public Rectangle getHeaderRect(int column) {
        Rectangle rect = super.getHeaderRect(column);
        if (!painting && getTable() instanceof CsvTable table && column >= 0 && column < table.getFrozenColumnCount()) {
            rect.x += table.getFrozenColumnOffset();
        }
        return rect;
    }

    @Override
    public int columnAtPoint(Point point) {
        Point location = new Point(point);
        if (!painting && getTable() instanceof CsvTable table) {
            int offset = table.getFrozenColumnOffset();
            if (location.x >= offset && location.x < offset + table.getFrozenColumnWidth()) {
                location.x -= offset;
            }
        }
        return super.columnAtPoint(location);
    }

    @Override
    public void paint(Graphics g) {
        if (getTable() instanceof CsvTable csvTable && csvTable.deferPaintWithoutReadAccess()) return;
        super.paint(g);
    }

    @Override
    protected void paintComponent(Graphics g) {
        painting = true;
        try {
            super.paintComponent(g);
            if (getTable() instanceof CsvTable table && table.getFrozenColumnCount() > 0) {
                Graphics frozen = g.create();
                try {
                    frozen.translate(table.getFrozenColumnOffset(), 0);
                    frozen.clipRect(0, 0, table.getFrozenColumnWidth(), getHeight());
                    super.paintComponent(frozen);
                } finally {
                    frozen.dispose();
                }
                table.paintFrozenColumnSeparator(g, getHeight());
            }
        } finally {
            painting = false;
        }
    }
}
