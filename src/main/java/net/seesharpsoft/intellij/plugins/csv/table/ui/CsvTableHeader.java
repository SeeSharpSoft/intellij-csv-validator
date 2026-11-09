package net.seesharpsoft.intellij.plugins.csv.table.ui;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.*;
import java.awt.dnd.DragSource;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class CsvTableHeader extends JTableHeader {
    private boolean painting;
    private Runnable columnReorderingHandler;
    private final CsvTablePaintSnapshot paintSnapshot = new CsvTablePaintSnapshot();
    private int[] draggedColumns;
    private int sourceColumn;
    private int targetColumn;
    private int pressedX;
    private boolean selectionModifiers;
    private boolean draggingGroup;

    public CsvTableHeader(TableColumnModel columnModel) {
        super(columnModel);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        applyHeaderStyle(this);
    }

    @Override
    public @Nullable CsvTableBase getTable() {
        return (CsvTableBase) super.getTable();
    }

    static void applyHeaderStyle(JTableHeader header) {
        header.setBackground(UIUtil.getPanelBackground());
        header.setForeground(UIUtil.getLabelForeground());
        header.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            private final Border border = BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, JBColor.border()),
                    JBUI.Borders.empty(8, 10));

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, value, false, false, row, column);
                JTableHeader currentHeader = table.getTableHeader();
                setBackground(currentHeader.getBackground());
                setForeground(currentHeader.getForeground());
                setFont(currentHeader.getFont());
                setBorder(border);
                return this;
            }
        });
    }

    public void setColumnReorderingHandler(Runnable handler) {
        columnReorderingHandler = handler;
    }

    public void cancelColumnReordering() {
        clearGroupDrag();
        super.setDraggedColumn(null);
        setDraggedDistance(0);
    }

    @Override
    public void setDraggedColumn(TableColumn column) {
        boolean finished = getDraggedColumn() != null && column == null;
        if (finished) finishGroupDrag();
        super.setDraggedColumn(column);
        if (column != null) beginGroupDrag(column);
        if (finished && columnReorderingHandler != null) columnReorderingHandler.run();
    }

    private void beginGroupDrag(TableColumn column) {
        CsvTableBase table = getTable();
        if (selectionModifiers || table == null) return;
        int[] selected = table.getSelectedColumns();
        if (selected.length <= 1) return;
        int source = table.convertColumnIndexToView(column.getModelIndex());
        if (Arrays.binarySearch(selected, source) < 0) return;
        draggedColumns = selected;
        sourceColumn = targetColumn = source;
    }

    private void finishGroupDrag() {
        if (draggedColumns == null) return;
        if (draggingGroup) moveDraggedColumns();
        else getColumnModel().getSelectionModel().setSelectionInterval(sourceColumn, sourceColumn);
        clearGroupDrag();
    }

    private void clearGroupDrag() {
        draggedColumns = null;
        draggingGroup = false;
    }

    private void moveDraggedColumns() {
        int targetIndex = Arrays.binarySearch(draggedColumns, targetColumn);
        if (targetIndex >= 0) return;
        int selectedBeforeTarget = -targetIndex - 1;
        int firstTarget = targetColumn - selectedBeforeTarget + (sourceColumn < targetColumn ? 1 : 0);
        TableColumnModel columns = getColumnModel();
        // This traversal order keeps each not-yet-moved source at its original index.
        moveSelectedColumnsRight(columns, firstTarget);
        moveSelectedColumnsLeft(columns, firstTarget);
        columns.getSelectionModel().setSelectionInterval(firstTarget, firstTarget + draggedColumns.length - 1);
    }

    private void moveSelectedColumnsRight(TableColumnModel columns, int firstTarget) {
        for (int index = draggedColumns.length - 1; index >= 0; index--) {
            int from = draggedColumns[index];
            int to = firstTarget + index;
            if (from < to) columns.moveColumn(from, to);
        }
    }

    private void moveSelectedColumnsLeft(TableColumnModel columns, int firstTarget) {
        for (int index = 0; index < draggedColumns.length; index++) {
            int from = draggedColumns[index];
            int to = firstTarget + index;
            if (from > to) columns.moveColumn(from, to);
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent event) {
        CsvTableBase table = getTable();
        if (event.getID() == MouseEvent.MOUSE_PRESSED) {
            pressedX = event.getX();
            selectionModifiers = event.isControlDown() || event.isShiftDown() || event.isMetaDown();
            if (SwingUtilities.isLeftMouseButton(event) && table != null && table.isEditing()) {
                if (!table.getCellEditor().stopCellEditing()) return;
                // Flush the delayed cell update before the UI captures a dragged TableColumn.
                table.getModel().flushPendingUpdates();
            }
        }
        super.processMouseEvent(event);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent event) {
        CsvTableBase table = getTable();
        if (event.getID() == MouseEvent.MOUSE_DRAGGED && getDraggedColumn() != null &&
                getReorderingAllowed() && table != null) {
            if (draggedColumns != null) {
                dragColumnGroup(event, table);
                return;
            }
            if (table.getFrozenColumnCount() > 0) {
                dragFrozenColumn(event, table);
                return;
            }
        }
        super.processMouseMotionEvent(event);
    }

    private void dragColumnGroup(MouseEvent event, CsvTableBase table) {
        if (!draggingGroup && Math.abs(event.getX() - pressedX) < DragSource.getDragThreshold()) return;
        draggingGroup = true;
        targetColumn = dropColumnAtPoint(event.getPoint());
        setDraggedDistance(0);
        table.scrollColumnToVisible(targetColumn);
        repaint();
    }

    private int dropColumnAtPoint(Point point) {
        int column = columnAtPoint(point);
        if (column >= 0) return column;
        return point.x < getHeaderRect(0).x ? 0 : getColumnModel().getColumnCount() - 1;
    }

    private void dragFrozenColumn(MouseEvent event, CsvTableBase table) {
        // Native header dragging assumes contiguous, unscrolled column rectangles.
        // Frozen columns instead need hit testing in their painted positions.
        int from = table.convertColumnIndexToView(getDraggedColumn().getModelIndex());
        int to = columnAtPoint(event.getPoint());
        if (from >= 0 && to >= 0 && from != to) getColumnModel().moveColumn(from, to);
        setDraggedDistance(0);
        repaint();
    }

    @Override
    public Rectangle getHeaderRect(int column) {
        Rectangle rect = super.getHeaderRect(column);
        CsvTableBase table = getTable();
        if (!painting && table != null && column >= 0 && column < table.getFrozenColumnCount()) {
            rect.x += table.getFrozenColumnOffset();
        }
        return rect;
    }

    @Override
    public int columnAtPoint(Point point) {
        CsvTableBase table = getTable();
        if (!painting && table != null && table.getFrozenColumnCount() > 0) {
            int offset = table.getFrozenColumnOffset();
            if (point.x >= offset && point.x < offset + table.getFrozenColumnWidth()) {
                return super.columnAtPoint(new Point(point.x - offset, point.y));
            }
        }
        return super.columnAtPoint(point);
    }

    @Override
    public void paint(Graphics g) {
        CsvTableBase table = getTable();
        if (table != null && table.usesPaintSnapshot()) {
            paintSnapshot.paint(this, g, !table.deferPaintWithoutReadAccess(), super::paint);
        } else {
            super.paint(g);
        }
    }

    @Override
    public void removeNotify() {
        paintSnapshot.clear();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        CsvTableBase table = getTable();
        if (table == null) {
            super.paintComponent(g);
            return;
        }
        painting = true;
        try {
            super.paintComponent(g);
            paintFrozenColumns(g, table);
        } finally {
            painting = false;
        }
        paintGroupDragFeedback(g, table);
    }

    private void paintFrozenColumns(Graphics g, CsvTableBase table) {
        if (table.getFrozenColumnCount() == 0) return;
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

    private void paintGroupDragFeedback(Graphics graphics, CsvTableBase table) {
        if (!draggingGroup) return;
        Graphics g = graphics.create();
        try {
            Color accent = UIManager.getColor("Table.dropLineColor");
            if (accent == null) accent = table.getSelectionBackground();
            Color background = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40);
            for (int column : draggedColumns) {
                Rectangle bounds = getHeaderRect(column);
                g.setColor(background);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                g.setColor(accent);
                g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
            }
            if (Arrays.binarySearch(draggedColumns, targetColumn) >= 0) return;
            Rectangle target = getHeaderRect(targetColumn);
            int x = target.x + (targetColumn > sourceColumn ? target.width : 0);
            int thickness = JBUI.scale(3);
            g.fillRect(x - thickness / 2, 0, thickness, getHeight());
        } finally {
            g.dispose();
        }
    }
}
