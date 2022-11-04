package net.seesharpsoft.intellij.plugins.csv.editor.table;

public interface CsvTableActions<T extends CsvTableEditor> {

    void addRow(T tableEditor, boolean before);

    void addColumn(T tableEditor, boolean before);

    void deleteSelectedRows(T tableEditor);

    void deleteSelectedColumns(T tableEditor);

    void clearSelectedCells(T tableEditor);

    default void adjustColumnWidths(T tableEditor) {
        tableEditor.adjustAllColumnWidths();
        tableEditor.updateEditorLayout();
    }

    default void resetColumnWidths(T tableEditor) {
        tableEditor.resetAllColumnWidths();
        tableEditor.updateEditorLayout();
    }
}
