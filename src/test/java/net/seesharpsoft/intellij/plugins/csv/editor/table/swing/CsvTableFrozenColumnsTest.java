package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.util.ui.JBUI;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.actions.CsvTableEditorActions;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorState;
import org.jdom.Element;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class CsvTableFrozenColumnsTest extends CsvTableEditorSwingTestBase {
    private static final Color[] COLORS = {Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.YELLOW};

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        fileEditor.dispose();
        myFixture.configureByText(CsvFileType.INSTANCE,
                "#comment\nid,name,city,age,role,team\n" + "1,Alice,Berlin,30,Developer,Platform\n".repeat(40));
        fileEditor = createEditor();
        fileEditor.selectNotify();
        int[] widths = new int[6];
        Arrays.fill(widths, 100);
        fileEditor.getTableEditorState().setColumnWidths(widths);
        fileEditor.updateEditorLayout();
        JPanel root = new JPanel(new BorderLayout());
        root.add(fileEditor.getTableScrollPane());
        root.setSize(450, 180);
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

    private void scroll(int x, int y) {
        viewport().setViewPosition(new Point(x, y));
        assertEquals(new Point(x, y), viewport().getViewPosition());
    }

    private AnActionEvent event(AnAction action) {
        return TestActionEvent.createTestEvent(action,
                SimpleDataContext.getSimpleContext(PlatformDataKeys.FILE_EDITOR, fileEditor));
    }

    public void testFreezeAndUnfreezeActionsUseRightmostSelectedColumn() {
        AnAction freeze = ActionManager.getInstance().getAction(CsvTableEditorActions.FreezeColumns.class.getName());
        AnAction unfreeze = ActionManager.getInstance().getAction(CsvTableEditorActions.UnfreezeColumns.class.getName());
        assertNotNull(freeze);
        assertNotNull(unfreeze);
        AnActionEvent freezeEvent = event(freeze);
        AnActionEvent unfreezeEvent = event(unfreeze);
        fileEditor.getTable().clearSelection();
        freeze.update(freezeEvent);
        unfreeze.update(unfreezeEvent);
        assertFalse(freezeEvent.getPresentation().isEnabled());
        assertFalse(unfreezeEvent.getPresentation().isEnabled());
        fileEditor.getTable().setColumnSelectionInterval(0, 1);
        freeze.update(freezeEvent);
        assertTrue(freezeEvent.getPresentation().isEnabled());
        freeze.actionPerformed(freezeEvent);
        assertEquals(2, fileEditor.getFrozenColumnCount());
        assertEquals(2, fileEditor.getTableEditorState().getFrozenColumnCount());
        unfreeze.update(unfreezeEvent);
        assertTrue(unfreezeEvent.getPresentation().isEnabled());
        unfreeze.actionPerformed(unfreezeEvent);
        assertEquals(0, fileEditor.getFrozenColumnCount());
        assertEquals(0, fileEditor.getTableEditorState().getFrozenColumnCount());
    }

    public void testFrozenCellsAndHeadersStayFixedAndHitTestCorrectly() {
        CsvTable table = fileEditor.getTable();
        fileEditor.setFrozenColumnCount(2);
        scroll(230, 60);
        for (int column = 0; column < 2; column++) {
            Rectangle cell = table.getCellRect(3, column, true);
            assertEquals(230 + column * 100, cell.x);
            assertEquals(column, table.columnAtPoint(new Point(cell.x + 10, cell.y + 10)));
            Rectangle header = table.getTableHeader().getHeaderRect(column);
            assertEquals(cell.x, header.x);
            assertEquals(column, table.getTableHeader().columnAtPoint(new Point(header.x + 10, 5)));
        }
        assertEquals(400, table.getCellRect(3, 4, true).x);
        assertEquals(4, table.columnAtPoint(new Point(440, table.getRowHeight() * 3 + 5)));
        assertEquals(JViewport.SIMPLE_SCROLL_MODE, viewport().getScrollMode());
        fileEditor.setFrozenColumnCount(0);
        assertEquals(0, table.getCellRect(3, 0, true).x);
        assertEquals(2, table.columnAtPoint(new Point(240, table.getRowHeight() * 3 + 5)));
    }

    public void testFrozenCellsAndHeadersActuallyPaintAboveScrolledColumns() {
        CsvTable table = fileEditor.getTable();
        useColumnColors();
        fileEditor.setFrozenColumnCount(2);
        scroll(230, 60);
        BufferedImage cells = paint(table, viewport().getViewRect());
        int y = table.getCellRect(3, 0, true).y + table.getRowHeight() / 2;
        assertEquals(Color.RED.getRGB(), cells.getRGB(240, y));
        assertEquals(Color.GREEN.getRGB(), cells.getRGB(340, y));
        assertEquals(Color.MAGENTA.getRGB(), cells.getRGB(440, y));
        JTableHeader header = table.getTableHeader();
        BufferedImage headers = paint(header, new Rectangle(230, 0, viewport().getWidth(), header.getHeight()));
        assertEquals(Color.RED.getRGB(), headers.getRGB(240, header.getHeight() / 2));
        assertEquals(Color.GREEN.getRGB(), headers.getRGB(340, header.getHeight() / 2));
        assertEquals(Color.MAGENTA.getRGB(), headers.getRGB(440, header.getHeight() / 2));
    }

    private void useColumnColors() {
        CsvTable table = fileEditor.getTable();
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, "", false, false, row, column);
                setBackground(COLORS[column]);
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        table.getTableHeader().setDefaultRenderer(renderer);
    }

    public void testFrozenColumnDividerAlignsAcrossHeaderAndRowsAndDisappearsWhenUnfrozen() {
        useColumnColors();
        for (int count : new int[]{1, 2}) {
            fileEditor.setFrozenColumnCount(count);
            for (int x : new int[]{0, 230}) {
                scroll(x, 0);
                assertFrozenColumnDivider();
            }
        }
        fileEditor.setFrozenColumnCount(0);
        CsvTable table = fileEditor.getTable();
        BufferedImage cells = paint(table, viewport().getViewRect());
        JTableHeader header = table.getTableHeader();
        BufferedImage headers = paint(header, new Rectangle(230, 0, viewport().getWidth(), header.getHeight()));
        int y = table.getCellRect(1, 0, true).y + table.getRowHeight() / 2;
        assertEquals(Color.MAGENTA.getRGB(), cells.getRGB(429, y));
        assertEquals(Color.MAGENTA.getRGB(), headers.getRGB(429, header.getHeight() / 2));
    }

    public void testFrozenColumnDividerTracksColumnResizing() {
        useColumnColors();
        fileEditor.setFrozenColumnCount(2);
        scroll(150, 0);
        fileEditor.getTable().getColumnModel().getColumn(1).setPreferredWidth(140);
        fileEditor.getTable().getColumnModel().getColumn(1).setWidth(140);
        assertFrozenColumnDivider();
    }

    public void testFrozenColumnDividerPaintsAboveCellEditor() {
        useColumnColors();
        fileEditor.setFrozenColumnCount(2);
        assertTrue(fileEditor.getTable().editCellAt(1, 1));
        scroll(230, 0);
        assertFrozenColumnDivider();
        fileEditor.getTable().getCellEditor().cancelCellEditing();
    }

    private void assertFrozenColumnDivider() {
        CsvTable table = fileEditor.getTable();
        int boundary = viewport().getViewPosition().x + table.getFrozenColumnWidth();
        int width = JBUI.scale(2);
        BufferedImage cells = paint(table, viewport().getViewRect());
        int y = table.getCellRect(1, 0, true).y + table.getRowHeight() / 2;
        JTableHeader header = table.getTableHeader();
        BufferedImage headers = paint(header, new Rectangle(viewport().getViewPosition().x, 0, viewport().getWidth(), header.getHeight()));
        int color = headers.getRGB(boundary - 1, header.getHeight() / 2);
        assertFalse("The divider must be distinct from the ordinary grid", color == table.getGridColor().getRGB());
        for (int x = boundary - width; x < boundary; x++) {
            assertEquals(color, cells.getRGB(x, y));
            assertEquals(color, cells.getRGB(x, table.getRowHeight() / 2));
            assertEquals(color, headers.getRGB(x, header.getHeight() / 2));
        }
        int outside = boundary + 1;
        int column = table.columnAtPoint(new Point(outside, y));
        assertEquals(COLORS[column].getRGB(), cells.getRGB(outside, y));
        assertEquals(COLORS[column].getRGB(), headers.getRGB(outside, header.getHeight() / 2));
        if (!table.isEditing()) {
            int frozenColor = COLORS[table.getFrozenColumnCount() - 1].getRGB();
            assertEquals(frozenColor, cells.getRGB(boundary - width - 1, y));
            assertEquals(frozenColor, headers.getRGB(boundary - width - 1, header.getHeight() / 2));
        }
    }

    private BufferedImage paint(JComponent component, Rectangle clip) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setClip(clip);
            component.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    public void testSelectingFrozenCellPreservesHorizontalScrollAndScrollsVertically() {
        fileEditor.setFrozenColumnCount(2);
        scroll(230, 60);
        fileEditor.getTable().changeSelection(25, 0, false, false);
        assertEquals(230, viewport().getViewPosition().x);
        assertTrue(viewport().getViewRect().contains(fileEditor.getTable().getCellRect(25, 0, true)));
    }

    public void testNavigatingScrollableCellsKeepsThemOutsideFrozenArea() {
        fileEditor.setFrozenColumnCount(2);
        scroll(230, 60);
        fileEditor.getTable().changeSelection(3, 2, false, false);
        assertEquals(0, viewport().getViewPosition().x);
        fileEditor.getTable().changeSelection(3, 5, false, false);
        Rectangle visible = viewport().getViewRect();
        Rectangle cell = fileEditor.getTable().getCellRect(3, 5, true);
        assertTrue(visible.contains(cell));
        assertTrue(cell.x >= visible.x + fileEditor.getTable().getFrozenColumnWidth());
    }

    public void testNarrowViewportRevealsStartOfScrollableCell() {
        Container root = fileEditor.getTableScrollPane().getParent();
        root.setSize(320, 180);
        layoutRecursively(root);
        fileEditor.setFrozenColumnCount(2);
        scroll(230, 60);
        fileEditor.getTable().changeSelection(3, 2, false, false);
        assertEquals(0, viewport().getViewPosition().x);
        assertTrue(viewport().getViewRect().contains(fileEditor.getTable().getCellRect(3, 2, true).getLocation()));
    }

    public void testFrozenCellEditorTracksScrollingAndWritesToOriginalColumn() {
        CsvTable table = fileEditor.getTable();
        fileEditor.setFrozenColumnCount(2);
        assertTrue(table.editCellAt(3, 1));
        scroll(230, 60);
        assertEquals(table.getCellRect(3, 1, false), table.getEditorComponent().getBounds());
        paint(table, viewport().getViewRect());
        assertEquals(table.getCellRect(3, 1, false), table.getEditorComponent().getBounds());
        CsvMultiLineCellRenderer editor = (CsvMultiLineCellRenderer) table.getCellEditor();
        ((JTextArea) editor.getViewport().getView()).setText("Changed");
        editor.stopCellEditing();
        assertEquals("Changed", table.getModel().getValueAt(3, 1));
        assertEquals("1", table.getModel().getValueAt(3, 0));
    }

    public void testScrollableCellEditorDoesNotInterceptClicksOnFrozenCells() {
        CsvTable table = fileEditor.getTable();
        fileEditor.setFrozenColumnCount(2);
        assertTrue(table.editCellAt(3, 3));
        scroll(150, 60);
        Component editor = table.getEditorComponent();
        assertFalse(editor.contains(10, 5));
        assertTrue(editor.contains(75, 5));
        assertSame(table, table.findComponentAt(editor.getX() + 10, editor.getY() + 5));
        table.getCellEditor().cancelCellEditing();
    }

    public void testFrozenHeaderResizingPersistsOriginalColumnWidth() {
        CsvTable table = fileEditor.getTable();
        fileEditor.setFrozenColumnCount(2);
        scroll(230, 60);
        JTableHeader header = table.getTableHeader();
        int edge = header.getHeaderRect(0).x + header.getHeaderRect(0).width - 1;
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK,
                edge, 5, 1, false, MouseEvent.BUTTON1));
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_DRAGGED, 0, InputEvent.BUTTON1_DOWN_MASK,
                edge + 25, 5, 0, false, MouseEvent.NOBUTTON));
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_RELEASED, 0, 0,
                edge + 25, 5, 1, false, MouseEvent.BUTTON1));
        assertEquals(125, table.getColumnModel().getColumn(0).getWidth());
        assertEquals(125, fileEditor.getTableEditorState().getColumnWidths()[0]);
    }

    public void testCellAndColumnActionsUseOriginalModelIndices() {
        CsvTable table = fileEditor.getTable();
        fileEditor.setFrozenColumnCount(2);
        scroll(230, 60);
        table.changeSelection(3, 4, false, false);
        fileEditor.tableEditorActions.clearSelectedCells(fileEditor);
        assertEquals("", table.getModel().getValueAt(3, 4));
        assertEquals("1", table.getModel().getValueAt(3, 0));
        fileEditor.tableEditorActions.deleteSelectedColumns(fileEditor);
        assertEquals(5, table.getColumnCount());
        assertEquals("Platform", table.getModel().getValueAt(3, 4));
        assertEquals(2, fileEditor.getFrozenColumnCount());
        table.changeSelection(3, 1, false, false);
        fileEditor.tableEditorActions.addColumn(fileEditor, true);
        assertEquals("", table.getModel().getValueAt(3, 1));
        assertEquals("Alice", table.getModel().getValueAt(3, 2));
        assertEquals(2, fileEditor.getFrozenColumnCount());
    }

    public void testHeaderSelectionWorksWithLeadingCommentAndFrozenColumns() {
        CsvTable table = fileEditor.getTable();
        fileEditor.setFrozenColumnCount(2);
        scroll(230, 60);
        MouseEvent event = new MouseEvent(table.getTableHeader(), MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK,
                340, 5, 1, false, MouseEvent.BUTTON1);
        fileEditor.tableEditorMouseListener.mousePressed(event);
        assertEquals(1, table.getSelectedColumn());
    }

    public void testCommentRowsStillSpanTheTableAndRemainEditable() {
        CsvTable table = fileEditor.getTable();
        fileEditor.setFrozenColumnCount(2);
        scroll(230, 0);
        assertEquals(230, table.getCellRect(0, 0, true).x);
        assertEquals(viewport().getViewRect().width, table.getCellRect(0, 0, true).width);
        assertEquals(table.getCellRect(0, 0, true), table.getCellRect(0, 1, true));
        assertEquals(0, table.columnAtPoint(new Point(440, table.getRowHeight() / 2)));
        assertTrue(table.editCellAt(0, 0));
        paint(table, viewport().getViewRect());
        assertEquals(table.getCellRect(0, 0, false), table.getEditorComponent().getBounds());
        table.getCellEditor().cancelCellEditing();
    }

    public void testFreezeStateRestoresAndClampsAfterColumnsAreRemoved() {
        fileEditor.setFrozenColumnCount(2);
        Element element = new Element("state");
        fileEditor.getTableEditorState().write(getProject(), element);
        fileEditor.setFrozenColumnCount(0);
        fileEditor.setState(CsvTableEditorState.create(element, getProject(), fileEditor.getFile()));
        assertEquals(2, fileEditor.getFrozenColumnCount());
        fileEditor.removeColumns(List.of(1, 2, 3, 4, 5));
        assertEquals(1, fileEditor.getFrozenColumnCount());
        assertEquals(1, fileEditor.getTableEditorState().getFrozenColumnCount());
    }

    public void testResizingAndZoomingUseActualFrozenColumnWidths() {
        fileEditor.setFrozenColumnCount(2);
        fileEditor.getTable().getColumnModel().getColumn(0).setPreferredWidth(140);
        fileEditor.getTable().getColumnModel().getColumn(0).setWidth(140);
        scroll(230, 60);
        assertEquals(370, fileEditor.getTable().getCellRect(3, 1, true).x);
        assertEquals(240, fileEditor.getTable().getFrozenColumnWidth());
        fileEditor.changeFontSize(2);
        assertEquals(fileEditor.getTable().getColumnModel().getColumn(0).getWidth(),
                fileEditor.getTable().getCellRect(3, 1, true).x - viewport().getViewPosition().x);
        assertEquals(2, fileEditor.getFrozenColumnCount());
    }

    public void testFrozenColumnsRemainAvailableInReadOnlyEditor() {
        fileEditor.setEditable(false);
        assertContainsElements(Arrays.asList(fileEditor.getTable().getTableHeader().getMouseListeners()),
                fileEditor.tableEditorMouseListener);
        fileEditor.selectColumn(1, false);
        fileEditor.freezeColumnsThroughSelection();
        assertEquals(2, fileEditor.getFrozenColumnCount());
        for (AnAction action : new AnAction[]{new CsvTableEditorActions.AddColumnBefore(), new CsvTableEditorActions.DeleteSelectedColumns()}) {
            AnActionEvent event = event(action);
            action.update(event);
            assertFalse(event.getPresentation().isEnabled());
        }
        for (AnAction action : new AnAction[]{new CsvTableEditorActions.FreezeColumns(), new CsvTableEditorActions.UnfreezeColumns(),
                new CsvTableEditorActions.AdjustColumnWidths(), new CsvTableEditorActions.ResetColumnWidths()}) {
            AnActionEvent event = event(action);
            action.update(event);
            assertTrue(event.getPresentation().isEnabled());
        }
    }
}
