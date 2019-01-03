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
package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.UIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;


/**
 * TableRowUtilities. Utility for adding a row column to a JTable.
 *
 * https://github.com/oliverwatkins/swing_library
 *
 * Changes for CSV-Plugin:
 * - addNumberColumn method returns the created row header table
 * - use JBTable as RowHeaderTable instead of (deprecated) JTable
 * - removed System.out
 * - several code clean ups
 *
 * @author Oliver Watkins, Martin Sommer
 */
public final class TableRowUtilities {

    public static final int ROW_NUMBER_CELL_PADDING = 4;

    private TableRowUtilities() {
        // static helper
    }

    /**
     * Adds a number column in the row header of the scrollpane, to match rows
     * in the table. Assumes that table has already been added to a scollpane.
     * If the table is not in a scrollpane nothing will happen.
     *
     * @param userTable - Table to have column added to (if it is in a scrollpane)
     * @param startingNumber - Number to start number column with, typically 0 or 1.
     */
    public static JTable addNumberColumn(final JTable userTable, int startingNumber, boolean isRowSelectable) {
        Container parentContainer = userTable.getParent();

        if (parentContainer instanceof JViewport) {
            Container parentParentContainer = parentContainer.getParent();

            if (parentParentContainer instanceof JScrollPane) {
                final JScrollPane scrollPane = (JScrollPane) parentParentContainer;

                // Make certain we are the viewPort's view and not, for  example, the rowHeaderView of the scrollPane - an implementor of fixed columns might do this.
                JViewport viewport = scrollPane.getViewport();

                if (viewport == null || viewport.getView() != userTable) {
                    return null;
                }

                JTableHeader tableHeader = userTable.getTableHeader();
                scrollPane.setColumnHeaderView(tableHeader);

                final JTable rowHeadersTable = new JBTable(new RowHeadersTableModel(userTable.getModel().getRowCount(), startingNumber));

                // rowHeadersTable.getModel().addTableModelListener()
                userTable.getModel().addTableModelListener(new TableModelListener() {
                    public void tableChanged(TableModelEvent e) {
                        RowHeadersTableModel m = (RowHeadersTableModel) rowHeadersTable.getModel();

                        if (userTable.getRowCount() != m.getRowCount()) {
                            if (userTable.getRowCount() > m.getRowCount()) {

                                int rowDiff = userTable.getRowCount() - m.getRowCount();

                                for (int i = 0; i < rowDiff; i++) {
                                    m.addNumber();
                                }
                            } else if (userTable.getRowCount() < m.getRowCount()) {

                                int rowDiff = m.getRowCount() - userTable.getRowCount();

                                for (int i = 0; i < rowDiff; i++) {
                                    m.removeNumber();
                                }
                            }
                            m.fireTableDataChanged();
                        }
                    }
                });

                // label used for rendering and for
                final JLabel label = new JLabel();

                scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
                    public void adjustmentValueChanged(AdjustmentEvent e) {
                        int scrollBarValue = e.getValue();

                        adjustColumnWidth(rowHeadersTable, label, scrollBarValue);
                    }
                });

                // this is where you set the width of the row headers
                rowHeadersTable.createDefaultColumnsFromModel();

                // make the rows look like headers
                rowHeadersTable.setBackground(rowHeadersTable.getTableHeader().getBackground());
                rowHeadersTable.setForeground(rowHeadersTable.getTableHeader().getForeground());
                rowHeadersTable.setFont(rowHeadersTable.getTableHeader().getFont());

                rowHeadersTable.setRowHeight(userTable.getRowHeight());

                rowHeadersTable.getTableHeader().setReorderingAllowed(false);

                // If selectable then change the colouring in the renderer
                if (isRowSelectable) {
                    // adding a renderer
                    rowHeadersTable.getColumnModel().getColumn(0).setCellRenderer(new TableCellRenderer() {
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                       boolean hasFocus, int row, int column) {
                            label.setText("" + value);

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

                    rowHeadersTable.setRowSelectionAllowed(true);
                    rowHeadersTable.setCellSelectionEnabled(true);
                    rowHeadersTable.setFocusable(true);
                    rowHeadersTable.setDragEnabled(true);

                }
                scrollPane.setRowHeaderView(rowHeadersTable);

                // set the row header name into the top left corner
                scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowHeadersTable.getTableHeader());

                Border border = scrollPane.getBorder();
                if (border == null || border instanceof UIResource) {
                    scrollPane.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));
                }

                scrollPane.addComponentListener(new ComponentAdapter() {
                    /**
                     * Whenever the component is resized need to re-adjust the
                     * column width if necessary. This method is also called
                     * when the screen is first layed out.
                     */
                    public void componentResized(ComponentEvent e) {
                        adjustColumnWidth(rowHeadersTable, label, scrollPane.getVerticalScrollBar().getValue());
                    }
                });

                rowHeadersTable.setSelectionMode(userTable.getSelectionModel().getSelectionMode());

                TableListener.create(rowHeadersTable, userTable);

                return rowHeadersTable;
            }
        }
        return null;
    }

    /**
     * Table Model for the row number column. It just has one column (the numbers)
     */
    private static final class RowHeadersTableModel extends AbstractTableModel {
        private ArrayList<Integer> numbersList = new ArrayList<Integer>();
        private int startNumber;


        /**
         * Initialize model
         *
         * @param maxNumber determined by JTable row size
         * @param startingNumber usually zero or 1
         */
        RowHeadersTableModel(int maxNumber, int startingNumber) {
            // start at starting number and then go to row count (plus starting number amount)
            this.startNumber = startingNumber;
            int j = 0;
            for (int i = startingNumber; i < maxNumber + startNumber; i++) {
                numbersList.add(new Integer(j + startNumber));
                j++;
            }
        }

        public int getRowCount() {
            return numbersList != null ? numbersList.size() : 0;
        }

        public int getMaxIntValue() {
            if (numbersList != null && numbersList.size() != 0) {
                Integer integer = (Integer) getValueAt(numbersList.size() - 1, 0);
                return integer.intValue();
            }
            return 0;
        }

        public int getColumnCount() {
            return 1;
        }

        public String getColumnName(int columnIndex) {
            return "";
        }

        public Class getColumnClass(int columnIndex) {
            return Integer.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return numbersList.get(rowIndex);
        }

        public void addNumber() {
            if (numbersList.isEmpty()) {
                numbersList.add(0, new Integer(startNumber));
            } else {
                Integer maxNum = numbersList.get(numbersList.size() - 1);
                numbersList.add(numbersList.size(), maxNum.intValue() + 1);
            }
            this.fireTableDataChanged();
        }

        public void removeNumber() {
            numbersList.remove(numbersList.size() - 1);
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        public void addTableModelListener(TableModelListener l) {
            super.addTableModelListener(l);
        }

        public void removeTableModelListener(TableModelListener l) {
            super.removeTableModelListener(l);
        }

    }

    /**
     * Adjusts the column width of the row headers table containing the number
     * column. The font metrics are extracted from the label of the row at the
     * bottom of the viewport and used to determining the appropriate width.
     *
     * The reason why this method is important, is that when the row number increases by an extra digit
     * the column needs to get wider. It also needs to shrink when scrolling to smaller digit numbers.
     *
     * @param rowHeadersTable - single column table in the row header
     * @param label - label used to get font metrics
     * @param scrollBarValue - int value for determining point of lowest row
     */
    private static void adjustColumnWidth(final JTable rowHeadersTable, final JLabel label, int scrollBarValue) {

        label.setFont(rowHeadersTable.getFont());
        label.setOpaque(true);
        label.setHorizontalAlignment(JLabel.CENTER);

        int v = rowHeadersTable.getVisibleRect().height;

        int row = rowHeadersTable.rowAtPoint(new Point(0, v + scrollBarValue));

        Integer modelValue = null;
        if (row != -1) {
            modelValue = (Integer) rowHeadersTable.getModel().getValueAt(row, 0);
        } else {
            RowHeadersTableModel tm = (RowHeadersTableModel) rowHeadersTable.getModel();
            modelValue = tm.getMaxIntValue();
        }

        label.setText("" + modelValue);
        FontMetrics fontMetrics = label.getFontMetrics(label.getFont());

        int widthFactor;
        int totalPadding = 2 * ROW_NUMBER_CELL_PADDING;

        if (fontMetrics != null && label.getText() != null) {
            widthFactor = fontMetrics.stringWidth(label.getText());

            rowHeadersTable.setPreferredScrollableViewportSize(new Dimension(widthFactor + totalPadding, 0)); // height is ignored
            rowHeadersTable.repaint();
        }
    }

    /**
     * Listener that joins the two tables; the main table, and the single column row number table. When either
     * are moved or selected, then the affect is passed on to the other table.
     */
    private static final class TableListener implements ListSelectionListener {

        private JTable rowHeadersTable;
        private JTable userTable;
        private JViewport userTableViewPort;
        private JViewport rowHeadersViewPort;

        public static TableListener create(JTable rowHeadersTableArg, JTable userTableArg) {
            return new TableListener(rowHeadersTableArg, userTableArg);
        }

        private TableListener(JTable rowHeadersTableArg, JTable userTableArg) {
            this.userTable = userTableArg;
            this.rowHeadersTable = rowHeadersTableArg;

            Container p = userTableArg.getParent();
            userTableViewPort = (JViewport) p;

            Container p2 = rowHeadersTableArg.getParent();
            rowHeadersViewPort = (JViewport) p2;

            Point newPosition = userTableViewPort.getViewPosition();
            rowHeadersViewPort.setViewPosition(newPosition);

            rowHeadersTable.getSelectionModel().addListSelectionListener(this);
            userTable.getSelectionModel().addListSelectionListener(this);

        }

        public void valueChanged(ListSelectionEvent e) {
            if (e.getSource() == userTable.getSelectionModel()) {
                rowHeadersTable.getSelectionModel().removeListSelectionListener(this);
                rowHeadersTable.getSelectionModel().clearSelection();

                int[] rows = userTable.getSelectedRows();

                for (int i = 0; i < rows.length; i++) {
                    rowHeadersTable.getSelectionModel().addSelectionInterval(rows[i], rows[i]);

                }

                rowHeadersTable.getSelectionModel().addListSelectionListener(this);
            } else if (e.getSource() == rowHeadersTable.getSelectionModel()) {
                boolean isColumnSelectionAllowed = userTable.getColumnSelectionAllowed();
                boolean isRowSelectionAllowed = userTable.getRowSelectionAllowed();
                boolean isCellSelectionAllowed = userTable.getCellSelectionEnabled();

                userTable.getSelectionModel().removeListSelectionListener(this);
                userTable.getSelectionModel().clearSelection();

                int[] rows = rowHeadersTable.getSelectedRows();

                if (isRowSelectionAllowed && !isCellSelectionAllowed && !isColumnSelectionAllowed) {
                    for (int i = 0; i < rows.length; i++) {
                        userTable.addRowSelectionInterval(rows[i], rows[i]);
                        userTableViewPort.setViewPosition(rowHeadersViewPort.getViewPosition());

                    }
                } else {
                    // looks cleaner
                    userTableViewPort.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);

                    for (int i = 0; i < rows.length; i++) {
                        if (i == 0) {
                            // need to create row first with change selection
                            userTable.changeSelection(rows[i], 0, false, false);
                            userTable.changeSelection(rows[i], userTable.getColumnCount(), false, true);

                        } else {
                            userTable.addRowSelectionInterval(rows[i], rows[i]);
                        }
                    }
                }
                // re-adding the listener to the user table
                userTable.getSelectionModel().addListSelectionListener(this);
            }
        }
    }
}
