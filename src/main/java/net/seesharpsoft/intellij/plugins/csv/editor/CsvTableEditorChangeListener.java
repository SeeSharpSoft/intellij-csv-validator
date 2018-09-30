package net.seesharpsoft.intellij.plugins.csv.editor;

import javax.swing.event.*;

public class CsvTableEditorChangeListener extends CsvTableEditorUtilBase implements TableModelListener, TableColumnModelListener {

    private int movedColumnIndex = -1;
    private int targetColumnIndex = -1;

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
        System.out.println("Column added: " + e.getSource());
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
        System.out.println("Column removed: " + e.getSource());
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

        System.out.println("Column MOVED: " + e.getFromIndex() + "=>" + e.getToIndex());
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {

    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {

    }

    @Override
    public void tableChanged(TableModelEvent e) {
        this.csvTableEditor.storeStateChange(true);
        System.out.println("tableChanged: " + e.getSource());
    }
}
