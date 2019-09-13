package net.seesharpsoft.intellij.plugins.csv.editor.table.api;

import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;

public interface TableActions<T extends CsvTableEditor> {

    void addRow(T tableEditor, boolean before);

    void addColumn(T tableEditor, boolean before);

    void deleteSelectedRows(T tableEditor);

    void deleteSelectedColumns(T tableEditor);

    void clearSelectedCells(T tableEditor);

    default void adjustColumnWidths(T tableEditor) {
        tableEditor.adjustAllColumnWidths();
    }

    default void resetColumnWidths(T tableEditor) {
        tableEditor.resetAllColumnWidths();
    }

    default void undoLastAction(T tableEditor) {
        if (tableEditor.getDataHandler().canGetLastState()) {
            tableEditor.updateTableComponentData(tableEditor.getDataHandler().getLastState());
        }
    }

    default void redoLastAction(T tableEditor) {
        if (tableEditor.getDataHandler().canGetNextState()) {
            tableEditor.updateTableComponentData(tableEditor.getDataHandler().getNextState());
        }
    }
}
