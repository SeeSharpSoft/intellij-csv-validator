package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.event.*;

public class CsvTableEditorChangeListener extends CsvTableEditorUtilBase implements TableModelListener, TableColumnModelListener {

    public CsvTableEditorChangeListener(CsvTableEditorSwing csvTableEditor) {
        super(csvTableEditor);
    }


    @Override
    public void columnAdded(TableColumnModelEvent e) {
        // on column added
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
        // on column removed
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        if (e.getFromIndex() != e.getToIndex() && csvTableEditor.getTable().getTableHeader().getDraggedColumn() == null) {
            csvTableEditor.finishColumnReordering();
        }
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
        csvTableEditor.storeCurrentTableLayout();
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        // on column selection changed
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        // on table data changed
    }
}
