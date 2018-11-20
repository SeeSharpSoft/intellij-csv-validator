package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;

public class CsvTableEditorChangeListenerTest extends CsvTableEditorSwingTestBase {

    // TODO more meaningful test for columnMoved and columnMarginChanged
    public void testFunctionsNotFaulty() {
        TableColumnModelEvent event = new TableColumnModelEvent(fileEditor.getTable().getColumnModel(), 0, 1);
        fileEditor.tableEditorListener.columnAdded(event);
        fileEditor.tableEditorListener.columnRemoved(event);
        fileEditor.tableEditorListener.columnMoved(event);

        fileEditor.tableEditorListener.columnMarginChanged(new ChangeEvent(fileEditor.getTable().getColumnModel()));
        fileEditor.tableEditorListener.columnSelectionChanged(new ListSelectionEvent(fileEditor.getTable().getColumnModel(), 0, 1, true));
        fileEditor.tableEditorListener.tableChanged(new TableModelEvent(fileEditor.getTableModel()));
        assertTrue(true);
    }

}
