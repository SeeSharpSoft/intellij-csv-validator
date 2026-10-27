package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;

public class CsvTableHeader extends JTableHeader {
    private boolean painting;

    public CsvTableHeader(TableColumnModel columnModel) {
        super(columnModel);
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
