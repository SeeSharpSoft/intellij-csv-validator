package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.*;
import javax.swing.event.*;

public class CsvTableEditorChangeListener extends CsvTableEditorUtilBase implements TableModelListener, TableColumnModelListener {

    private boolean columnHeightWillBeCalculated = false;
    private boolean columnPositionWillBeCalculated = false;

    public CsvTableEditorChangeListener(CsvTableEditorSwing csvTableEditor) {
        super(csvTableEditor);
    }


    @Override
    public void columnAdded(TableColumnModelEvent e) {
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
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
                        csvTableEditor.syncTableModelWithUI();
                        columnPositionWillBeCalculated = false;
                    }
                }
            });
        }
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
        JTable table = csvTableEditor.getTable();
        if (!columnHeightWillBeCalculated && table.getTableHeader().getResizingColumn() != null) {
            columnHeightWillBeCalculated = true;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (table.getTableHeader().getResizingColumn() != null) {
                        SwingUtilities.invokeLater(this);
                    } else {
                        csvTableEditor.storeCurrentTableLayout();
                        csvTableEditor.updateRowHeights(null);
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
        this.csvTableEditor.updateRowHeights(e);
    }
}
