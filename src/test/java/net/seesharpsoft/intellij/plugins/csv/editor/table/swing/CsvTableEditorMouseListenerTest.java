package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import static org.junit.Assert.assertArrayEquals;

public class CsvTableEditorMouseListenerTest extends CsvTableEditorSwingTestBase {

    public void testSingleClickSelectsCellWithoutEditing() {
        JTable table = fileEditor.getTable();
        table.setSize(600, 300);

        clickCell(table, 1, 1, 1);

        assertFalse("A single click must select the cell without opening its editor", table.isEditing());
        assertArrayEquals(new int[]{1}, table.getSelectedRows());
        assertArrayEquals(new int[]{1}, table.getSelectedColumns());
    }

    public void testMouseDragSelectsRectangularCellRangeWithoutEditing() {
        JTable table = fileEditor.getTable();
        table.setSize(600, 300);
        String original = myFixture.getFile().getText();

        dispatchCellMouse(table, MouseEvent.MOUSE_PRESSED, 1, 0, 1, InputEvent.BUTTON1_DOWN_MASK);
        dispatchCellMouse(table, MouseEvent.MOUSE_DRAGGED, 2, 1, 0, InputEvent.BUTTON1_DOWN_MASK);
        dispatchCellMouse(table, MouseEvent.MOUSE_RELEASED, 2, 1, 1, 0);

        assertFalse("Dragging must extend cell selection rather than edit cell text", table.isEditing());
        assertArrayEquals(new int[]{1, 2}, table.getSelectedRows());
        assertArrayEquals(new int[]{0, 1}, table.getSelectedColumns());
        assertEquals(original, myFixture.getFile().getText());
    }

    public void testDoubleClickEditsCellAndUpdatesCsv() {
        JTable table = fileEditor.getTable();
        table.setSize(600, 300);
        clickCell(table, 1, 1, 1);

        clickCell(table, 1, 1, 2);

        assertTrue(table.isEditing());
        assertEquals(1, table.getEditingRow());
        assertEquals(1, table.getEditingColumn());
        JTextArea text = (JTextArea) ((JScrollPane) table.getEditorComponent()).getViewport().getView();
        text.setText("Updated value");
        assertTrue(table.getCellEditor().stopCellEditing());
        assertEquals("Updated value", getTableModel().getValue(1, 1));
    }

    public void testCommentCellRequiresDoubleClickToEdit() {
        fileEditor.dispose();
        myFixture.configureByText(CsvFileType.INSTANCE, "# comment\na,b\n1,2\n");
        fileEditor = createEditor();
        fileEditor.selectNotify();
        JTable table = fileEditor.getTable();
        table.setSize(600, 300);

        clickCell(table, 0, 1, 1);

        assertFalse(table.isEditing());
        assertEquals(0, table.getSelectedRow());
        assertEquals(0, table.getSelectedColumn());

        clickCell(table, 0, 1, 2);

        assertTrue(table.isEditing());
        assertEquals(0, table.getEditingRow());
        assertEquals(0, table.getEditingColumn());
        JTextArea text = (JTextArea) ((JScrollPane) table.getEditorComponent()).getViewport().getView();
        text.setText("Updated comment");
        assertTrue(table.getCellEditor().stopCellEditing());
        assertEquals("Updated comment", getTableModel().getValue(0, 0));
    }

    private static void clickCell(JTable table, int row, int column, int clickCount) {
        dispatchCellMouse(table, MouseEvent.MOUSE_PRESSED, row, column, clickCount, InputEvent.BUTTON1_DOWN_MASK);
        dispatchCellMouse(table, MouseEvent.MOUSE_RELEASED, row, column, clickCount, 0);
        dispatchCellMouse(table, MouseEvent.MOUSE_CLICKED, row, column, clickCount, 0);
    }

    private static void dispatchCellMouse(JTable table, int id, int row, int column, int clickCount, int modifiers) {
        Rectangle cell = table.getCellRect(row, column, true);
        table.dispatchEvent(new MouseEvent(table, id, System.currentTimeMillis(), modifiers,
                cell.x + cell.width / 2, cell.y + cell.height / 2, clickCount, false,
                id == MouseEvent.MOUSE_DRAGGED ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1));
    }

    private static class MockResult<T> {
        T result;

        public MockResult(T initial) {
            result = initial;
        }

        public T getResult() {
            return result;
        }

        public void setResult(T myResult) {
            result = myResult;
        }
    }

    private void mockShowColumnPopupMenu(CsvTableEditorMouseListener spiedMouseListener, MockResult<Boolean> mockResult) {
        Mockito.doAnswer(invocation -> {
            assertSame(spiedMouseListener.getColumnPopupMenu(), invocation.getArgument(0));
            assertSame(fileEditor.getTable().getTableHeader(), invocation.getArgument(1));
            mockResult.setResult(true);
            return null;
        }).when(spiedMouseListener).showPopupMenu(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
    }

    private void mockShowRowPopupMenu(CsvTableEditorMouseListener spiedMouseListener, MockResult<Boolean> mockResult) {
        Mockito.doAnswer(invocation -> {
            assertSame(spiedMouseListener.getRowPopupMenu(), invocation.getArgument(0));
            assertSame(fileEditor.getTable(), invocation.getArgument(1));
            mockResult.setResult(true);
            return null;
        }).when(spiedMouseListener).showPopupMenu(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
    }

    public void testColumnPopupMenuPressed() {
        MouseEvent mouseEvent = new MouseEvent(fileEditor.getTable().getTableHeader(), MouseEvent.MOUSE_PRESSED, JComponent.WHEN_FOCUSED,
                0, 0, 0, 1, true);

        CsvTableEditorMouseListener spiedMouseListener = Mockito.spy(fileEditor.tableEditorMouseListener);

        final MockResult<Boolean> mockResult = new MockResult(false);
        mockShowColumnPopupMenu(spiedMouseListener, mockResult);

        spiedMouseListener.mousePressed(mouseEvent);

        assertTrue(mockResult.getResult());
    }

    public void testRowPopupMenuPressed() {
        MouseEvent mouseEvent = new MouseEvent(fileEditor.getTable(), MouseEvent.MOUSE_PRESSED, JComponent.WHEN_FOCUSED,
                0, 0, 0, 1, true);

        CsvTableEditorMouseListener spiedMouseListener = Mockito.spy(fileEditor.tableEditorMouseListener);

        final MockResult<Boolean> mockResult = new MockResult(false);
        mockShowRowPopupMenu(spiedMouseListener, mockResult);

        spiedMouseListener.mousePressed(mouseEvent);

        assertTrue(mockResult.getResult());
    }

    public void testColumnPopupMenuReleased() {
        MouseEvent mouseEvent = new MouseEvent(fileEditor.getTable().getTableHeader(), MouseEvent.MOUSE_RELEASED, JComponent.WHEN_FOCUSED,
                0, 0, 0, 1, true);

        CsvTableEditorMouseListener spiedMouseListener = Mockito.spy(fileEditor.tableEditorMouseListener);

        final MockResult<Boolean> mockResult = new MockResult(false);
        mockShowColumnPopupMenu(spiedMouseListener, mockResult);

        spiedMouseListener.mouseReleased(mouseEvent);

        assertTrue(mockResult.getResult());
    }

    public void testRowPopupMenuReleased() {
        MouseEvent mouseEvent = new MouseEvent(fileEditor.getTable(), MouseEvent.MOUSE_RELEASED, JComponent.WHEN_FOCUSED,
                0, 0, 0, 1, true);

        CsvTableEditorMouseListener spiedMouseListener = Mockito.spy(fileEditor.tableEditorMouseListener);

        final MockResult<Boolean> mockResult = new MockResult(false);
        mockShowRowPopupMenu(spiedMouseListener, mockResult);

        spiedMouseListener.mouseReleased(mouseEvent);

        assertTrue(mockResult.getResult());
    }

    public void testSelectNew() {
        MouseEvent mouseEvent = new MouseEvent(fileEditor.getTable().getTableHeader(), MouseEvent.MOUSE_PRESSED, JComponent.WHEN_FOCUSED,
                InputEvent.BUTTON1_MASK, 0, 0, 1, true);

        CsvTableEditorMouseListener spiedMouseListener = Mockito.spy(fileEditor.tableEditorMouseListener);
        final MockResult<Boolean> mockResult = new MockResult(false);
        mockShowColumnPopupMenu(spiedMouseListener, mockResult);

        JTable spiedTable = Mockito.spy(fileEditor.getTable());
        CsvTableEditorSwing spiedTableEditorSwing = Mockito.spy(fileEditor);
        Mockito.doReturn(spiedTable).when(spiedTableEditorSwing).getTable();
        spiedMouseListener.csvTableEditor = spiedTableEditorSwing;

        assertEquals(-1, spiedTable.getSelectedColumn());

        spiedMouseListener.mousePressed(mouseEvent);

        assertEquals(0, spiedTable.getSelectionModel().getMinSelectionIndex());
        assertEquals(3, spiedTable.getSelectionModel().getMaxSelectionIndex());
        assertEquals(0, spiedTable.getColumnModel().getSelectionModel().getMinSelectionIndex());
        assertEquals(0, spiedTable.getColumnModel().getSelectionModel().getMaxSelectionIndex());

        mouseEvent = new MouseEvent(fileEditor.getTable().getTableHeader(), MouseEvent.MOUSE_PRESSED, JComponent.WHEN_FOCUSED,
                InputEvent.BUTTON1_MASK, spiedTable.getTableHeader().getHeaderRect(1).x + 1, 0, 1, true);
        spiedMouseListener.mousePressed(mouseEvent);

        assertEquals(0, spiedTable.getSelectionModel().getMinSelectionIndex());
        assertEquals(3, spiedTable.getSelectionModel().getMaxSelectionIndex());
        assertEquals(1, spiedTable.getColumnModel().getSelectionModel().getMinSelectionIndex());
        assertEquals(1, spiedTable.getColumnModel().getSelectionModel().getMaxSelectionIndex());
    }

    public void testSelectAppend() {
        MouseEvent mouseEvent = new MouseEvent(fileEditor.getTable().getTableHeader(), MouseEvent.MOUSE_PRESSED, JComponent.WHEN_FOCUSED,
                InputEvent.BUTTON1_MASK | InputEvent.SHIFT_MASK, 0, 0, 1, true);

        CsvTableEditorMouseListener spiedMouseListener = Mockito.spy(fileEditor.tableEditorMouseListener);
        final MockResult<Boolean> mockResult = new MockResult(false);
        mockShowColumnPopupMenu(spiedMouseListener, mockResult);

        JTable spiedTable = Mockito.spy(fileEditor.getTable());
        CsvTableEditorSwing spiedTableEditorSwing = Mockito.spy(fileEditor);
        Mockito.doReturn(spiedTable).when(spiedTableEditorSwing).getTable();
        spiedMouseListener.csvTableEditor = spiedTableEditorSwing;

        assertEquals(-1, spiedTable.getSelectedColumn());

        spiedMouseListener.mousePressed(mouseEvent);

        assertEquals(0, spiedTable.getSelectionModel().getMinSelectionIndex());
        assertEquals(3, spiedTable.getSelectionModel().getMaxSelectionIndex());
        assertEquals(0, spiedTable.getColumnModel().getSelectionModel().getMinSelectionIndex());
        assertEquals(0, spiedTable.getColumnModel().getSelectionModel().getMaxSelectionIndex());

        mouseEvent = new MouseEvent(fileEditor.getTable().getTableHeader(), MouseEvent.MOUSE_PRESSED, JComponent.WHEN_FOCUSED,
                InputEvent.BUTTON1_MASK | InputEvent.SHIFT_MASK, spiedTable.getTableHeader().getHeaderRect(1).x + 1, 0, 1, true);
        spiedMouseListener.mousePressed(mouseEvent);

        assertEquals(0, spiedTable.getSelectionModel().getMinSelectionIndex());
        assertEquals(3, spiedTable.getSelectionModel().getMaxSelectionIndex());
        assertEquals(0, spiedTable.getColumnModel().getSelectionModel().getMinSelectionIndex());
        assertEquals(1, spiedTable.getColumnModel().getSelectionModel().getMaxSelectionIndex());
    }
}
