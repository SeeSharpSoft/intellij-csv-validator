package net.seesharpsoft.intellij.plugins.csv.editor;

import com.google.common.primitives.Ints;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.ui.table.JBTable;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class CsvTableEditorActions extends CsvTableEditorUtilBase {

    protected ActionListener readOnly = new ReadOnlyAction();
    protected ActionListener readWrite = new ReadWriteAction();

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

    private Object[] generateColumnIdentifiers(TableModel tableModel) {
        int columnCount = tableModel.getColumnCount();
        Object[] identifiers = new Object[columnCount];
        for (int i = 0; i < columnCount; ++i) {
            identifiers[i] = tableModel.getColumnName(i);
        }
        return identifiers;
    }

    private class ReadOnlyAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            csvTableEditor.setEditable(false);
        }
    }

    private class ReadWriteAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            csvTableEditor.setEditable(true);
        }
    }

    @Override
    protected void onEditorUpdated() {
        csvTableEditor.removeTableChangeListener();
        try {
            DefaultTableModel tableModel = csvTableEditor.getTableModel();
            tableModel.setColumnIdentifiers(generateColumnIdentifiers(tableModel));
        } finally {
            csvTableEditor.applyTableChangeListener();
        }
    }

    private class UndoAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.stateManagement.canGetLastState()) {
                csvTableEditor.updateEditorTable(csvTableEditor.stateManagement.getLastState());
            }
        }
    }

    private class RedoAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.stateManagement.canGetNextState()) {
                csvTableEditor.updateEditorTable(csvTableEditor.stateManagement.getNextState());
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

            int currentRow = csvTableEditor.getTable().getSelectedRow();
            DefaultTableModel tableModel = csvTableEditor.getTableModel();
            if (this.before != null) {
                tableModel.insertRow(currentRow + (before && currentRow != -1 ? 0 : 1), new Object[tableModel.getColumnCount()]);
            } else {
                tableModel.addRow(new Object[tableModel.getColumnCount()]);
            }
        }
    }

    private final class DeleteRowAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (csvTableEditor.hasErrors()) {
                return;
            }

            csvTableEditor.removeTableChangeListener();
            try {
                List<Integer> currentRows = Ints.asList(csvTableEditor.getTable().getSelectedRows());
                if (currentRows == null || currentRows.size() == 0) {
                    return;
                }

                DefaultTableModel tableModel = csvTableEditor.getTableModel();
                currentRows.sort(Collections.reverseOrder());
                for (int currentRow : currentRows) {
                    tableModel.removeRow(currentRow);
                }

                csvTableEditor.syncTableModelWithUI(true);
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
                int currentColumn = csvTableEditor.getTable().getSelectedColumn();
                JBTable table = csvTableEditor.getTable();
                DefaultTableModel tableModel = csvTableEditor.getTableModel();
                int columnCount = tableModel.getColumnCount();
                tableModel.addColumn(tableModel.getColumnName(columnCount));
                if (before != null && currentColumn != -1 && columnCount > 0 && currentColumn < columnCount - 1) {
                    table.moveColumn(tableModel.getColumnCount() - 1, currentColumn + (before ? 0 : 1));
                }

                csvTableEditor.syncTableModelWithUI(true);
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

                csvTableEditor.syncTableModelWithUI(true);
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
