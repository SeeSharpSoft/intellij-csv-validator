package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableActions;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CsvTableEditorActionListeners extends CsvTableEditorUtilBase implements CsvTableActions<CsvTableEditorSwing> {

    protected ActionListener addRow = event -> addRow(csvTableEditor, false);
    protected ActionListener addRowBefore = event -> addRow(csvTableEditor, true);
    protected ActionListener addRowAfter = event -> addRow(csvTableEditor, false);
    protected ActionListener addColumnBefore = event -> addColumn(csvTableEditor, true);
    protected ActionListener addColumnAfter = event -> addColumn(csvTableEditor, false);
    protected ActionListener deleteRow = event -> deleteSelectedRows(csvTableEditor);
    protected ActionListener deleteColumn = event -> deleteSelectedColumns(csvTableEditor);
    protected ActionListener clearCells = event -> clearSelectedCells(csvTableEditor);
    protected ActionListener adjustColumnWidthAction = event -> adjustColumnWidths(csvTableEditor);
    protected ActionListener resetColumnWidthAction = event -> resetColumnWidths(csvTableEditor);

    protected LinkListener openTextEditor = new OpenTextEditor();
    protected LinkListener openCsvPluginLink = new OpenCsvPluginLink();

    public CsvTableEditorActionListeners(CsvTableEditorSwing tableEditor) {
        super(tableEditor);
    }

    @Override
    public void addRow(CsvTableEditorSwing tableEditor, boolean before) {
        if (tableEditor.getTableModel().hasErrors()) {
            return;
        }

        tableEditor.removeTableChangeListener();
        try {
            JTable table = tableEditor.getTable();
            int currentRow = table.getSelectedRow();
            if (currentRow == -1) return;

            tableEditor.addRow(table.convertRowIndexToModel(currentRow), before);

            int targetRow = before ? currentRow : currentRow + 1;
            int targetColumn = 0;
            selectCell(tableEditor.getTable(), targetRow, targetColumn);
        } finally {
            tableEditor.applyTableChangeListener();
        }
    }

    @Override
    public void addColumn(CsvTableEditorSwing tableEditor, boolean before) {
        if (tableEditor.getTableModel().hasErrors()) {
            return;
        }

        tableEditor.removeTableChangeListener();
        try {
            JTable table = tableEditor.getTable();
            int currentColumn = table.getSelectedColumn();
            int currentRow = table.getSelectedRow();
            if (currentColumn == -1 || currentRow == -1) return;

            tableEditor.addColumn(table.convertColumnIndexToModel(currentColumn), before);

            selectCell(table, currentRow, before ? currentColumn + 1 : currentColumn);
        } finally {
            tableEditor.applyTableChangeListener();
        }
    }

    @Override
    public void deleteSelectedRows(CsvTableEditorSwing tableEditor) {
        if (tableEditor.getTableModel().hasErrors()) {
            return;
        }

        tableEditor.removeTableChangeListener();
        try {
            JTable table = tableEditor.getTable();
            int[] currentRows = tableEditor.getTable().getSelectedRows();
            if (currentRows == null || currentRows.length == 0) {
                return;
            }
            int currentColumn = table.getSelectedColumn();

            tableEditor.removeRows(Arrays.stream(currentRows).map(row -> table.convertRowIndexToModel(row)).boxed().collect(Collectors.toList()));

            selectCell(table, currentRows[0], currentColumn);
        } finally {
            tableEditor.applyTableChangeListener();
        }
    }

    @Override
    public void deleteSelectedColumns(CsvTableEditorSwing tableEditor) {
        if (tableEditor.getTableModel().hasErrors()) {
            return;
        }

        tableEditor.removeTableChangeListener();
        try {
            JTable table = tableEditor.getTable();
            int[] selectedColumns = table.getSelectedColumns();
            if (selectedColumns == null || selectedColumns.length == 0) {
                return;
            }
            int focusedRow = table.getSelectedRow();
            final int columnCount = table.getColumnCount();

            tableEditor.removeColumns(
                    Arrays.stream(selectedColumns)
                            .filter(selectedColumn -> selectedColumn >= 0 && selectedColumn < columnCount)
                            .map(col -> table.convertColumnIndexToModel(col))
                            .boxed()
                            .collect(Collectors.toList())
            );

            selectCell(table, focusedRow, selectedColumns[0]);
        } finally {
            tableEditor.applyTableChangeListener();
        }
    }

    @Override
    public void clearSelectedCells(CsvTableEditorSwing tableEditor) {
        if (tableEditor.getTableModel().hasErrors()) {
            return;
        }

        tableEditor.removeTableChangeListener();
        try {
            JTable table = tableEditor.getTable();
            int[] selectedRows = table.getSelectedRows();
            int[] selectedColumns = table.getSelectedColumns();

            if (selectedRows == null || selectedRows.length == 0 ||
                    selectedColumns == null || selectedColumns.length == 0) {
                return;
            }

            int focusedRow = table.getSelectedRow();
            int focusedColumn = table.getSelectedColumn();

            tableEditor.clearCells(
                    Arrays.stream(selectedRows).map(row -> table.convertRowIndexToModel(row)).boxed().collect(Collectors.toList()),
                    Arrays.stream(selectedColumns).map(col -> table.convertColumnIndexToModel(col)).boxed().collect(Collectors.toList())
            );

            selectCell(table, focusedRow, focusedColumn);
        } finally {
            tableEditor.applyTableChangeListener();
        }
    }

    private void selectCell(JTable table, int row, int column) {
        int actualRow = Math.min(row, table.getRowCount() - 1);
        int actualColumn = Math.min(column, table.getColumnCount() - 1);
        if (actualRow < 0 || actualColumn < 0) {
            return;
        }
        table.changeSelection(actualRow, actualColumn, false, false);
    }

    private final class OpenTextEditor implements LinkListener {
        @Override
        public void linkSelected(LinkLabel linkLabel, Object o) {
            FileEditorManager.getInstance(csvTableEditor.getProject()).openTextEditor(new OpenFileDescriptor(csvTableEditor.getProject(), csvTableEditor.getFile()), true);
        }
    }

    private final class OpenCsvPluginLink implements LinkListener {
        @Override
        public void linkSelected(LinkLabel linkLabel, Object o) {
            BrowserUtil.browse("https://github.com/SeeSharpSoft/intellij-csv-validator");
        }
    }
}
