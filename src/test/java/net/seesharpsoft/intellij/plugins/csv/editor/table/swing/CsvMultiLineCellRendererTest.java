package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class CsvMultiLineCellRendererTest extends CsvTableEditorSwingTestBase {

    private static final String OVERFLOWING_CELL = "one\ntwo\nthree\nfour\nfive\nsix";

    private CsvTable layoutTable(String text) {
        fileEditor.dispose();
        myFixture.configureByText(CsvFileType.INSTANCE, text);
        fileEditor = createEditor();
        fileEditor.selectNotify();
        int[] widths = new int[fileEditor.getTable().getColumnCount()];
        Arrays.fill(widths, 100);
        fileEditor.getTableEditorState().setColumnWidths(widths);
        fileEditor.updateEditorLayout();
        JPanel root = new JPanel(new BorderLayout());
        root.add(fileEditor.getTableScrollPane());
        root.setSize(450, 240);
        layoutRecursively(root);
        UIUtil.dispatchAllInvocationEvents();
        return fileEditor.getTable();
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layoutRecursively(nested);
        }
    }

    // Headless components have no peer to validate, so lay out like a displayed renderer.
    private CsvMultiLineCellRenderer displayedRenderer() {
        return new CsvMultiLineCellRenderer(fileEditor.tableEditorKeyListener, fileEditor) {
            @Override
            public void validate() {
                super.validate();
                layoutRecursively(this);
            }
        };
    }

    private static int[] paintPixels(JComponent component, Rectangle clip) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setClip(clip);
            component.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image.getRGB(clip.x, clip.y, clip.width, clip.height, null, 0, clip.width);
    }

    public void testCellsRenderIdenticallyAfterRendererIsPreparedOutsidePainting() {
        CsvTable table = layoutTable("a,b,c\n\"" + OVERFLOWING_CELL + "\",x,y\n1,2,3\n4,5,6\n");
        table.setDefaultRenderer(Object.class, displayedRenderer());
        Rectangle row = table.getCellRect(1, 0, true).union(table.getCellRect(1, 2, true));
        int[] painted = paintPixels(table, row);
        UIUtil.dispatchAllInvocationEvents();

        // Hover expansion probes the hovered cell's renderer outside painting.
        table.prepareRenderer(table.getCellRenderer(1, 0), 1, 0);
        UIUtil.dispatchAllInvocationEvents();

        assertArrayEquals(painted, paintPixels(table, row));
    }

    public void testOverflowingCellAlwaysRendersItsFirstLines() {
        CsvTable table = layoutTable("a,b,c\n\"" + OVERFLOWING_CELL + "\",x,y\n1,2,3\n");
        CsvMultiLineCellRenderer renderer = displayedRenderer();
        table.setDefaultRenderer(Object.class, renderer);
        paintPixels(table, table.getCellRect(1, 0, true));

        renderer.getTableCellRendererComponent(table, OVERFLOWING_CELL, false, false, 1, 0);
        UIUtil.dispatchAllInvocationEvents();
        renderer.getTableCellRendererComponent(table, OVERFLOWING_CELL, false, false, 1, 0);

        assertEquals(new Point(), renderer.getViewport().getViewPosition());
    }

    public void testCommentRendererWrapsAtTheCommentSpan() {
        CsvTable table = layoutTable("# a comment wider than one column\na,b,c\n1,2,3\n");
        List<Boolean> scrollBarsShown = new ArrayList<>();
        table.setDefaultRenderer(CsvTable.CommentColumn.class,
                new CsvMultiLineCellRenderer.Comment(fileEditor.tableEditorKeyListener, fileEditor) {
                    @Override
                    public void validate() {
                        super.validate();
                        layoutRecursively(this);
                    }

                    @Override
                    public void paint(Graphics g) {
                        scrollBarsShown.add(getVerticalScrollBar().isVisible());
                        super.paint(g);
                    }
                });

        paintPixels(table, table.getCellRect(0, 0, true));

        assertEquals(List.of(false), scrollBarsShown);
    }

    public void testCellRendererUsage() {
        TableCellRenderer cellRenderer = fileEditor.getTable().getCellRenderer(0, 0);

        assertInstanceOf(cellRenderer, CsvMultiLineCellRenderer.class);
    }

    public void testPreferredSize() {
        JScrollPane jTextArea = (JScrollPane) fileEditor.getTable().getCellRenderer(0, 0);

        assertNotNull(jTextArea.getPreferredSize());
    }

    public void testCellEditorComponent() {
        CsvMultiLineCellRenderer cellRenderer = (CsvMultiLineCellRenderer) fileEditor.getTable().getCellRenderer(0, 0);

        assertEquals(cellRenderer, cellRenderer.getTableCellEditorComponent(fileEditor.getTable(), "Test", true, 0, 0));
    }

    public void testFocusedEditableCellUsesTableColors() {
        JTable table = fileEditor.getTable();
        Color foreground = Color.MAGENTA;
        Color background = Color.ORANGE;
        table.setForeground(foreground);
        table.setBackground(background);

        CsvMultiLineCellRenderer renderer = (CsvMultiLineCellRenderer) table.getCellRenderer(0, 0);
        renderer.getTableCellRendererComponent(table, "Test", false, true, 0, 0);
        JTextArea textArea = (JTextArea) renderer.getViewport().getView();

        assertEquals(foreground, textArea.getForeground());
        assertEquals(background, textArea.getBackground());
    }

    public void testCellEditing() {
        CsvMultiLineCellRenderer cellRenderer = (CsvMultiLineCellRenderer) fileEditor.getTable().getCellRenderer(0, 0);

        assertTrue(cellRenderer.isCellEditable(null));
        assertTrue(cellRenderer.shouldSelectCell(null));

        final int[] states = {0, 0};
        CellEditorListener listener = new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent e) {
                ++states[0];
            }

            @Override
            public void editingCanceled(ChangeEvent e) {
                ++states[1];
            }
        };
        cellRenderer.addCellEditorListener(listener);

        assertTrue(cellRenderer.stopCellEditing());
        assertEquals(1, states[0]);
        cellRenderer.cancelCellEditing();
        assertEquals(1, states[1]);

        cellRenderer.removeCellEditorListener(listener);
        assertTrue(cellRenderer.stopCellEditing());
        assertEquals(1, states[0]);
        cellRenderer.cancelCellEditing();
        assertEquals(1, states[1]);
    }
}
