package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.psi.PsiDocumentManager;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import javax.swing.table.TableColumn;
import java.util.Enumeration;

public class CsvTableEditorActionsTest extends CsvTableEditorSwingTestBase {

    public void testAddRowActionWithoutSelectedRow() {
        int initialRowCount = fileEditor.getTableModel().getRowCount();

        fileEditor.tableEditorActions.addRow.actionPerformed(null);

        assertEquals(initialRowCount, fileEditor.getTableModel().getRowCount());
    }

    public void testAddRowBeforeAction() {
        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.addRowBefore.actionPerformed(null);

        PsiDocumentManager.getInstance(fileEditor.getProject()).doPostponedOperationsAndUnblockDocument(fileEditor.getDocument());

        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialRowCount + 1, tableModel.getRowCount());
        assertEquals("", tableModel.getValue(1, 0));
        assertEquals("", tableModel.getValue(1, 1));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddRowAfterAction() {
        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.addRowAfter.actionPerformed(null);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialRowCount + 1, tableModel.getRowCount());
        assertEquals("", tableModel.getValue(2, 0));
        assertEquals("", tableModel.getValue(2, 1));
        assertEquals(2, fileEditor.getTable().getSelectedRow());
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddColumnBeforeAction() {
        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.addColumnBefore.actionPerformed(null);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialColumnCount + 1, tableModel.getColumnCount());
        assertEquals("", tableModel.getValue(0, 1));
        assertEquals("", tableModel.getValue(1, 1));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(2, fileEditor.getTable().getSelectedColumn());
    }

    public void testAddColumnAfterAction() {
        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.addColumnAfter.actionPerformed(null);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialColumnCount + 1, tableModel.getColumnCount());
        assertEquals("", tableModel.getValue(0, 2));
        assertEquals("", tableModel.getValue(1, 2));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testDeleteRowAction() {
        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.deleteRow.actionPerformed(null);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialRowCount - 1, tableModel.getRowCount());
        assertEquals("just another line with leading and trailing whitespaces", tableModel.getValue(1, 0));
        assertEquals("  and one more value  ", tableModel.getValue(1, 1));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
    }

    public void testDeleteAllRowsAction() {
        fileEditor.getTable().setRowSelectionInterval(0, 3);
        fileEditor.getTable().setColumnSelectionInterval(0, 0);
        fileEditor.tableEditorActions.deleteRow.actionPerformed(null);
        CsvTableModel tableModel = getTableModel();
        assertEquals(1, tableModel.getRowCount());
        assertEquals(1, tableModel.getColumnCount());
        assertEquals("", tableModel.getValue(0, 0));
    }

    public void testDeleteColumnAction() {
        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(1, 1);
        fileEditor.tableEditorActions.deleteColumn.actionPerformed(null);
        CsvTableModel tableModel = getTableModel();
        assertEquals(myInitialColumnCount - 1, tableModel.getColumnCount());
        assertEquals("Header1", tableModel.getValue(0, 0));
        assertEquals("this is column \"Header1\"", tableModel.getValue(1, 0));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
    }

    public void testDeleteAllColumnsAction() {
        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(0, 1);
        fileEditor.tableEditorActions.deleteColumn.actionPerformed(null);
        CsvTableModel tableModel = getTableModel();
        assertEquals(1, tableModel.getRowCount());
        assertEquals(1, tableModel.getColumnCount());
        assertEquals("", tableModel.getValue(0, 0));
    }

    public void testOpenTextEditor() {
        fileEditor.tableEditorActions.openTextEditor.linkSelected(null, null);

        assertInstanceOf(FileEditorManager.getInstance(this.getProject()).getSelectedEditor(myFixture.getFile().getVirtualFile()), TextEditor.class);
    }

    public void testAutoColumnWidthAction() {
        fileEditor.tableEditorActions.adjustColumnWidthAction.actionPerformed(null);

        assertTrue("first column is bigger than second", fileEditor.getTable().getColumnModel().getColumn(0).getWidth() > fileEditor.getTable().getColumnModel().getColumn(1).getWidth());
    }

    public void testResetColumnWidthAction() {
        fileEditor.tableEditorActions.adjustColumnWidthAction.actionPerformed(null);

        assertTrue("first column is bigger than second", fileEditor.getTable().getColumnModel().getColumn(0).getWidth() > fileEditor.getTable().getColumnModel().getColumn(1).getWidth());

        fileEditor.tableEditorActions.resetColumnWidthAction.actionPerformed(null);

        Enumeration<TableColumn> tableColumnEnumeration = fileEditor.getTable().getColumnModel().getColumns();
        int expectedWidth = CsvEditorSettings.getInstance().getTableDefaultColumnWidth();
        while (tableColumnEnumeration.hasMoreElements()) {
            TableColumn tableColumn = tableColumnEnumeration.nextElement();
            assertEquals(expectedWidth, tableColumn.getWidth());
        }
    }
}
