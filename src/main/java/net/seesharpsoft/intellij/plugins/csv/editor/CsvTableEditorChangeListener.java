package net.seesharpsoft.intellij.plugins.csv.editor;

import javax.swing.*;
import javax.swing.event.*;

public class CsvTableEditorChangeListener extends CsvTableEditorUtilBase implements TableModelListener, TableColumnModelListener {

    private int movedColumnIndex = -1;
    private int targetColumnIndex = -1;
    private boolean columnHeightWillBeCalculated = false;

    public CsvTableEditorChangeListener(CsvTableEditor csvTableEditor) {
        super(csvTableEditor);
    }

    @Override
    protected void onEditorUpdated() {
        movedColumnIndex = -1;
        targetColumnIndex = -1;
    }

    @Override
    public void columnAdded(TableColumnModelEvent e) {
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        if (e.getFromIndex() == e.getToIndex()) {
            return;
        }
        if (movedColumnIndex == -1) {
            movedColumnIndex = e.getFromIndex();
        }
        targetColumnIndex = e.getToIndex();
        csvTableEditor.requiresEditorUpdate();
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
        JTable table = csvTableEditor.getTable();
        if (!columnHeightWillBeCalculated && table.getTableHeader().getResizingColumn() != null) {
            columnHeightWillBeCalculated = true;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // table.getTableHeader().getResizingColumn() is != null as long as the user still is holding the mouse down
                    // To avoid going over all data every few milliseconds wait for user to release
                    if (table.getTableHeader().getResizingColumn() != null) {
                        SwingUtilities.invokeLater(this);
                    } else {
                        tableChanged(null);
                        columnHeightWillBeCalculated = false;
                    }
                }
            });
        }
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {

    }

    @Override
    public void tableChanged(TableModelEvent e) {
        this.csvTableEditor.storeStateChange(true);
    }
}
