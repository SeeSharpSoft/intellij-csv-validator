package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.google.common.primitives.Ints;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.ui.table.JBTable;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvFileEditorProvider;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.List;

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
    protected LinkListener openTextEditor = new OpenTextEditor();
    protected LinkListener openCsvPluginLink = new OpenCsvPluginLink();

    public CsvTableEditorActions(CsvTableEditor tableEditor) {
        super(tableEditor);
    }

    private class UndoAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.dataManagement.canGetLastState()) {
                csvTableEditor.updateTableData(csvTableEditor.dataManagement.getLastState());
            }
        }
    }

    private class RedoAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.dataManagement.canGetNextState()) {
                csvTableEditor.updateTableData(csvTableEditor.dataManagement.getNextState());
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
                DefaultTableModel tableModel = csvTableEditor.getTableModel();
                if (this.before != null) {
                    tableModel.insertRow(currentRow + (before && currentRow != -1 ? 0 : 1), new Object[tableModel.getColumnCount()]);
                } else {
                    tableModel.addRow(new Object[tableModel.getColumnCount()]);
                }

                csvTableEditor.syncTableModelWithUI();

                selectCell(table, currentRow, currentColumn);
            } finally {
                csvTableEditor.applyTableChangeListener();
            }
        }
    }

    private void selectCell(JTable table, int row, int column) {
        row = Math.min(row, table.getRowCount());
        column = Math.min(column, table.getColumnCount());
        table.setRowSelectionInterval(row, row);
        table.setColumnSelectionInterval(column, column);
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
                int currentColumn = table.getSelectedColumn();

                List<Integer> currentRows = Ints.asList(csvTableEditor.getTable().getSelectedRows());
                if (currentRows == null || currentRows.size() == 0) {
                    return;
                }

                DefaultTableModel tableModel = csvTableEditor.getTableModel();
                currentRows.sort(Collections.reverseOrder());
                for (int currentRow : currentRows) {
                    tableModel.removeRow(currentRow);
                }

                csvTableEditor.syncTableModelWithUI();

                currentRows.sort(Comparator.naturalOrder());
                selectCell(table, currentRows.get(0), currentColumn);
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
                DefaultTableModel tableModel = csvTableEditor.getTableModel();
                int columnCount = tableModel.getColumnCount();
                tableModel.addColumn(tableModel.getColumnName(columnCount));
                if (before != null && currentColumn != -1 && columnCount > 0 && currentColumn < columnCount - 1) {
                    table.moveColumn(tableModel.getColumnCount() - 1, currentColumn + (before ? 0 : 1));
                }

                csvTableEditor.syncTableModelWithUI();

                selectCell(table, currentRow, currentColumn);
            } finally {
                csvTableEditor.applyTableChangeListener();
            }
        }
    }

    private void removeColumn(DefaultTableModel tableModel, int column) {
        int prevColumnCount = tableModel.getColumnCount();

        Vector rows = tableModel.getDataVector();
        for (Object row : rows) {
            ((Vector) row).remove(column);
        }

        tableModel.setColumnCount(prevColumnCount - 1);
        tableModel.fireTableStructureChanged();
    }

    private final class DeleteColumnAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.hasErrors()) {
                return;
            }

            csvTableEditor.removeTableChangeListener();
            try {
                List<Integer> currentColumns = Ints.asList(csvTableEditor.getTable().getSelectedColumns());
                if (currentColumns == null || currentColumns.size() == 0) {
                    return;
                }
                JBTable table = csvTableEditor.getTable();
                TableColumnModel tableColumnModel = table.getColumnModel();

                List<Integer> tableModelIndices = new ArrayList<>();
                currentColumns.forEach(currentColumn -> tableModelIndices.add(table.convertColumnIndexToModel(currentColumn)));

                currentColumns.sort(Collections.reverseOrder());
                for (int currentColumn : currentColumns) {
                    tableColumnModel.removeColumn(tableColumnModel.getColumn(currentColumn));
                }

                DefaultTableModel tableModel = csvTableEditor.getTableModel();
                tableModelIndices.sort(Collections.reverseOrder());
                for (int currentColumn : tableModelIndices) {
                    removeColumn(tableModel, currentColumn);
                }

                csvTableEditor.syncTableModelWithUI();
            } finally {
                csvTableEditor.applyTableChangeListener();
            }
        }
    }

    private final class OpenTextEditor implements LinkListener {
        @Override
        public void linkSelected(LinkLabel linkLabel, Object o) {
            FileEditorManager.getInstance(csvTableEditor.project).openTextEditor(new OpenFileDescriptor(csvTableEditor.project, csvTableEditor.file), true);
            // this line is for legacy reasons (https://youtrack.jetbrains.com/issue/IDEA-199790)
            FileEditorManager.getInstance(csvTableEditor.project).setSelectedEditor(csvTableEditor.file, CsvFileEditorProvider.EDITOR_TYPE_ID);
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
