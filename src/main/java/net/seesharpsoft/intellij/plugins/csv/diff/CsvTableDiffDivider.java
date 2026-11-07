package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.diff.util.DiffDrawUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;
import net.seesharpsoft.intellij.plugins.csv.table.ui.CsvMultiLineCellRenderer;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Connects the changed row ranges of both diff tables like the text diff divider. Like the text diff gutters, it shows
 * the row numbers of both sides between a {@code >>} and a {@code <<} arrow that apply each change to the writable
 * side, and continues the wavy lines of collapsed unchanged rows. Dragging the empty middle between the row numbers
 * changes the width of both tables.
 */
final class CsvTableDiffDivider extends JComponent {
    interface Host {
        CsvDiffComparison comparison();

        CsvDiffFolding folding();

        boolean isWritable(int side);

        void apply(CsvDiffComparison.Change change, int targetSide);
    }

    record RowNumber(String text, Rectangle bounds, Color color) {
    }

    private static final int GAP = 4;
    private static final int SPLITTER_WIDTH = 20;
    private static final int MIN_TABLE_WIDTH = 50;

    private final JTable[] tables;
    private final Host host;
    private double proportion = 0.5;
    private int dragOffset = -1;

    CsvTableDiffDivider(JTable left, JTable right, Host host) {
        this.tables = new JTable[]{left, right};
        this.host = host;
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) return;
                ArrowHit hit = arrowAt(event.getPoint());
                if (hit != null) host.apply(hit.change(), hit.targetSide());
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && isSplitter(event.getPoint())) dragOffset = event.getX();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (dragOffset >= 0) moveTo(getX() + event.getX() - dragOffset);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                dragOffset = -1;
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                setCursor(cursorAt(event.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (dragOffset < 0) setCursor(null);
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    private Cursor cursorAt(Point point) {
        if (arrowAt(point) != null) return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        return isSplitter(point) ? Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR) : null;
    }

    private boolean isSplitter(Point point) {
        Columns columns = columns();
        return point.x >= columns.curveStart() && point.x < columns.curveEnd();
    }

    /**
     * Moves this divider to the given x coordinate of its parent, keeping both tables at least partially visible.
     */
    private void moveTo(int x) {
        Container parent = getParent();
        if (parent == null) return;
        Insets insets = parent.getInsets();
        int available = parent.getWidth() - insets.left - insets.right - getWidth();
        if (available <= 0) return;
        int minimum = Math.min(JBUI.scale(MIN_TABLE_WIDTH), available / 2);
        proportion = (double) Math.clamp(x - insets.left, minimum, available - minimum) / available;
        parent.revalidate();
        parent.repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(columns().width(), 0);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        ArrowHit hit = arrowAt(event.getPoint());
        return hit == null ? null
                : CsvPluginManager.getLocalizedText(hit.targetSide() == 0 ? "table.diff.apply.left" : "table.diff.apply.right");
    }

    /**
     * Returns the bounds of the arrow applying the change at the given table row to the target side,
     * or {@code null} if the row is unchanged or the target side cannot receive changes.
     */
    Rectangle applyArrowBounds(int row, int targetSide) {
        CsvDiffComparison comparison = host.comparison();
        CsvDiffFolding folding = host.folding();
        CsvDiffComparison.Change change = comparison == null || folding.isFold(row) ? null
                : comparison.changeAt(folding.firstRow(row));
        return change == null ? null : arrowBounds(change, targetSide, columns());
    }

    /**
     * Returns the record number shown for the given side's table row and the area it is drawn in,
     * or {@code null} if the row is not displayed. Fold rows skip the numbers of their hidden records.
     */
    RowNumber rowNumber(int side, int row) {
        return rowNumber(side, row, columns());
    }

    private RowNumber rowNumber(int side, int row, Columns columns) {
        CsvDiffComparison comparison = host.comparison();
        CsvDiffFolding folding = host.folding();
        if (comparison == null || row < 0 || row >= folding.displayRowCount() || row >= tables[side].getRowCount()) {
            return null;
        }
        Rectangle rowBounds = rowBounds(side, row);
        boolean fold = folding.isFold(row);
        int alignedRow = folding.firstRow(row);
        return new RowNumber(fold ? "" : comparison.rowNumber(side, alignedRow),
                new Rectangle(columns.numbersX(side), rowBounds.y, columns.numbersWidth(side), rowBounds.height),
                numberColor(!fold && comparison.changeAt(alignedRow) != null));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        CsvDiffComparison comparison = host.comparison();
        if (comparison == null) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.clip(tableArea());
            Columns columns = columns();
            List<CsvDiffComparison.Change> changes = visibleChanges(comparison);
            for (CsvDiffComparison.Change change : changes) paintBand(g, change, columns);
            paintFolds(g, columns);
            paintRowNumbers(g, columns);
            for (CsvDiffComparison.Change change : changes) paintArrows(g, change, columns);
        } finally {
            g.dispose();
        }
    }

    /**
     * Highlights the change behind each side's row numbers and joins both sides with the text diff's curved band.
     */
    private void paintBand(Graphics2D g, CsvDiffComparison.Change change, Columns columns) {
        Rectangle left = rows(0, change);
        Rectangle right = rows(1, change);
        Color color = change.type().getColor(null);
        g.setColor(color);
        g.fillRect(0, left.y, columns.curveStart(), left.height);
        g.fillRect(columns.curveEnd(), right.y, getWidth() - columns.curveEnd(), right.height);
        // The curve fills through its end coordinates, which would overhang the exclusive bottom of the rows.
        DiffDrawUtil.drawCurveTrapezium(g, columns.curveStart(), columns.curveEnd(),
                left.y, (int) left.getMaxY() - 1, right.y, (int) right.getMaxY() - 1, color, null);
    }

    /**
     * Continues the waves of each side's fold rows through the divider; each side's wave covers half of the splitter.
     */
    private void paintFolds(Graphics2D g, Columns columns) {
        CsvDiffFolding folding = host.folding();
        int middle = (columns.curveStart() + columns.curveEnd()) / 2;
        int originX = CsvDiffFoldWave.originX(this);
        for (int side = 0; side < 2; side++) {
            int lastRow = visibleRow(side, Rectangle::getMaxY);
            for (int row = Math.max(0, visibleRow(side, Rectangle::getMinY)); row <= lastRow; row++) {
                if (!folding.isFold(row)) continue;
                Rectangle bounds = rowBounds(side, row);
                CsvDiffFoldWave.paint(g, side == 0 ? 0 : middle, side == 0 ? middle : getWidth(),
                        bounds.y, bounds.height, originX);
            }
        }
    }

    private void paintRowNumbers(Graphics2D g, Columns columns) {
        UISettings.setupAntialiasing(g);
        g.setFont(numberFont());
        FontMetrics metrics = g.getFontMetrics();
        for (int side = 0; side < 2; side++) {
            int lastRow = visibleRow(side, Rectangle::getMaxY);
            for (int row = visibleRow(side, Rectangle::getMinY); row <= lastRow; row++) {
                RowNumber number = rowNumber(side, row, columns);
                if (number == null || number.text().isEmpty()) continue;
                Rectangle bounds = number.bounds();
                int x = side == 0 ? (int) bounds.getMaxX() - metrics.stringWidth(number.text()) : bounds.x;
                g.setColor(number.color());
                g.drawString(number.text(), x, firstLineTop(bounds) + metrics.getAscent());
            }
        }
    }

    private void paintArrows(Graphics2D g, CsvDiffComparison.Change change, Columns columns) {
        for (int targetSide = 0; targetSide < 2; targetSide++) {
            Rectangle bounds = arrowBounds(change, targetSide, columns);
            if (bounds != null) arrow(targetSide).paintIcon(this, g, bounds.x, bounds.y);
        }
    }

    private static Font numberFont() {
        return EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);
    }

    /**
     * Aligns row numbers and arrows with the first text line of the cells, which use the same editor font.
     */
    private static int firstLineTop(Rectangle row) {
        return row.y + JBUI.scale(CsvMultiLineCellRenderer.CELL_VERTICAL_PADDING);
    }

    /**
     * Uses the editor text color on change bands: the dimmed line number color is unreadable on diff backgrounds,
     * which the color schemes design for regular text.
     */
    private static Color numberColor(boolean changed) {
        var scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Color color = changed ? scheme.getDefaultForeground() : scheme.getColor(EditorColors.LINE_NUMBERS_COLOR);
        return Objects.requireNonNullElse(color, JBColor.GRAY);
    }

    /**
     * Places the arrow beside the row numbers of the side providing the change: {@code >>} on the left, {@code <<} on the right.
     */
    private Rectangle arrowBounds(CsvDiffComparison.Change change, int targetSide, Columns columns) {
        if (!host.isWritable(targetSide)) return null;
        Icon icon = arrow(targetSide);
        int sourceSide = 1 - targetSide;
        Rectangle firstRow = rowBounds(sourceSide, host.folding().displayRow(change.startRow()));
        int lineHeight = getFontMetrics(numberFont()).getHeight();
        int y = firstLineTop(firstRow) + (lineHeight - icon.getIconHeight()) / 2;
        return new Rectangle(columns.arrowX(sourceSide), y, icon.getIconWidth(), icon.getIconHeight());
    }

    private static Icon arrow(int targetSide) {
        return targetSide == 0 ? AllIcons.Diff.Arrow : AllIcons.Diff.ArrowRight;
    }

    private ArrowHit arrowAt(Point point) {
        CsvDiffComparison comparison = host.comparison();
        if (comparison == null || !tableArea().contains(point)) return null;
        Columns columns = columns();
        for (CsvDiffComparison.Change change : visibleChanges(comparison)) {
            for (int targetSide = 0; targetSide < 2; targetSide++) {
                Rectangle bounds = arrowBounds(change, targetSide, columns);
                if (bounds != null && bounds.contains(point)) return new ArrowHit(change, targetSide);
            }
        }
        return null;
    }

    private record ArrowHit(CsvDiffComparison.Change change, int targetSide) {
    }

    /**
     * Horizontal layout of the divider: {@code [>>] [left row numbers] [splitter with curve] [right row numbers] [<<]}.
     */
    private record Columns(int leftArrowX, int leftNumbersX, int leftNumbersWidth,
                           int rightNumbersX, int rightNumbersWidth, int rightArrowX, int width) {
        int arrowX(int side) {
            return side == 0 ? leftArrowX : rightArrowX;
        }

        int numbersX(int side) {
            return side == 0 ? leftNumbersX : rightNumbersX;
        }

        int numbersWidth(int side) {
            return side == 0 ? leftNumbersWidth : rightNumbersWidth;
        }

        int curveStart() {
            return leftNumbersX + leftNumbersWidth;
        }

        int curveEnd() {
            return rightNumbersX;
        }
    }

    private Columns columns() {
        int gap = JBUI.scale(GAP);
        FontMetrics metrics = getFontMetrics(numberFont());
        int leftNumbersWidth = numbersWidth(metrics, 0);
        int rightNumbersWidth = numbersWidth(metrics, 1);
        int leftArrowX = gap;
        int leftNumbersX = leftArrowX + arrow(1).getIconWidth() + gap;
        int rightNumbersX = leftNumbersX + leftNumbersWidth + JBUI.scale(SPLITTER_WIDTH);
        int rightArrowX = rightNumbersX + rightNumbersWidth + gap;
        return new Columns(leftArrowX, leftNumbersX, leftNumbersWidth, rightNumbersX, rightNumbersWidth,
                rightArrowX, rightArrowX + arrow(0).getIconWidth() + gap);
    }

    private int numbersWidth(FontMetrics metrics, int side) {
        CsvDiffComparison comparison = host.comparison();
        int records = comparison == null ? 0 : comparison.sides().get(side).rows().size();
        return metrics.stringWidth("0".repeat(String.valueOf(Math.max(1, records)).length()));
    }

    private Rectangle rows(int side, CsvDiffComparison.Change change) {
        CsvDiffFolding folding = host.folding();
        return rowBounds(side, folding.displayRow(change.startRow()))
                .union(rowBounds(side, folding.displayRow(change.endRow() - 1)));
    }

    private Rectangle rowBounds(int side, int row) {
        JTable table = tables[side];
        return SwingUtilities.convertRectangle(table, table.getCellRect(row, 0, true), this);
    }

    /**
     * Restricts painting and clicks to the scrolled table rows so the divider never covers the titles or column headers.
     */
    private Rectangle tableArea() {
        Rectangle left = visibleTableArea(0);
        Rectangle right = visibleTableArea(1);
        int top = Math.min(left.y, right.y);
        int bottom = (int) Math.max(left.getMaxY(), right.getMaxY());
        return new Rectangle(0, top, getWidth(), Math.max(0, bottom - top));
    }

    private Rectangle visibleTableArea(int side) {
        JTable table = tables[side];
        return SwingUtilities.convertRectangle(table, table.getVisibleRect(), this);
    }

    private List<CsvDiffComparison.Change> visibleChanges(CsvDiffComparison comparison) {
        List<CsvDiffComparison.Change> changes = comparison.differences();
        CsvDiffFolding folding = host.folding();
        int firstRow = folding.firstRow(Math.min(visibleRow(0, Rectangle::getMinY), visibleRow(1, Rectangle::getMinY)));
        int lastRow = folding.lastRow(Math.max(visibleRow(0, Rectangle::getMaxY), visibleRow(1, Rectangle::getMaxY)));
        int first = comparison.firstChangeEndingAfter(firstRow);
        int last = first;
        while (last < changes.size() && changes.get(last).startRow() <= lastRow) last++;
        return changes.subList(first, last);
    }

    private int visibleRow(int side, ToDoubleFunction<Rectangle> edge) {
        JTable table = tables[side];
        Rectangle visible = table.getVisibleRect();
        int y = (int) Math.min(edge.applyAsDouble(visible), table.getHeight() - 1);
        int row = table.rowAtPoint(new Point(visible.x, y));
        return row < 0 ? table.getRowCount() - 1 : row;
    }

    /**
     * Lays out the left table, this divider at its preferred width, and the right table, splitting the remaining width
     * in the proportion the divider was dragged to.
     */
    static final class Layout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component component) {
        }

        @Override
        public void removeLayoutComponent(Component component) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return layoutSize(parent, Component::getPreferredSize);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return layoutSize(parent, Component::getMinimumSize);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            CsvTableDiffDivider divider = (CsvTableDiffDivider) parent.getComponent(1);
            int dividerWidth = divider.getPreferredSize().width;
            int height = parent.getHeight() - insets.top - insets.bottom;
            int available = Math.max(0, parent.getWidth() - insets.left - insets.right - dividerWidth);
            int leftWidth = (int) Math.round(available * divider.proportion);
            parent.getComponent(0).setBounds(insets.left, insets.top, leftWidth, height);
            divider.setBounds(insets.left + leftWidth, insets.top, dividerWidth, height);
            parent.getComponent(2).setBounds(insets.left + leftWidth + dividerWidth, insets.top, available - leftWidth, height);
        }

        private static Dimension layoutSize(Container parent, Function<Component, Dimension> size) {
            Insets insets = parent.getInsets();
            Dimension left = size.apply(parent.getComponent(0));
            Dimension divider = parent.getComponent(1).getPreferredSize();
            Dimension right = size.apply(parent.getComponent(2));
            return new Dimension(2 * Math.max(left.width, right.width) + divider.width + insets.left + insets.right,
                    Math.max(left.height, right.height) + insets.top + insets.bottom);
        }
    }
}
