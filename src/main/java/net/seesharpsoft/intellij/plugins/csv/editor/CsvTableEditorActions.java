package net.seesharpsoft.intellij.plugins.csv.editor;

import com.google.common.primitives.Ints;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.ui.table.JBTable;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

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

    public CsvTableEditorActions(CsvTableEditor tableEditor) {
        super(tableEditor);
    }

    @Override
    protected void onEditorUpdated() {
        // nothing to do
    }

    private class UndoAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            csvTableEditor.updateEditorTable(csvTableEditor.stateManagement.getLastState());
        }
    }

    private class RedoAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            csvTableEditor.updateEditorTable(csvTableEditor.stateManagement.getNextState());
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
            if (currentRow == -1) {
                currentRow = 0;
            }
            DefaultTableModel tableModel = csvTableEditor.getTableModel();
            if (this.before != null) {
                tableModel.insertRow(currentRow + (before ? 0 : 1), new Object[tableModel.getColumnCount()]);
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
                if (currentColumn == -1) {
                    currentColumn = 0;
                }
                JBTable table = csvTableEditor.getTable();
                DefaultTableModel tableModel = csvTableEditor.getTableModel();
                tableModel.addColumn("");
                if (before != null) {
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
            FileEditorManager.getInstance(csvTableEditor.project).navigateToTextEditor(new OpenFileDescriptor(csvTableEditor.project, csvTableEditor.file), true);
        }
    }
}
