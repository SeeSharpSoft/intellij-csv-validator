package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;

public class CsvTableEditorActionsTest extends CsvTableEditorSwingTestBase {

    public void testUndoRedoAction() {
        Object[][] newState = changeValue("new value", 1, 1);
        assertTrue(fileEditor.isModified());
        assertTrue(fileEditor.getDataHandler().equalsCurrentState(newState));

        fileEditor.tableEditorActions.undo.actionPerformed(null);
        assertFalse(fileEditor.isModified());
        assertTrue(fileEditor.getDataHandler().equalsCurrentState(initialState));

        fileEditor.tableEditorActions.redo.actionPerformed(null);
        assertTrue(fileEditor.isModified());
        assertTrue(fileEditor.getDataHandler().equalsCurrentState(newState));
    }

    public void testAddRowAction() {
        fileEditor.tableEditorActions.addRow.actionPerformed(null);

        Object[][] newState = fileEditor.getDataHandler().getCurrentState();

        assertEquals(initialState.length + 1, newState.length);
        assertEquals(null, newState[newState.length - 1][0]);
        assertEquals(null, newState[newState.length - 1][1]);
    }

    public void testAddRowBeforeAction() {
        fileEditor.tableEditorActions.addRowBefore.actionPerformed(null);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(initialState.length + 1, newState.length);
        assertEquals(null, newState[0][0]);
        assertEquals(null, newState[0][1]);

        fileEditor.tableEditorActions.undo.actionPerformed(null);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.addRowBefore.actionPerformed(null);
        newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(initialState.length + 1, newState.length);
        assertEquals(null, newState[1][0]);
        assertEquals(null, newState[1][1]);
        assertEquals(2, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddRowAfterAction() {
        fileEditor.tableEditorActions.addRowAfter.actionPerformed(null);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(initialState.length + 1, newState.length);
        assertEquals("Header1", newState[0][0]);
        assertEquals("header 2", newState[0][1]);
        assertEquals(null, newState[4][0]);
        assertEquals(null, newState[4][1]);

        fileEditor.tableEditorActions.undo.actionPerformed(null);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.addRowAfter.actionPerformed(null);
        newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(initialState.length + 1, newState.length);
        assertEquals(null, newState[2][0]);
        assertEquals(null, newState[2][1]);
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddColumnAction() {
        fileEditor.tableEditorActions.addColumn.actionPerformed(null);

        Object[][] newState = fileEditor.getDataHandler().getCurrentState();

        assertEquals(null, newState[0][2]);
        assertEquals(null, newState[1][2]);
    }

    public void testAddColumnBeforeAction() {
        fileEditor.tableEditorActions.addColumnBefore.actionPerformed(null);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals("header 2", newState[0][2]);
        assertEquals("this is column header 2", newState[1][2]);
        assertEquals(null, newState[0][0]);
        assertEquals(null, newState[1][0]);

        fileEditor.tableEditorActions.undo.actionPerformed(null);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.addColumnBefore.actionPerformed(null);
        newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(null, newState[0][1]);
        assertEquals(null, newState[1][1]);
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(2, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddColumnAfterAction() {
        fileEditor.tableEditorActions.addColumnAfter.actionPerformed(null);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(null, newState[0][2]);
        assertEquals(null, newState[1][2]);

        fileEditor.tableEditorActions.undo.actionPerformed(null);

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.addColumnAfter.actionPerformed(null);
        newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(null, newState[0][2]);
        assertEquals(null, newState[1][2]);
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testDeleteRowAction() {
        fileEditor.tableEditorActions.deleteRow.actionPerformed(null);
        assertTrue(fileEditor.getDataHandler().equalsCurrentState(initialState));

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.deleteRow.actionPerformed(null);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(3, newState.length);
        assertEquals("just another line with leading and trailing whitespaces", newState[1][0]);
        assertEquals("  and one more value  ", newState[1][1]);
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testDeleteColumnAction() {
        fileEditor.tableEditorActions.deleteColumn.actionPerformed(null);
        assertTrue(fileEditor.getDataHandler().equalsCurrentState(initialState));

        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.deleteColumn.actionPerformed(null);
        Object[][] newState = fileEditor.getDataHandler().getCurrentState();
        assertEquals(1, newState[0].length);
        assertEquals(1, newState[1].length);
        assertEquals("Header1", newState[0][0]);
        assertEquals("this is column \"Header1\"", newState[1][0]);
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
    }

    public void testOpenTextEditor() {
        fileEditor.tableEditorActions.openTextEditor.linkSelected(null, null);

        assertInstanceOf(FileEditorManager.getInstance(this.getProject()).getSelectedEditor(this.getFile().getVirtualFile()), TextEditor.class);
    }
}
