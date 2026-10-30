package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.CsvFileType;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvTableCommentRowTest extends CsvTableEditorSwingTestBase {
    private static final int COMMENT_ROW = 7;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        fileEditor.dispose();
        StringBuilder text = new StringBuilder("id,name,city,age,role,team\n");
        for (int row = 1; row < 40; row++) {
            text.append(row % 5 == COMMENT_ROW % 5 ? "# note " + row : row + ",Alice,Berlin,30,Developer,Platform").append('\n');
        }
        myFixture.configureByText(CsvFileType.INSTANCE, text.toString());
        fileEditor = createEditor();
        fileEditor.selectNotify();
        int[] widths = new int[6];
        Arrays.fill(widths, 100);
        fileEditor.getTableEditorState().setColumnWidths(widths);
        fileEditor.updateEditorLayout();
        JPanel root = new JPanel(new BorderLayout());
        root.add(fileEditor.getTableScrollPane());
        root.setSize(450, 240);
        layoutRecursively(root);
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layoutRecursively(nested);
        }
    }

    private JViewport viewport() {
        return fileEditor.getTableScrollPane().getViewport();
    }

    private void scrollToRow(int x, int row) {
        Point position = new Point(x, fileEditor.getTable().getCellRect(row, 0, true).y);
        viewport().setViewPosition(position);
        assertEquals(position, viewport().getViewPosition());
    }

    private BufferedImage paint(JComponent component) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setClip(viewport().getViewRect());
            component.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private Rectangle visibleCommentSpan() {
        return fileEditor.getTable().getCellRect(COMMENT_ROW, 0, true).intersection(viewport().getViewRect());
    }

    public void testCommentEditorIsPaintedAcrossTheRow() {
        CsvTable table = fileEditor.getTable();
        List<Integer> paintedWidths = new ArrayList<>();
        table.setDefaultEditor(CsvTable.CommentColumn.class,
                new CsvMultiLineCellRenderer.Comment(fileEditor.tableEditorKeyListener, fileEditor) {
                    @Override
                    public void validate() {
                        super.validate();
                        // Headless components have no peer to validate, so lay out like a displayed editor.
                        layoutRecursively(this);
                    }

                    @Override
                    public void paint(Graphics g) {
                        paintedWidths.add(getViewport().getWidth());
                        super.paint(g);
                    }
                });
        scrollToRow(0, COMMENT_ROW);
        assertTrue(table.editCellAt(COMMENT_ROW, 0));

        for (int frozenColumns : new int[]{0, 2}) {
            fileEditor.setFrozenColumnCount(frozenColumns);
            paintedWidths.clear();
            paint(table);

            assertEquals(table.getCellRect(COMMENT_ROW, 0, false), table.getEditorComponent().getBounds());
            assertEquals("Frozen columns: " + frozenColumns, 1, paintedWidths.size());
            assertTrue("The editable comment must span more than the first column: " + paintedWidths,
                    paintedWidths.getFirst() > table.getColumnModel().getColumn(0).getWidth());
        }
        table.getCellEditor().cancelCellEditing();
    }

    public void testPaintingDoesNotMoveOrResizeTheCommentEditor() {
        CsvTable table = fileEditor.getTable();
        List<Rectangle> boundsChanges = new ArrayList<>();
        table.setDefaultEditor(CsvTable.CommentColumn.class,
                new CsvMultiLineCellRenderer.Comment(fileEditor.tableEditorKeyListener, fileEditor) {
                    @Override
                    public void setBounds(int x, int y, int width, int height) {
                        Rectangle bounds = new Rectangle(x, y, width, height);
                        if (!bounds.equals(getBounds())) boundsChanges.add(bounds);
                        super.setBounds(x, y, width, height);
                    }
                });
        scrollToRow(0, COMMENT_ROW);
        assertTrue(table.editCellAt(COMMENT_ROW, 0));

        for (int frozenColumns : new int[]{0, 2}) {
            fileEditor.setFrozenColumnCount(frozenColumns);
            boundsChanges.clear();
            paint(table);
            paint(table);
            assertEquals("Frozen columns: " + frozenColumns, List.of(), boundsChanges);
        }
        table.getCellEditor().cancelCellEditing();
    }

    public void testCommentIsSelectedWhenTheSelectionIntersectsAnyOfItsColumns() {
        CsvTable table = fileEditor.getTable();
        scrollToRow(0, COMMENT_ROW - 1);
        table.changeSelection(COMMENT_ROW - 1, 2, false, false);
        table.changeSelection(COMMENT_ROW + 1, 3, false, true);

        assertTrue(table.isCellSelected(COMMENT_ROW, 0));
        BufferedImage image = paint(table);
        Rectangle comment = visibleCommentSpan();
        assertEquals(table.getSelectionBackground().getRGB(),
                image.getRGB(comment.x + comment.width - 4, comment.y + comment.height / 2));
    }

    public void testDeselectingTheLastColumnRepaintsTheWholeComment() {
        CsvTable table = fileEditor.getTable();
        scrollToRow(0, COMMENT_ROW - 2);
        table.changeSelection(COMMENT_ROW - 2, 1, false, false);
        table.changeSelection(COMMENT_ROW + 2, 1, false, true);
        assertTrue(table.isCellSelected(COMMENT_ROW, 0));

        try (DirtyRegions dirty = new DirtyRegions(table)) {
            table.changeSelection(COMMENT_ROW - 1, 1, true, false);

            assertFalse(table.isCellSelected(COMMENT_ROW, 0));
            assertTrue(dirty.covers(visibleCommentSpan()));
        }
    }

    public void testExtendingTheSelectionToACommentRepaintsEveryNewlySelectedRow() {
        CsvTable table = fileEditor.getTable();
        scrollToRow(0, COMMENT_ROW - 4);
        table.changeSelection(COMMENT_ROW - 4, 1, false, false);

        try (DirtyRegions dirty = new DirtyRegions(table)) {
            table.changeSelection(COMMENT_ROW, 1, false, true);

            for (int row = COMMENT_ROW - 3; row < COMMENT_ROW; row++) {
                assertTrue("Row " + row, dirty.covers(table.getCellRect(row, 1, true)));
            }
            assertTrue(dirty.covers(visibleCommentSpan()));
        }
    }

    public void testExtendingTheSelectionOntoACommentKeepsTheViewport() {
        CsvTable table = fileEditor.getTable();
        scrollToRow(0, COMMENT_ROW - 2);
        Point position = viewport().getViewPosition();
        table.changeSelection(COMMENT_ROW - 1, 2, false, false);

        table.changeSelection(COMMENT_ROW, 2, false, true);

        assertEquals(position, viewport().getViewPosition());
    }

    public void testEachVisibleCommentIsRenderedOncePerPaint() {
        CsvTable table = fileEditor.getTable();
        TableCellRenderer renderer = table.getDefaultRenderer(CsvTable.CommentColumn.class);
        List<Point> rendered = new ArrayList<>();
        table.setDefaultRenderer(CsvTable.CommentColumn.class, (target, value, selected, focused, row, column) -> {
            rendered.add(new Point(column, row));
            return renderer.getTableCellRendererComponent(target, value, selected, focused, row, column);
        });
        scrollToRow(0, COMMENT_ROW - 1);
        Rectangle view = viewport().getViewRect();
        List<Point> visibleComments = new ArrayList<>();
        for (int row = table.rowAtPoint(view.getLocation()); row <= table.rowAtPoint(new Point(0, view.y + view.height - 1)); row++) {
            if (table.isCommentRow(row)) visibleComments.add(new Point(0, row));
        }
        assertFalse(visibleComments.isEmpty());

        for (int frozenColumns : new int[]{0, 2}) {
            fileEditor.setFrozenColumnCount(frozenColumns);
            rendered.clear();
            paint(table);
            assertEquals("Frozen columns: " + frozenColumns, visibleComments, rendered);
        }
    }

    public void testGridLinesStayAlignedBelowACommentAtTheTopOfAHorizontallyScrolledView() {
        CsvTable table = fileEditor.getTable();
        scrollToRow(150, COMMENT_ROW);

        BufferedImage image = paint(table);

        Rectangle view = viewport().getViewRect();
        int x = view.x + view.width / 2;
        for (int row = COMMENT_ROW; row < COMMENT_ROW + 4; row++) {
            Rectangle cell = table.getCellRect(row, 0, true);
            assertEquals("Grid line below row " + row, table.getGridColor().getRGB(), image.getRGB(x, cell.y + cell.height - 1));
        }
    }

    private static final class DirtyRegions extends RepaintManager implements AutoCloseable {
        private final RepaintManager original;
        private final JComponent component;
        private final Area area = new Area();

        private DirtyRegions(JComponent component) {
            this.component = component;
            original = RepaintManager.currentManager(component);
            RepaintManager.setCurrentManager(this);
        }

        @Override
        public void addDirtyRegion(JComponent target, int x, int y, int width, int height) {
            if (target == component) area.add(new Area(new Rectangle(x, y, width, height)));
        }

        private boolean covers(Rectangle rectangle) {
            return area.contains(rectangle);
        }

        @Override
        public void close() {
            RepaintManager.setCurrentManager(original);
        }
    }
}
