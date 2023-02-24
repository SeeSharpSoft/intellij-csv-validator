package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ui.table.JBTable;
import net.seesharpsoft.intellij.ui.EditableCellFocusAction;

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
        TableUI newUI = ui instanceof MultiSpanCellTableUI ? ui : new MultiSpanCellTableUI();
        super.setUI(newUI);
    }

    @Override
    public MultiSpanCellTableUI getUI() {
        return (MultiSpanCellTableUI) super.getUI();
    }

    @Override
    public CsvTableModelSwing getModel() {
        return (CsvTableModelSwing) super.getModel();
    }

    public boolean isCommentRow(int row) {
        return getModel().isCommentRow(convertRowIndexToModel(row));
    }

    @Override
    public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
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
    public int columnAtPoint(Point point) {
        if (getUI().isPainting()) {
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
        if (isCommentRow(row)) {
            return getDefaultRenderer(CommentColumn.class);
        }
        return super.getCellRenderer(row, column);
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

        int paintCounter = 0;

        public boolean isPainting() {
            return paintCounter != 0;
        }

        public void paint(Graphics g, JComponent c) {
            paintCounter++;

            try {
                super.paint(g, c);

                Rectangle clip = g.getClipBounds();

                Rectangle bounds = table.getBounds();
                // account for the fact that the graphics has already been translated
                // into the table's bounds
                bounds.x = bounds.y = 0;

                if (table.getRowCount() <= 0 || table.getColumnCount() <= 0 ||
                        // this check prevents us from painting the entire table
                        // when the clip doesn't intersect our bounds at all
                        !bounds.intersects(clip)) {
                    return;
                }

                // compute the visible part of table which needs to be painted
                Rectangle visibleBounds = clip.intersection(bounds);
                Point upperLeft = visibleBounds.getLocation();
                Point lowerRight = new Point(visibleBounds.x + visibleBounds.width - 1,
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
                    rMax = table.getRowCount() - 1;
                }

                for (int row = rMin; row <= rMax; row++) {
                    if (isCommentRow(row)) {
                        paintCommentRow(g, row);
                    }
                }
            } finally {
                paintCounter--;
            }
        }

        private void paintCommentRow(Graphics g, int row) {
            Rectangle cellRect = table.getCellRect(row, 0, true);
            cellRect.width = table.getWidth();
            paintCell(g, cellRect, row, 0);
        }

        private void paintCell(Graphics g, Rectangle cellRect, int row, int column) {
            int spacingHeight = table.getRowMargin();
            int spacingWidth = table.getColumnModel().getColumnMargin();

            Color c = g.getColor();
            g.setColor(table.getGridColor());
            g.drawRect(cellRect.x, cellRect.y, cellRect.width - 1, cellRect.height - 1);
            g.setColor(c);

            cellRect.setBounds(cellRect.x + spacingWidth / 2, cellRect.y + spacingHeight / 2,
                    cellRect.width - spacingWidth, cellRect.height - spacingHeight);

            if (table.isEditing() && table.getEditingRow() == row &&
                    table.getEditingColumn() == column) {
                Component component = table.getEditorComponent();
                component.setBounds(cellRect);
                component.validate();
            } else {
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
