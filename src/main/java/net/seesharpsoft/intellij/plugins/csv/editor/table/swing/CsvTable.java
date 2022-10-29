package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ui.table.JBTable;
import net.seesharpsoft.intellij.ui.EditableCellFocusAction;
import net.seesharpsoft.intellij.util.Suspendable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

public class CsvTable extends JBTable implements Suspendable {

    public static class CommentColumn {}

    private boolean myPaintRequestedWhileSuspended = false;

    public CsvTable(TableModel model) {
        super(model);
        setUI(new MultiSpanCellTableUI());
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("TAB"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("shift TAB"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("RIGHT"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("LEFT"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("UP"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("DOWN"));
//        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("ctrl shift ENTER"));
//        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("shift ENTER"));
    }

    public void resume() {
        Suspendable.super.resume();
        if (!isSuspended() && myPaintRequestedWhileSuspended) {
            myPaintRequestedWhileSuspended = false;
            this.repaint();
        }
    }

    @Override
    public CsvTableModelSwing getModel() {
        return (CsvTableModelSwing) super.getModel();
    }

    public boolean isCommentRow(int row) {
        return getModel().isCommentRow(convertRowIndexToModel(row));
    }

    @Override
    public void paint(@NotNull Graphics g) {
        if (isSuspended()) {
            myPaintRequestedWhileSuspended = true;
            return;
        }
        super.paint(g);
    }

    @Override
    public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
        Rectangle rect = super.getCellRect(row, column, includeSpacing);
        if (isCommentRow(row)) {
            rect.width = convertColumnIndexToModel(column) == 0 ? this.getWidth() : 0;
            rect.x = 0;
        }
        return rect;
    }

    @Override
    public int columnAtPoint(Point point) {
        int rowAtPoint = super.rowAtPoint(point);
        return isCommentRow(rowAtPoint) ? 0 : super.columnAtPoint(point);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (isCommentRow(row)) {
            return getDefaultRenderer(CommentColumn.class);
        }
        return super.getCellRenderer(row, column);
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        if (isCommentRow(row) && convertColumnIndexToModel(column) != 0) {
            return null;
        }
        return super.prepareRenderer(renderer, row, column);
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        if (isCommentRow(row)) {
            return getDefaultEditor(CommentColumn.class);
        }
        return super.getCellEditor(row, column);
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        if (isCommentRow(row) && convertColumnIndexToModel(column) != 0) {
            return null;
        }
        return super.prepareEditor(editor, row, column);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return isCommentRow(row) && convertColumnIndexToModel(column) != 0 ? false : super.isCellEditable(row, column);
    }

    public class MultiSpanCellTableUI extends BasicTableUI {

        public void paint(Graphics g, JComponent c) {
            Rectangle clip = g.getClipBounds();

            Rectangle bounds = table.getBounds();
            // account for the fact that the graphics has already been translated
            // into the table's bounds
            bounds.x = bounds.y = 0;

            if (table.getRowCount() <= 0 || table.getColumnCount() <= 0 ||
                    // this check prevents us from painting the entire table
                    // when the clip doesn't intersect our bounds at all
                    !bounds.intersects(clip)) {

                super.paint(g, c);
                return;
            }

            boolean ltr = table.getComponentOrientation().isLeftToRight();
            Point upperLeft, lowerRight;
            // compute the visible part of table which needs to be painted
            Rectangle visibleBounds = clip.intersection(bounds);
            upperLeft = visibleBounds.getLocation();
            lowerRight = new Point(visibleBounds.x + visibleBounds.width - 1,
                    visibleBounds.y + visibleBounds.height - 1);

            int rMin = table.rowAtPoint(upperLeft);
            int rMax = table.rowAtPoint(lowerRight);
            // This should never happen (as long as our bounds intersect the clip,
            // which is why we bail above if that is the case).
            if (rMin == -1) {
                rMin = 0;
            }
            // If the table does not have enough rows to fill the view we'll get -1.
            // (We could also get -1 if our bounds don't intersect the clip,
            // which is why we bail above if that is the case).
            // Replace this with the index of the last row.
            if (rMax == -1) {
                rMax = table.getRowCount()-1;
            }

            // For FIT_WIDTH, all columns should be printed irrespective of
            // how many columns are visible. So, we used clip which is already set to
            // total col width instead of visible region
            // Since JTable.PrintMode is not accessible
            // from here, we aet "Table.printMode" in TablePrintable#print and
            // access from here.
            Object printMode = table.getClientProperty("Table.printMode");
            if ((printMode == JTable.PrintMode.FIT_WIDTH)) {
                upperLeft = clip.getLocation();
                lowerRight = new Point(clip.x + clip.width - 1,
                        clip.y + clip.height - 1);
            }
            int cMin = table.columnAtPoint(ltr ? upperLeft : lowerRight);
            int cMax = table.columnAtPoint(ltr ? lowerRight : upperLeft);
            // This should never happen.
            if (cMin == -1) {
                cMin = 0;
            }
            // If the table does not have enough columns to fill the view we'll get -1.
            // Replace this with the index of the last column.
            if (cMax == -1) {
                cMax = table.getColumnCount()-1;
            }

            Container comp = SwingUtilities.getUnwrappedParent(table);
            if (comp != null) {
                comp = comp.getParent();
            }

            if (comp != null && !(comp instanceof JViewport) && !(comp instanceof JScrollPane)) {
                // We did rMax-1 to paint the same number of rows that are drawn on console
                // otherwise 1 extra row is printed per page than that are displayed
                // when there is no scrollPane and we do printing of table
                // but not when rmax is already pointing to index of last row
                // and if there is any selected rows
                if (rMax != (table.getRowCount() - 1) &&
                        (table.getSelectedRow() == -1)) {
                    // Do not decrement rMax if rMax becomes
                    // less than or equal to rMin
                    // else cells will not be painted
                    if (rMax - rMin > 1) {
                        rMax = rMax - 1;
                    }
                }
            }

            Rectangle oldClipBounds = g.getClipBounds();
            Rectangle clipBounds    = new Rectangle(oldClipBounds);
            int tableWidth   = table.getColumnModel().getTotalColumnWidth();
            clipBounds.width = Math.min(clipBounds.width, tableWidth);
//            g.setClip(clipBounds);

            int firstIndex = rMin;// table.rowAtPoint(new Point(0, clipBounds.y));
            int  lastIndex = rMax;//table.getRowCount() - 1;

            Rectangle rowRect = new Rectangle(0,0,
                    tableWidth, table.getRowHeight() + table.getRowMargin());
            rowRect.y = firstIndex*rowRect.height;

            for (int index = firstIndex; index <= lastIndex; index++) {
//                if (rowRect.intersects(clipBounds)) {
                    paintRow(g, index, cMin, cMax);
//                }
                rowRect.y += rowRect.height;
            }
//            g.setClip(oldClipBounds);


            // Remove any renderers that may be left in the rendererPane.
            rendererPane.removeAll();
        }

        private void paintRow(Graphics g, int row, int colMin, int colMax) {
            Rectangle clipBounds = g.getClipBounds();
            boolean drawn = false;
            if (isCommentRow(row)) {
                Rectangle cellRect = table.getCellRect(row, 0,true);
//                cellRect.width = table.getWidth();
                paintCell(g, cellRect, row, 0);
            } else {
                for (int column = colMin; column <= colMax; column++) {
                    Rectangle cellRect = table.getCellRect(row, column, true);
                    paintCell(g, cellRect, row, column);
//                    if (cellRect.intersects(clipBounds)) {
//                        drawn = true;
//                        paintCell(g, cellRect, row, column);
//                    } else if(drawn) {
//                        break;
//                    }
                }
            }
        }

        private void paintCell(Graphics g, Rectangle cellRect, int row, int column) {
            int spacingHeight = table.getRowMargin();
            int spacingWidth  = table.getColumnModel().getColumnMargin();

            Color c = g.getColor();
            g.setColor(table.getGridColor());
            g.drawRect(cellRect.x,cellRect.y,cellRect.width-1,cellRect.height-1);
            g.setColor(c);

            cellRect.setBounds(cellRect.x + spacingWidth/2, cellRect.y + spacingHeight/2,
                    cellRect.width - spacingWidth, cellRect.height - spacingHeight);

            if (table.isEditing() && table.getEditingRow() == row &&
                    table.getEditingColumn() == column) {
                Component component = table.getEditorComponent();
                component.setBounds(cellRect);
                component.validate();
            }
            else {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component component = table.prepareRenderer(renderer, row, column);
                if (component.getParent() == null) {
                    rendererPane.add(component);
                }
                rendererPane.paintComponent(g, component, table, cellRect.x, cellRect.y,
                        cellRect.width, cellRect.height, true);
            }
        }
    }

}
