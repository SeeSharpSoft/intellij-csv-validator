package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import org.mockito.Mockito;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class CsvTableEditorMouseListenerTest extends CsvTableEditorSwingTestBase {

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

        Mockito.doReturn(0).when(spiedTable).columnAtPoint(Mockito.any());
        spiedMouseListener.mousePressed(mouseEvent);

        assertEquals(0, spiedTable.getSelectionModel().getMinSelectionIndex());
        assertEquals(3, spiedTable.getSelectionModel().getMaxSelectionIndex());
        assertEquals(0, spiedTable.getColumnModel().getSelectionModel().getMinSelectionIndex());
        assertEquals(0, spiedTable.getColumnModel().getSelectionModel().getMaxSelectionIndex());

        Mockito.doReturn(1).when(spiedTable).columnAtPoint(Mockito.any());
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

        Mockito.doReturn(0).when(spiedTable).columnAtPoint(Mockito.any());
        spiedMouseListener.mousePressed(mouseEvent);

        assertEquals(0, spiedTable.getSelectionModel().getMinSelectionIndex());
        assertEquals(3, spiedTable.getSelectionModel().getMaxSelectionIndex());
        assertEquals(0, spiedTable.getColumnModel().getSelectionModel().getMinSelectionIndex());
        assertEquals(0, spiedTable.getColumnModel().getSelectionModel().getMaxSelectionIndex());

        Mockito.doReturn(1).when(spiedTable).columnAtPoint(Mockito.any());
        spiedMouseListener.mousePressed(mouseEvent);

        assertEquals(0, spiedTable.getSelectionModel().getMinSelectionIndex());
        assertEquals(3, spiedTable.getSelectionModel().getMaxSelectionIndex());
        assertEquals(0, spiedTable.getColumnModel().getSelectionModel().getMinSelectionIndex());
        assertEquals(1, spiedTable.getColumnModel().getSelectionModel().getMaxSelectionIndex());
    }
}
