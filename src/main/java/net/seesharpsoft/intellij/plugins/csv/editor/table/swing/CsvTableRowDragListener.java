package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.dnd.DragSource;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

final class CsvTableRowDragListener extends MouseInputAdapter {
    private final CsvTableEditorSwing editor;
    private final JTable rowHeaders;
    private int sourceRow = -1;
    private int startY;
    private boolean dragging;
    private int targetRow = -1;
    private Cursor tableCursor;
    private BufferedImage dragPreview;

    CsvTableRowDragListener(CsvTableEditorSwing editor, JTable rowHeaders) {
        this.editor = editor;
        this.rowHeaders = rowHeaders;
        editor.getTable().putClientProperty(CsvTableRowDragListener.class, this);
        rowHeaders.putClientProperty(CsvTableRowDragListener.class, this);
        rowHeaders.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "csv.cancel.row.drag");
        rowHeaders.getActionMap().put("csv.cancel.row.drag", new AbstractAction() {
            @Override
            public boolean isEnabled() {
                return sourceRow >= 0;
            }

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                cancelDrag();
            }
        });
    }

    @Override
    public void mousePressed(MouseEvent event) {
        cancelDrag();
        if (!SwingUtilities.isLeftMouseButton(event) || event.isPopupTrigger() || event.isShiftDown() || event.isControlDown() ||
                event.isMetaDown() || !editor.isEditable() || editor.getTableModel().hasErrors()) return;
        int row = draggableRow(event.getPoint());
        if (row < 0 || !editor.stopCellEditing()) return;
        sourceRow = row;
        startY = event.getY();
        rowHeaders.setRowSelectionInterval(row, row);
    }

    private int draggableRow(Point point) {
        int row = rowHeaders.rowAtPoint(point);
        if (row < 0 || row >= editor.getTableModel().getMovableRowCount()) return -1;
        Rectangle bounds = rowHeaders.getCellRect(row, 0, true);
        bounds.grow(0, -3);
        return bounds.contains(point) ? row : -1;
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        if (rowHeaders.getCursor().getType() == Cursor.N_RESIZE_CURSOR) return;
        boolean movable = editor.isEditable() && draggableRow(event.getPoint()) >= 0 &&
                !event.isShiftDown() && !event.isControlDown() && !event.isMetaDown();
        rowHeaders.setCursor(Cursor.getPredefinedCursor(movable ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (sourceRow < 0) return;
        if (!editor.isEditable() || editor.getTableModel().hasErrors() ||
                sourceRow >= editor.getTableModel().getMovableRowCount()) {
            cancelDrag();
            return;
        }
        if (!dragging && Math.abs(event.getY() - startY) < DragSource.getDragThreshold()) return;
        CsvTable table = editor.getTable();
        if (!dragging) {
            tableCursor = table.getCursor();
            rowHeaders.requestFocusInWindow();
            table.getSelectionModel().setValueIsAdjusting(true);
            table.setRowSelectionInterval(sourceRow, sourceRow);
            if (table.getColumnCount() > 0) table.setColumnSelectionInterval(0, table.getColumnCount() - 1);
            rowHeaders.getAccessibleContext().setAccessibleDescription(
                    String.format(CsvPluginManager.getLocalizedText("table.row.dragging"), sourceRow + 1));
            // Capture before enabling feedback so the preview cannot capture itself.
            dragPreview = createDragPreview(table, sourceRow);
            dragging = true;
        }
        rowHeaders.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        table.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        targetRow = targetRow(event);
        rowHeaders.setRowSelectionInterval(sourceRow, sourceRow);
        Rectangle target = table.getCellRect(targetRow, 0, true);
        target.x = table.getVisibleRect().x;
        target.width = table.getVisibleRect().width;
        table.scrollRectToVisible(target);
        table.repaint();
        rowHeaders.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        int from = sourceRow;
        int to = dragging ? targetRow(event) : -1;
        cancelDrag();
        if (from >= 0 && to >= 0) editor.moveRow(from, to);
    }

    void cancelDrag() {
        if (sourceRow < 0 && !dragging) return;
        sourceRow = targetRow = -1;
        if (dragging) {
            editor.getTable().setCursor(tableCursor);
            rowHeaders.setCursor(Cursor.getDefaultCursor());
            editor.getTable().getSelectionModel().setValueIsAdjusting(false);
            rowHeaders.getAccessibleContext().setAccessibleDescription(null);
        }
        dragging = false;
        dragPreview = null;
        rowHeaders.getSelectionModel().setValueIsAdjusting(false);
        editor.getTable().repaint();
        rowHeaders.repaint();
    }

    static BufferedImage createDragPreview(CsvTable table, int sourceRow) {
        int padding = JBUI.scale(6);
        Rectangle visible = table.getVisibleRect();
        Rectangle source = table.getCellRect(sourceRow, 0, true);
        Font font = table.getFont().deriveFont(Font.BOLD);
        FontMetrics metrics = table.getFontMetrics(font);
        String number = Integer.toString(sourceRow + 1);
        int width = Math.max(1, Math.min(JBUI.scale(400), visible.width - padding * 2));
        int height = Math.max(metrics.getHeight() + padding * 2, Math.min(source.height, JBUI.scale(56)));
        int badgeWidth = Math.max(JBUI.scale(32), metrics.stringWidth(number) + padding * 2);
        int arc = JBUI.scale(8);
        BufferedImage image = ImageUtil.createImage(table.getGraphicsConfiguration(), width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.clip(new RoundRectangle2D.Float(0, 0, width, height, arc, arc));
            g.setColor(table.getSelectionBackground());
            g.fillRect(0, 0, width, height);
            if (width > badgeWidth + 1) {
                int cellTop = Math.max(1, (height - source.height) / 2);
                Graphics cells = g.create(badgeWidth, cellTop, width - badgeWidth - 1, Math.min(source.height, height - 2));
                try {
                    cells.translate(-visible.x, -source.y);
                    table.paint(cells);
                } finally {
                    cells.dispose();
                }
            }
            g.setColor(dragAccent(table));
            g.fillRect(0, 0, badgeWidth, height);
            g.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
            g.setColor(table.getSelectionForeground());
            g.setFont(font);
            g.drawString(number, (badgeWidth - metrics.stringWidth(number)) / 2,
                    (height - metrics.getHeight()) / 2 + metrics.getAscent());
            int fadeStart = Math.max(badgeWidth, width - JBUI.scale(64));
            if (fadeStart < width - 1) {
                g.setComposite(AlphaComposite.DstIn);
                g.setPaint(new GradientPaint(fadeStart, 0, Color.BLACK, width - 1, 0, new Color(0, 0, 0, 0)));
                g.fillRect(fadeStart, 0, width - fadeStart, height);
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    private static Color dragAccent(JTable table) {
        Color accent = UIManager.getColor("Table.dropLineColor");
        return accent == null ? table.getSelectionBackground() : accent;
    }

    void paintFeedback(Graphics graphics, JTable target) {
        if (!dragging || sourceRow >= target.getRowCount() || targetRow < 0) return;
        Rectangle visible = target.getVisibleRect().intersection(new Rectangle(0, 0, target.getWidth(), target.getHeight()));
        if (visible.isEmpty()) return;
        Graphics g = graphics.create();
        try {
            g.clipRect(visible.x, visible.y, visible.width, visible.height);
            paintSourceRow(g, target, visible);
            Rectangle destination = target.getCellRect(targetRow, 0, true);
            int y = destination.y + (targetRow > sourceRow ? destination.height : 0);
            boolean rowHeader = rowHeaders.equals(target);
            if (targetRow != sourceRow) paintDropLine(g, visible, y, rowHeader);
            if (!rowHeader && dragPreview != null) paintDragPreview(g, visible, y);
        } finally {
            g.dispose();
        }
    }

    private void paintSourceRow(Graphics g, JTable target, Rectangle visible) {
        Color accent = dragAccent(target);
        Rectangle source = target.getCellRect(sourceRow, 0, true);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
        g.fillRect(visible.x, source.y, visible.width, source.height);
        g.setColor(accent);
        g.drawRect(visible.x, source.y, visible.width - 1, source.height - 1);
    }

    private static void paintDropLine(Graphics g, Rectangle visible, int y, boolean withArrow) {
        int thickness = JBUI.scale(3);
        g.fillRect(visible.x, y - thickness / 2, visible.width, thickness);
        if (!withArrow) return;
        int right = visible.x + visible.width - 1;
        int arrow = JBUI.scale(6);
        g.fillPolygon(new int[]{right, right - arrow, right - arrow}, new int[]{y, y - arrow, y + arrow}, 3);
    }

    private void paintDragPreview(Graphics g, Rectangle visible, int y) {
        int padding = JBUI.scale(6);
        int height = ImageUtil.getUserHeight(dragPreview);
        int top = y + padding;
        if (top + height > visible.y + visible.height - padding) top = y - height - padding;
        top = Math.max(visible.y + padding, top);
        UIUtil.drawImage(g, dragPreview, visible.x + padding, top, null);
    }

    private int targetRow(MouseEvent event) {
        int row = rowHeaders.rowAtPoint(event.getPoint());
        int last = editor.getTableModel().getMovableRowCount() - 1;
        return row < 0 ? (event.getY() < 0 ? 0 : last) : Math.min(row, last);
    }
}
