//MIT License - https://github.com/oliverwatkins/swing_library
//
//        Copyright (c) 2018 Oliver Watkins
//
//        Permission is hereby granted, free of charge, to any person obtaining a copy
//        of this software and associated documentation files (the "Software"), to deal
//        in the Software without restriction, including without limitation the rights
//        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//        copies of the Software, and to permit persons to whom the Software is
//        furnished to do so, subject to the following conditions:
//
//        The above copyright notice and this permission notice shall be included in all
//        copies or substantial portions of the Software.
//
//        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//        SOFTWARE.
package net.seesharpsoft.intellij.plugins.csv.table.ui;

import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.plaf.UIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import java.awt.*;
import java.awt.event.*;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * TableRowUtilities. Utility for adding a row column to a JTable.
 * <p>
 * https://github.com/oliverwatkins/swing_library
 * <p>
 * Changes for CSV-Plugin:
 * - addNumberColumn method returns the created row header table
 * - addRowMarkerColumn shows thin row markers instead of numbers in the same row header
 * - use JBTable as RowHeaderTable instead of (deprecated) JTable
 * - removed System.out
 * - several code clean ups
 *
 * @author Oliver Watkins, Martin Sommer
 */
public final class CsvTableRowUtilities {

    public static final int ROW_NUMBER_CELL_PADDING = 8;
    private static final Object ROW_HEADER_OVERLAY = new Object();
    private static final Object ROW_HEADER_MOUSE_HANDLER = new Object();

    private record RowHeaderOverlay(BiConsumer<Graphics, JTable> painter) {
    }

    private record RowHeaderMouseHandler(Predicate<MouseEvent> handler) {
    }

    private CsvTableRowUtilities() {
        // static helper
    }

    public static void setSelection(ListSelectionModel selection, int[] indices, int count) {
        boolean adjusting = selection.getValueIsAdjusting();
        selection.setValueIsAdjusting(true);
        try {
            selection.clearSelection();
            int start = -1;
            int end = -1;
            for (int index : indices) {
                if (count == 0) break;
                index = Math.min(index, count - 1);
                if (start < 0) {
                    start = end = index;
                } else if (index <= end + 1) {
                    end = index;
                } else {
                    selection.addSelectionInterval(start, end);
                    start = end = index;
                }
            }
            if (start >= 0) selection.addSelectionInterval(start, end);
        } finally {
            selection.setValueIsAdjusting(adjusting);
        }
    }

    private static JTable createRowHeadersTable(final JTable userTable, IntFunction<?> rowLabels) {
        final JBTable rowHeadersTable = new JBTable(new RowHeadersTableModel(userTable.getModel().getRowCount(), rowLabels)) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBackground(UIUtil.getPanelBackground());
                setForeground(UIUtil.getLabelForeground());
                setFont(UIUtil.getLabelFont());
                setGridColor(JBColor.border());
                setShowHorizontalLines(true);
                setShowVerticalLines(false);
                setIntercellSpacing(JBUI.size(0, 1));
            }

            @Override
            protected @NotNull JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    @Override
                    public void updateUI() {
                        super.updateUI();
                        CsvTableHeader.applyHeaderStyle(this);
                    }
                };
            }

            @Override
            protected void processMouseEvent(MouseEvent event) {
                if (!handleMouseEvent(event)) super.processMouseEvent(event);
            }

            @Override
            protected void processMouseMotionEvent(MouseEvent event) {
                if (!handleMouseEvent(event)) super.processMouseMotionEvent(event);
            }

            private boolean handleMouseEvent(MouseEvent event) {
                return getClientProperty(ROW_HEADER_MOUSE_HANDLER) instanceof RowHeaderMouseHandler handler &&
                        handler.handler().test(event);
            }

            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);
                if (getClientProperty(ROW_HEADER_OVERLAY) instanceof RowHeaderOverlay overlay) {
                    overlay.painter().accept(graphics, this);
                }
            }
        };

        rowHeadersTable.createDefaultColumnsFromModel();
        rowHeadersTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // make the rows look and behave like headers
        rowHeadersTable.setRowHeight(userTable.getRowHeight());
        rowHeadersTable.getTableHeader().setReorderingAllowed(false);
        rowHeadersTable.setRowSelectionAllowed(true);
        rowHeadersTable.setCellSelectionEnabled(true);
        rowHeadersTable.setFocusable(true);
        rowHeadersTable.setDragEnabled(false);
        rowHeadersTable.setSelectionMode(userTable.getSelectionModel().getSelectionMode());
        rowHeadersTable.getEmptyText().setText("");

        return rowHeadersTable;
    }

    public static void setRowHeaderOverlay(JTable rowHeadersTable, BiConsumer<Graphics, JTable> painter) {
        rowHeadersTable.putClientProperty(ROW_HEADER_OVERLAY, new RowHeaderOverlay(Objects.requireNonNull(painter)));
    }

    /**
     * Returning true consumes the event before Swing's selection and resizing handlers run.
     */
    public static void setRowHeaderMouseHandler(JTable rowHeadersTable, Predicate<MouseEvent> handler) {
        rowHeadersTable.putClientProperty(ROW_HEADER_MOUSE_HANDLER, new RowHeaderMouseHandler(Objects.requireNonNull(handler)));
    }

    private static void installRowHeader(final JScrollPane scrollPane, final JTable rowHeadersTable) {
        scrollPane.setRowHeaderView(rowHeadersTable);
        // set the row header name into the top left corner
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowHeadersTable.getTableHeader());
        Border border = scrollPane.getBorder();
        if (border == null || border instanceof UIResource) {
            scrollPane.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));
        }
    }

    private static void adjustComponents(final JScrollPane scrollPane, final JTable rowHeadersTable,
                                         final JTable userTable, IntSupplier savedWidth, IntConsumer widthChanged,
                                         BooleanSupplier resizing) {
        installRowHeader(scrollPane, rowHeadersTable);

        // label used for rendering
        final JLabel label = new JLabel();
        label.setOpaque(true);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, JBColor.border()),
                JBUI.Borders.empty(3, ROW_NUMBER_CELL_PADDING)));

        scrollPane.getVerticalScrollBar().addAdjustmentListener(
                event -> adjustColumnWidth(savedWidth.getAsInt(), rowHeadersTable, event.getValue()));

        TableColumn numberColumn = rowHeadersTable.getColumnModel().getColumn(0);
        numberColumn.setCellRenderer(new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                label.setText("" + value);
                label.setFont(rowHeadersTable.getFont());

                if (isSelected) {
                    label.setForeground(rowHeadersTable.getSelectionForeground());
                    label.setBackground(rowHeadersTable.getSelectionBackground());
                } else {
                    label.setBackground(rowHeadersTable.getBackground());
                    label.setForeground(rowHeadersTable.getForeground());
                }
                return label;
            }
        });
        numberColumn.addPropertyChangeListener(event -> {
            if ("width".equals(event.getPropertyName()) &&
                    numberColumn.equals(rowHeadersTable.getTableHeader().getResizingColumn())) {
                int width = numberColumn.getWidth();
                widthChanged.accept(width);
                rowHeadersTable.setPreferredScrollableViewportSize(new Dimension(width, 0));
            }
        });

        scrollPane.addComponentListener(new ComponentAdapter() {
            /**
             * Whenever the component is resized need to re-adjust the
             * column width if necessary. This method is also called
             * when the screen is first layed out.
             */
            public void componentResized(ComponentEvent e) {
                adjustColumnWidth(savedWidth.getAsInt(), rowHeadersTable, scrollPane.getVerticalScrollBar().getValue());
            }
        });

        new TableSynchronizer(rowHeadersTable, userTable,
                () -> adjustColumnWidth(savedWidth.getAsInt(), rowHeadersTable, scrollPane.getVerticalScrollBar().getValue()), resizing);
    }

    /**
     * Adds a number column in the row header of the scrollpane, to match rows
     * in the table. Assumes that table has already been added to a scollpane.
     * If the table is not in a scrollpane nothing will happen.
     *
     * @param userTable      - Table to have column added to (if it is in a scrollpane)
     * @param startingNumber - Number to start number column with, typically 0 or 1.
     * @param savedWidth     - Current saved width; zero enables automatic sizing.
     * @param widthChanged   - Persists a user-resized row-number column width.
     * @param rowsResized    - Persists the layout after row resizing finishes.
     */
    public static JTable addNumberColumn(final JTable userTable, int startingNumber, IntSupplier savedWidth,
                                         IntConsumer widthChanged, Runnable rowsResized) {
        JScrollPane scrollPane = getScrollPane(userTable);
        if (scrollPane == null) return null;
        JTable rowHeadersTable = createRowHeadersTable(userTable, row -> startingNumber + row);
        TableRowResizer resizer = new TableRowResizer(rowHeadersTable, userTable, rowsResized);
        adjustComponents(scrollPane, rowHeadersTable, userTable, savedWidth, widthChanged, resizer::isResizing);
        return rowHeadersTable;
    }

    /**
     * Adds a fixed row header of the given width that marks rows with colors instead of numbers; rows without a
     * marker color show the header background. Like {@link #addNumberColumn}, clicking it selects entire rows.
     * Neither its width nor its rows can be resized; callers keep its row heights aligned with the table.
     */
    public static JTable addRowMarkerColumn(final JTable userTable, int width, IntFunction<Color> rowMarkers) {
        JScrollPane scrollPane = getScrollPane(userTable);
        if (scrollPane == null) return null;
        JTable rowHeadersTable = createRowHeadersTable(userTable, row -> "");
        installRowHeader(scrollPane, rowHeadersTable);
        rowHeadersTable.setPreferredScrollableViewportSize(new Dimension(width, 0));
        TableColumn column = rowHeadersTable.getColumnModel().getColumn(0);
        column.setMinWidth(width);
        column.setMaxWidth(width);
        column.setResizable(false);
        JLabel marker = new JLabel();
        marker.setOpaque(true);
        column.setCellRenderer((table, value, isSelected, hasFocus, row, columnIndex) -> {
            Color color = isSelected ? table.getSelectionBackground() : rowMarkers.apply(row);
            marker.setBackground(color == null ? table.getBackground() : color);
            return marker;
        });
        new TableSynchronizer(rowHeadersTable, userTable, rowHeadersTable::repaint, () -> false);
        return rowHeadersTable;
    }

    private static JScrollPane getScrollPane(JTable userTable) {
        if (!(userTable.getParent() instanceof JViewport viewport)
                || !(viewport.getParent() instanceof JScrollPane scrollPane)) {
            return null;
        }
        // Make certain we are the viewPort's view and not, for example, the rowHeaderView of the scrollPane - an implementor of fixed columns might do this.
        if (!Objects.equals(scrollPane.getViewport(), viewport)) return null;
        scrollPane.setColumnHeaderView(userTable.getTableHeader());
        return scrollPane;
    }

    /**
     * Table Model for the row header column. It just has one column (the row labels)
     */
    private static final class RowHeadersTableModel extends AbstractTableModel {
        private int rowCount;
        private final IntFunction<?> rowLabels;

        RowHeadersTableModel(int rowCount, IntFunction<?> rowLabels) {
            this.rowCount = rowCount;
            this.rowLabels = rowLabels;
        }

        public int getRowCount() {
            return rowCount;
        }

        public int getColumnCount() {
            return 1;
        }

        public String getColumnName(int columnIndex) {
            return "";
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return rowLabels.apply(rowIndex);
        }

        Object getLastLabel() {
            return rowLabels.apply(Math.max(0, rowCount - 1));
        }

        void setRowCount(int count) {
            if (rowCount == count) return;
            rowCount = count;
            fireTableDataChanged();
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            // value is generated and should not be manipulated
        }
    }

    /**
     * Uses the saved width, or fits the last visible row label until the user resizes the column.
     *
     * @param savedWidth      - saved column width, or zero for automatic sizing
     * @param rowHeadersTable - single column table in the row header
     * @param scrollBarValue  - vertical position, before the row-header viewport is synchronized
     */
    public static void adjustColumnWidth(int savedWidth, final JTable rowHeadersTable, int scrollBarValue) {
        TableColumn column = rowHeadersTable.getColumnModel().getColumn(0);
        int width = savedWidth;
        if (width == 0) {
            int height = rowHeadersTable.getVisibleRect().height;
            int row = rowHeadersTable.rowAtPoint(new Point(0, scrollBarValue + height));
            RowHeadersTableModel model = (RowHeadersTableModel) rowHeadersTable.getModel();
            Object value = row == -1 ? model.getLastLabel() : model.getValueAt(row, 0);
            Component renderer = column.getCellRenderer().getTableCellRendererComponent(
                    rowHeadersTable, value, false, false, row, 0);
            width = renderer.getPreferredSize().width;
        }
        width = Math.max(column.getMinWidth(), width);
        column.setPreferredWidth(width);
        column.setWidth(width);
        rowHeadersTable.setPreferredScrollableViewportSize(new Dimension(width, 0));
    }

    /**
     * Listener that joins the two tables; the main table, and the single column row number table. When either
     * are moved or selected, then the affect is passed on to the other table.
     */
    private static final class TableSynchronizer implements ListSelectionListener, TableModelListener {

        private final JTable rowHeadersTable;
        private final JTable userTable;
        private final Runnable rowsChanged;
        private final BooleanSupplier resizing;
        private boolean synchronizing;

        private TableSynchronizer(JTable rowHeadersTableArg, JTable userTableArg, Runnable rowsChangedArg, BooleanSupplier resizingArg) {
            this.userTable = userTableArg;
            this.rowHeadersTable = rowHeadersTableArg;
            this.rowsChanged = rowsChangedArg;
            this.resizing = resizingArg;

            Container p = userTableArg.getParent();
            JViewport userTableViewPort = (JViewport) p;

            Container p2 = rowHeadersTableArg.getParent();
            JViewport rowHeadersViewPort = (JViewport) p2;

            Point newPosition = userTableViewPort.getViewPosition();
            rowHeadersViewPort.setViewPosition(newPosition);

            rowHeadersTable.getSelectionModel().addListSelectionListener(this);
            userTable.getSelectionModel().addListSelectionListener(this);
            userTable.getModel().addTableModelListener(this);
        }

        public void valueChanged(ListSelectionEvent e) {
            if (synchronizing || e.getValueIsAdjusting() || resizing.getAsBoolean()) return;
            synchronizing = true;
            try {
                if (userTable.getSelectionModel().equals(e.getSource())) {
                    setSelection(rowHeadersTable.getSelectionModel(), userTable.getSelectedRows(), rowHeadersTable.getRowCount());
                } else {
                    int[] rows = rowHeadersTable.getSelectedRows();
                    setSelection(userTable.getSelectionModel(), rows, userTable.getRowCount());
                    if (rows.length > 0 && userTable.getColumnCount() > 0) {
                        userTable.setColumnSelectionInterval(0, userTable.getColumnCount() - 1);
                    }
                }
            } finally {
                synchronizing = false;
            }
        }

        @Override
        public void tableChanged(TableModelEvent e) {
            RowHeadersTableModel m = (RowHeadersTableModel) rowHeadersTable.getModel();
            m.setRowCount(userTable.getRowCount());
            // Labels are derived from the table's rows and may change without a change of the row count.
            rowsChanged.run();
        }
    }

    public static class TableRowResizer extends MouseInputAdapter {
        public static Cursor RESIZE_CURSOR = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);

        private int mouseYOffset;
        private int resizingRow = -1;
        private Cursor otherCursor = RESIZE_CURSOR;
        private final JTable rowHeadersTable;
        private final JTable userTable;
        private final Runnable rowsResized;

        public TableRowResizer(JTable rowHeadersTableArg, JTable userTableArg, Runnable rowsResized) {
            this.userTable = userTableArg;
            this.rowHeadersTable = rowHeadersTableArg;
            this.rowsResized = rowsResized;
            rowHeadersTable.addMouseListener(this);
            rowHeadersTable.addMouseMotionListener(this);
        }

        private int getResizingRow(Point p) {
            return getResizingRow(p, rowHeadersTable.rowAtPoint(p));
        }

        public boolean isResizing() {
            return this.resizingRow != -1;
        }

        private int getResizingRow(Point p, int row) {
            if (row == -1) {
                return -1;
            }
            int col = rowHeadersTable.columnAtPoint(p);
            if (col == -1)
                return -1;
            Rectangle r = rowHeadersTable.getCellRect(row, col, true);
            r.grow(0, -3);
            if (r.contains(p))
                return -1;

            int midPoint = r.y + r.height / 2;

            return (p.y < midPoint) ? row - 1 : row;
        }

        public void mousePressed(MouseEvent e) {
            if (!rowHeadersTable.isEnabled()) return;
            Point p = e.getPoint();

            resizingRow = getResizingRow(p);
            mouseYOffset = p.y - rowHeadersTable.getRowHeight(resizingRow);
        }

        private void swapCursor() {
            Cursor tmp = rowHeadersTable.getCursor();
            rowHeadersTable.setCursor(otherCursor);
            otherCursor = tmp;
        }

        public void mouseMoved(MouseEvent e) {
            if (resizingRow == -1 &&
                    (getResizingRow(e.getPoint()) >= 0)
                            != (rowHeadersTable.getCursor() == RESIZE_CURSOR)) {
                swapCursor();
            }
        }

        public void mouseDragged(MouseEvent e) {
            int mouseY = e.getY();

            if (resizingRow >= 0) {
                int newHeight = mouseY - mouseYOffset;
                if (newHeight > 0) {
                    userTable.setRowHeight(resizingRow, newHeight);
                    rowHeadersTable.setRowHeight(resizingRow, newHeight);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            int mouseY = e.getY();

            if (resizingRow >= 0) {
                int newHeight = mouseY - mouseYOffset;
                if (newHeight > 0) {
                    rowHeadersTable.setRowHeight(newHeight);
                    userTable.setRowHeight(newHeight);
                    rowsResized.run();
                }
            }
            resizingRow = -1;
        }
    }
}
