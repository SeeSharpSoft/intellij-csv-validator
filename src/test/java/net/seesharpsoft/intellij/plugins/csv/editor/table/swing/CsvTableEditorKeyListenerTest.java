package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class CsvTableEditorKeyListenerTest extends CsvTableEditorSwingTestBase {

    public void testAddRowBeforeAction() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(initialState.length + 1, newState.length);
        assertEquals(null, newState[0][0]);
        assertEquals(null, newState[0][1]);

        fileEditor.tableEditorActions.undo.actionPerformed(null);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(initialState.length + 1, newState.length);
        assertEquals(null, newState[1][0]);
        assertEquals(null, newState[1][1]);
        assertEquals(2, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddRowAfterAction() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(initialState.length + 1, newState.length);
        assertEquals(null, newState[0][0]);
        assertEquals(null, newState[0][1]);

        fileEditor.tableEditorActions.undo.actionPerformed(null);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(initialState.length + 1, newState.length);
        assertEquals(null, newState[2][0]);
        assertEquals(null, newState[2][1]);
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddColumnBeforeAction() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(null, newState[0][2]);
        assertEquals(null, newState[1][2]);

        fileEditor.tableEditorActions.undo.actionPerformed(null);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(null, newState[0][1]);
        assertEquals(null, newState[1][1]);
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(2, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddColumnAfterAction() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(null, newState[0][2]);
        assertEquals(null, newState[1][2]);

        fileEditor.tableEditorActions.undo.actionPerformed(null);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(null, newState[0][2]);
        assertEquals(null, newState[1][2]);
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testDeleteRowActionByDelete() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        assertTrue(fileEditor.getDataHandler().equalsCurrentState(initialState));

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(3, newState.length);
        assertEquals("just another line with leading and trailing whitespaces", newState[1][0]);
        assertEquals("  and one more value  ", newState[1][1]);
    }

    public void testDeleteRowActionByBackSpace() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_BACK_SPACE, KeyEvent.CHAR_UNDEFINED);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        assertTrue(fileEditor.getDataHandler().equalsCurrentState(initialState));

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(3, newState.length);
        assertEquals("just another line with leading and trailing whitespaces", newState[1][0]);
        assertEquals("  and one more value  ", newState[1][1]);
    }

    public void testDeleteColumnActionByDelete() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        assertTrue(fileEditor.getDataHandler().equalsCurrentState(initialState));

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(1, newState[0].length);
        assertEquals(1, newState[1].length);
        assertEquals("Header1", newState[0][0]);
        assertEquals("this is column \"Header1\"", newState[1][0]);
    }

    public void testDeleteColumnActionByBackspace() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_BACK_SPACE, KeyEvent.CHAR_UNDEFINED);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        assertTrue(fileEditor.getDataHandler().equalsCurrentState(initialState));

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(1, newState[0].length);
        assertEquals(1, newState[1].length);
        assertEquals("Header1", newState[0][0]);
        assertEquals("this is column \"Header1\"", newState[1][0]);
    }

    public void testStartEditingActionByEnter() {
        KeyEvent keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        assertEquals(-1, fileEditor.getTable().getEditingColumn());
        assertEquals(-1, fileEditor.getTable().getEditingRow());

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        assertEquals(1, fileEditor.getTable().getEditingColumn());
        assertEquals(1, fileEditor.getTable().getEditingRow());

        // stop editing
        keyEvent = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, JComponent.WHEN_FOCUSED,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);

        fileEditor.tableEditorKeyListener.keyReleased(keyEvent);
        assertEquals(-1, fileEditor.getTable().getEditingColumn());
        assertEquals(-1, fileEditor.getTable().getEditingRow());
    }

}
