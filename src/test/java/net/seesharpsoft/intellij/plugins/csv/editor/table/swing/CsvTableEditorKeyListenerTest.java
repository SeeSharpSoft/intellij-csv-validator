package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class CsvTableEditorKeyListenerTest extends CsvTableEditorSwingTestBase {

    public void testAddRowBeforeAction() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialRowCount + 1, tableModel.getRowCount());
        assertEquals("", tableModel.getValue(1, 0));
        assertEquals("", tableModel.getValue(1, 1));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddRowAfterAction() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialRowCount + 1, tableModel.getRowCount());
        assertEquals("", tableModel.getValue(2, 0));
        assertEquals("", tableModel.getValue(2, 1));
        assertEquals(2, fileEditor.getTable().getSelectedRow());
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddColumnBeforeAction() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialColumnCount + 1, tableModel.getColumnCount());
        assertEquals("", tableModel.getValue(0, 1));
        assertEquals("", tableModel.getValue(1, 1));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(2, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddColumnAfterAction() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialColumnCount + 1, tableModel.getColumnCount());
        assertEquals("", tableModel.getValue(0, 2));
        assertEquals("", tableModel.getValue(1, 2));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testDeleteRowActionByDelete() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialRowCount - 1, tableModel.getRowCount());
        assertEquals(" just another line with leading and trailing whitespaces  ", tableModel.getValue(1, 0));
        assertEquals("  and one more value  ", tableModel.getValue(1, 1));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testDeleteRowActionByBackSpace() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_BACK_SPACE, KeyEvent.CHAR_UNDEFINED);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialRowCount - 1, tableModel.getRowCount());
        assertEquals(" just another line with leading and trailing whitespaces  ", tableModel.getValue(1, 0));
        assertEquals("  and one more value  ", tableModel.getValue(1, 1));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testDeleteColumnActionByDelete() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialColumnCount - 1, tableModel.getColumnCount());
        assertEquals("Header1", tableModel.getValue(0, 0));
        assertEquals("this is column \"Header1\"", tableModel.getValue(1, 0));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
    }

    public void testDeleteColumnActionByBackspace() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_BACK_SPACE, KeyEvent.CHAR_UNDEFINED);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialColumnCount - 1, tableModel.getColumnCount());
        assertEquals("Header1", tableModel.getValue(0, 0));
        assertEquals("this is column \"Header1\"", tableModel.getValue(1, 0));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
    }

    public void testStartEditingActionByEnter() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        assertEquals(-1, fileEditor.getTable().getEditingColumn());
        assertEquals(-1, fileEditor.getTable().getEditingRow());

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(0, 0);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        assertEquals(0, fileEditor.getTable().getEditingColumn());
        assertEquals(1, fileEditor.getTable().getEditingRow());

        KeyEvent actualEnterKey = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        fileEditor.tableEditorKeyListener.keyReleased(actualEnterKey);
        assertEquals(0, fileEditor.getTable().getEditingColumn());
        assertEquals(1, fileEditor.getTable().getEditingRow());

        // stop editing
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        assertEquals(1, fileEditor.getTable().getEditingColumn());
        assertEquals(1, fileEditor.getTable().getEditingRow());
    }

    public void testClearCellActionByDelete() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                0, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        CsvTableModel tableModel = getTableModel();
        assertEquals("", tableModel.getValue(1, 1));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testClearCellActionByBackspace() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                0, KeyEvent.VK_BACK_SPACE, KeyEvent.CHAR_UNDEFINED);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        CsvTableModel tableModel = getTableModel();
        assertEquals("", tableModel.getValue(1, 1));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testNotClearCellActionByDeleteWhenEditing() {
        KeyEvent enterKeyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
        KeyEvent deleteKeyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                0, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);

        fileEditor.tableEditorKeyListener.keyReleased(enterKeyEvent);
        fileEditor.tableEditorKeyListener.keyReleased(deleteKeyEvent);

        CsvTableModel tableModel = getTableModel();
        assertEquals("this is column header 2", tableModel.getValue(1, 1));
    }
}
