package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.ui.EditableCellFocusAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
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

    private int paintCounter;
    private TableCellRenderer searchMatchRenderer;
    private int searchMatchRow;
    private int searchMatchColumn;
    private int frozenColumnCount;
    private boolean deferredPaintPending;
    private final CsvTablePaintSnapshot paintSnapshot = new CsvTablePaintSnapshot();

    public static class CommentColumn {
    }

    public CsvTable(TableModel model) {
        super(model);
        setCellSelectionEnabled(true);
        // Expansion re-renders and rewraps the shared multi-line renderer on hover, flickering at the viewport edge.
        setExpandableItemsEnabled(false);
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("TAB"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("shift TAB"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("RIGHT"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("LEFT"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("UP"));
        new EditableCellFocusAction(this, KeyStroke.getKeyStroke("DOWN"));
    }

    @Override
    public void setUI(TableUI ui) {
        super.setUI(new MultiSpanCellTableUI());
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        setBackground(scheme.getDefaultBackground());
        setForeground(scheme.getDefaultForeground());
        setSelectionBackground(UIUtil.getTableSelectionBackground(true));
        setSelectionForeground(UIUtil.getTableSelectionForeground(true));
        setGridColor(JBColor.border());
        setShowHorizontalLines(true);
        setShowVerticalLines(false);
        setIntercellSpacing(JBUI.size(0, 1));
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
        // Blitting would mix scrolled pixels with the complete previous frame used by unlocked paints.
        if (getParent() instanceof JViewport viewport) {
            viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        }
        if (getTableHeader().getParent() instanceof JViewport viewport) {
            viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
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
    public void paint(@NotNull Graphics g) {
        paintSnapshot.paint(this, g, !deferPaintWithoutReadAccess(), this::paintWithReadAccess);
    }

    private void paintWithReadAccess(Graphics g) {
        super.paint(g);
        // Paint above spanning comment rows and cell editors as well as ordinary cells.
        paintFrozenColumnSeparator(g, getHeight());
        if (getClientProperty(CsvTableRowDragListener.class) instanceof CsvTableRowDragListener drag) {
            drag.paintFeedback(g, this);
        }
    }

    @Override
    public void removeNotify() {
        paintSnapshot.clear();
        super.removeNotify();
    }

    boolean deferPaintWithoutReadAccess() {
        Application application = ApplicationManager.getApplication();
        if (application.isReadAccessAllowed()) return false;
        if (!deferredPaintPending) {
            deferredPaintPending = true;
            var editor = getModel().getPsiFileHolder();
            var project = editor.getProject();
            application.invokeLater(() -> {
                deferredPaintPending = false;
                if (SwingUtilities.getAncestorOfClass(JScrollPane.class, this) instanceof JScrollPane scrollPane &&
                        scrollPane.isShowing()) {
                    // repaint() would lose the callback's read access before rendering.
                    scrollPane.paintImmediately(scrollPane.getVisibleRect());
                }
            }, ModalityState.stateForComponent(this), ignored -> editor.isDisposed() || (project != null && project.isDisposed()));
        }
        return true;
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
        if (!isEditing()) return;
        Component editor = getEditorComponent();
        editor.setBounds(getCellRect(getEditingRow(), getEditingColumn(), false));
        // A resized multi-line editor must wrap its text again before it is painted.
        editor.validate();
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
        if (getColumnCount() > 0 && isCommentRow(row)) {
            return getCommentRect(row, includeSpacing);
        }
        Rectangle rect = super.getCellRect(row, column, includeSpacing);
        // The translated frozen-column paint pass needs the columns' unshifted positions.
        if (paintCounter == 0 && column >= 0 && column < getFrozenColumnCount()) {
            rect.x += getFrozenColumnOffset();
        }
        return rect;
    }

    /**
     * Comments span all columns like a merged cell, so every column of a comment row shares this rectangle.
     * Frozen columns pin comments to the viewport, where text wider than the viewport could never be scrolled into view.
     */
    private Rectangle getCommentRect(int row, boolean includeSpacing) {
        Rectangle rect = super.getCellRect(row, 0, includeSpacing);
        int x = 0;
        int width = getWidth();
        if (getFrozenColumnCount() > 0) {
            Rectangle visible = getVisibleRect();
            if (!visible.isEmpty()) {
                x = visible.x;
                width = visible.width;
            }
        }
        int margin = includeSpacing ? 0 : getColumnModel().getColumnMargin();
        rect.x = x + margin / 2;
        rect.width = width - margin;
        return rect;
    }

    @Override
    public int columnAtPoint(@NotNull Point point) {
        // BasicTableUI hit-tests painted positions, which exclude the frozen offset during the translated pass.
        if (paintCounter != 0) {
            return super.columnAtPoint(point);
        }
        if (isCommentRow(rowAtPoint(point))) return 0;
        Point location = new Point(point);
        int offset = getFrozenColumnOffset();
        if (location.x >= offset && location.x < offset + getFrozenColumnWidth()) {
            location.x -= offset;
        }
        return super.columnAtPoint(location);
    }

    @Override
    public boolean isCellSelected(int row, int column) {
        // Like copying and clearing, a comment belongs to every selection intersecting any of its columns.
        int selectionColumn = isCommentRow(row)
                ? Math.max(getColumnModel().getSelectionModel().getMinSelectionIndex(), 0) : column;
        return super.isCellSelected(row, selectionColumn);
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent event) {
        super.columnSelectionChanged(event);
        // Swing repaints only the changed columns, but every column affects a comment's selection.
        repaintSelectedCommentRows();
    }

    private void repaintSelectedCommentRows() {
        RowRange rows = getRowsIn(getVisibleRect());
        if (rows == null) return;
        ListSelectionModel selection = getSelectionModel();
        for (int row = rows.first(); row <= rows.last(); row++) {
            if ((selection.isSelectedIndex(row) || selection.getLeadSelectionIndex() == row) && isCommentRow(row)) {
                repaint(getCellRect(row, 0, true));
            }
        }
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
            paintTable(g, rendererPane, graphics -> paintRecordRows(graphics, c));
        }

        /**
         * BasicTableUI renders comments once per column and resizes and validates a comment editor to the first
         * column on every paint, so it only receives the bands of record rows between comments.
         */
        private void paintRecordRows(Graphics g, JComponent c) {
            if (getRowCount() == 0 || getColumnCount() == 0) {
                super.paint(g, c);
                return;
            }
            RowRange rows = getRowsIn(getClipBounds(g));
            if (rows == null) return;
            int bandStart = rows.first();
            for (int row = rows.first(); row <= rows.last(); row++) {
                if (!isCommentRow(row)) continue;
                paintRecordBand(g, c, bandStart, row - 1);
                bandStart = row + 1;
            }
            if (bandStart == rows.first()) {
                super.paint(g, c);
            } else {
                paintRecordBand(g, c, bandStart, rows.last());
            }
        }

        private void paintRecordBand(Graphics g, JComponent c, int firstRow, int lastRow) {
            if (firstRow > lastRow) return;
            int top = getCellRect(firstRow, 0, true).y;
            Rectangle last = getCellRect(lastRow, 0, true);
            Graphics band = g.create();
            try {
                band.clipRect(0, top, getWidth(), last.y + last.height - top);
                super.paint(band, c);
            } finally {
                band.dispose();
            }
        }
    }

    private record RowRange(int first, int last) {
    }

    private Rectangle getClipBounds(Graphics g) {
        Rectangle clip = g.getClipBounds();
        return clip == null ? new Rectangle(0, 0, getWidth(), getHeight()) : clip;
    }

    /**
     * Returns the rows intersecting the area, or null if it misses all rows.
     */
    private @Nullable RowRange getRowsIn(Rectangle area) {
        Rectangle visible = area.intersection(new Rectangle(0, 0, getWidth(), getHeight()));
        if (visible.isEmpty() || getRowCount() == 0 || getColumnCount() == 0) return null;
        int first = rowAtPoint(visible.getLocation());
        if (first < 0) return null;
        int last = rowAtPoint(new Point(visible.x, visible.y + visible.height - 1));
        return new RowRange(first, last < 0 ? getRowCount() - 1 : last);
    }

    private void paintTable(Graphics g, CellRendererPane rendererPane, Consumer<Graphics> paintRecordRows) {
        paintCounter++;
        try {
            paintRecordRows.accept(g);
            if (getFrozenColumnCount() > 0) {
                // Keep one selection and column model; only the leading columns' painted position changes.
                Graphics frozen = g.create();
                try {
                    frozen.translate(getFrozenColumnOffset(), 0);
                    frozen.clipRect(0, 0, getFrozenColumnWidth(), getHeight());
                    frozen.setColor(getBackground());
                    frozen.fillRect(0, 0, getFrozenColumnWidth(), getHeight());
                    paintRecordRows.accept(frozen);
                } finally {
                    frozen.dispose();
                }
            }
            paintCommentRows(g, rendererPane);
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

    private void paintCommentRows(Graphics g, CellRendererPane rendererPane) {
        RowRange rows = getRowsIn(getClipBounds(g));
        if (rows == null) return;
        for (int row = rows.first(); row <= rows.last(); row++) {
            if (isCommentRow(row)) paintCommentRow(g, rendererPane, row);
        }
    }

    private void paintCommentRow(Graphics g, CellRendererPane rendererPane, int row) {
        paintCommentGridLines(g, getCellRect(row, 0, true));
        // An edited comment is painted by its editor component.
        if (isEditing() && getEditingRow() == row) return;
        Rectangle cell = getCellRect(row, 0, false);
        Component component = prepareRenderer(getCellRenderer(row, 0), row, 0);
        rendererPane.paintComponent(g, component, this, cell.x, cell.y, cell.width, cell.height, true);
    }

    private void paintCommentGridLines(Graphics g, Rectangle span) {
        Color color = g.getColor();
        g.setColor(getGridColor());
        int right = span.x + span.width - 1;
        int bottom = span.y + span.height - 1;
        if (getShowHorizontalLines()) g.drawLine(span.x, bottom, right, bottom);
        if (getShowVerticalLines()) g.drawLine(right, span.y, right, bottom);
        g.setColor(color);
    }

}
