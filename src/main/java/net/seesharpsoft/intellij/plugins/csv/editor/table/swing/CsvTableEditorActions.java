package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.ui.table.JBTable;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvFileEditorProvider;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;

public class CsvTableEditorActions extends CsvTableEditorUtilBase {

    protected ActionListener undo = new UndoAction();
    protected ActionListener redo = new RedoAction();
    protected ActionListener addRow = new AddRowAction(null);
    protected ActionListener addRowBefore = new AddRowAction(true);
    protected ActionListener addRowAfter = new AddRowAction(false);
    protected ActionListener addColumn = new AddColumnAction(null);
    protected ActionListener addColumnBefore = new AddColumnAction(true);
    protected ActionListener addColumnAfter = new AddColumnAction(false);
    protected ActionListener deleteRow = new DeleteRowAction();
    protected ActionListener deleteColumn = new DeleteColumnAction();
    protected ActionListener clearCells = new ClearCellsAction();
    protected ActionListener adjustColumnWidthAction = new AutoColumnWidthAction();
    protected ActionListener resetColumnWidthAction = new ResetColumnWidthAction();

    protected LinkListener adjustColumnWidthLink = ((linkLabel, o) -> adjustColumnWidthAction.actionPerformed(null));
    protected LinkListener openTextEditor = new OpenTextEditor();
    protected LinkListener openCsvPluginLink = new OpenCsvPluginLink();

    public CsvTableEditorActions(CsvTableEditorSwing tableEditor) {
        super(tableEditor);
    }

    private class UndoAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.getDataHandler().canGetLastState()) {
                csvTableEditor.updateTableComponentData(csvTableEditor.getDataHandler().getLastState());
            }
        }
    }

    private class RedoAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.getDataHandler().canGetNextState()) {
                csvTableEditor.updateTableComponentData(csvTableEditor.getDataHandler().getNextState());
            }
        }
    }

    private final class AddRowAction implements ActionListener {
        private final Boolean before;

        private AddRowAction(Boolean beforeCurrent) {
            this.before = beforeCurrent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.hasErrors()) {
                return;
            }

            csvTableEditor.removeTableChangeListener();
            try {
                JTable table = csvTableEditor.getTable();
                int currentColumn = table.getSelectedColumn();
                int currentRow = table.getSelectedRow();

                csvTableEditor.addRow(this.before == null ? -1 : currentRow, Boolean.TRUE.equals(this.before));

                selectCell(csvTableEditor.getTable(), Boolean.TRUE.equals(this.before) ? currentRow + 1 : currentRow, currentColumn);
            } finally {
                csvTableEditor.applyTableChangeListener();
            }
        }
    }

    private void selectCell(JTable table, int row, int column) {
        int actualRow = Math.min(row, table.getRowCount() - 1);
        int actualColumn = Math.min(column, table.getColumnCount() - 1);
        if (actualRow < 0 || actualColumn < 0) {
            return;
        }
        table.setRowSelectionInterval(actualRow, actualRow);
        table.setColumnSelectionInterval(actualColumn, actualColumn);
    }

    private final class DeleteRowAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.hasErrors()) {
                return;
            }

            csvTableEditor.removeTableChangeListener();
            try {
                JTable table = csvTableEditor.getTable();
                int[] currentRows = csvTableEditor.getTable().getSelectedRows();
                if (currentRows == null || currentRows.length == 0) {
                    return;
                }
                int currentColumn = table.getSelectedColumn();

                csvTableEditor.removeRows(currentRows);

                selectCell(table, currentRows[0], currentColumn);
            } finally {
                csvTableEditor.applyTableChangeListener();
            }
        }
    }

    private final class AddColumnAction implements ActionListener {
        private final Boolean before;

        private AddColumnAction(Boolean beforeCurrent) {
            this.before = beforeCurrent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.hasErrors()) {
                return;
            }

            csvTableEditor.removeTableChangeListener();
            try {
                JBTable table = csvTableEditor.getTable();
                int currentColumn = table.getSelectedColumn();
                int currentRow = table.getSelectedRow();

                csvTableEditor.addColumn(currentColumn, Boolean.TRUE.equals(before));

                selectCell(table, currentRow, Boolean.TRUE.equals(before) ? currentColumn + 1 : currentColumn);
            } finally {
                csvTableEditor.applyTableChangeListener();
            }
        }
    }

    private final class DeleteColumnAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.hasErrors()) {
                return;
            }

            csvTableEditor.removeTableChangeListener();
            try {
                JBTable table = csvTableEditor.getTable();
                int[] selectedColumns = table.getSelectedColumns();
                if (selectedColumns == null || selectedColumns.length == 0) {
                    return;
                }
                int focusedRow = table.getSelectedRow();

                csvTableEditor.removeColumns(selectedColumns);

                selectCell(table, focusedRow, selectedColumns[0]);
            } finally {
                csvTableEditor.applyTableChangeListener();
            }
        }
    }

    private final class ClearCellsAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.hasErrors()) {
                return;
            }

            csvTableEditor.removeTableChangeListener();
            try {
                JBTable table = csvTableEditor.getTable();
                int[] selectedRows = table.getSelectedRows();
                if (selectedRows == null || selectedRows.length == 0) {
                    return;
                }
                int[] selectedColumns = table.getSelectedColumns();
                if (selectedColumns == null || selectedColumns.length == 0) {
                    return;
                }
                int focusedRow = table.getSelectedRow();
                int focusedColumn = table.getSelectedColumn();

                DefaultTableModel tableModel = csvTableEditor.getTableModel();

                for (int row : selectedRows) {
                    for (int column : selectedColumns) {
                        tableModel.setValueAt("", row, column);
                    }
                }

                csvTableEditor.syncTableModelWithUI();

                selectCell(table, focusedRow, focusedColumn);
            } finally {
                csvTableEditor.applyTableChangeListener();
            }
        }
    }

    private final class AutoColumnWidthAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            csvTableEditor.adjustAllColumnWidths();
        }
    }

    private final class ResetColumnWidthAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            csvTableEditor.resetAllColumnWidths();
        }
    }

    private final class OpenTextEditor implements LinkListener {
        @Override
        public void linkSelected(LinkLabel linkLabel, Object o) {
            FileEditorManager.getInstance(csvTableEditor.getProject()).openTextEditor(new OpenFileDescriptor(csvTableEditor.getProject(), csvTableEditor.getFile()), true);
            // this line is for legacy reasons (https://youtrack.jetbrains.com/issue/IDEA-199790)
            FileEditorManager.getInstance(csvTableEditor.getProject()).setSelectedEditor(csvTableEditor.getFile(), CsvFileEditorProvider.EDITOR_TYPE_ID);
        }
    }

    private final class OpenCsvPluginLink implements LinkListener {
        @Override
        public void linkSelected(LinkLabel linkLabel, Object o) {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(URI.create("https://github.com/SeeSharpSoft/intellij-csv-validator"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
