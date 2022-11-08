package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.*;
import javax.swing.event.*;

public class CsvTableEditorChangeListener extends CsvTableEditorUtilBase implements TableModelListener, TableColumnModelListener {

    private boolean columnPositionWillBeCalculated = false;

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
        JTable table = csvTableEditor.getTable();
        if (!columnPositionWillBeCalculated && table.getTableHeader().getDraggedColumn() != null && e.getFromIndex() != e.getToIndex()) {
            columnPositionWillBeCalculated = true;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (table.getTableHeader().getDraggedColumn() != null) {
                        SwingUtilities.invokeLater(this);
                    } else {
                        columnPositionWillBeCalculated = false;
                    }
                }
            });
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
