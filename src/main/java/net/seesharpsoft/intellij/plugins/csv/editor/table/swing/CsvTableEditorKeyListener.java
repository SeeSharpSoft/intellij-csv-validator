package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class CsvTableEditorKeyListener extends CsvTableEditorUtilBase implements KeyListener {

    public CsvTableEditorKeyListener(CsvTableEditorSwing csvTableEditorArg) {
        super(csvTableEditorArg);
    }

    private void startEditing() {
        JTable table = csvTableEditor.getTable();
        if (table.getSelectedRow() != -1 && table.getSelectedRow() != -1) {
            table.editCellAt(table.getSelectedRow(), table.getSelectedColumn());
        }
    }

    private void stopEditing() {
        CellEditor editor = csvTableEditor.getTable().getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
    }

    private void selectNextColumn(boolean startEdit) {
        selectOffsetColumn(1, startEdit);
    }

    private void selectPrevColumn(boolean startEdit) {
        selectOffsetColumn(-1, startEdit);
    }

    private void selectOffsetColumn(int offset, boolean startEdit) {
        JTable tblEditor = csvTableEditor.getTable();
        int row = tblEditor.getSelectedRow();
        int col = tblEditor.getSelectedColumn() + offset;
        if (col >= tblEditor.getColumnCount()) {
            col = 0;
            if (offset < 0) --row;
            else ++row;
        }
        if (row > 0 && row < tblEditor.getRowCount()) {
            tblEditor.changeSelection(row, col, false, false);
            if (startEdit) tblEditor.editCellAt(row, col);
        }
    }

    private void selectNextRow(boolean startEdit) {
        selectOffsetRow(1, startEdit);
    }

    private void selectPrevRow(boolean startEdit) {
        selectOffsetRow(-1, startEdit);
    }

    private void selectOffsetRow(int offset, boolean startEdit) {
        JTable tblEditor = csvTableEditor.getTable();
        int row = tblEditor.getSelectedRow() + offset;
        if (row < tblEditor.getRowCount()) {
            tblEditor.changeSelection(row, tblEditor.getSelectedColumn(), false, false);
            if (startEdit) tblEditor.editCellAt(row, tblEditor.getSelectedColumn());
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                if (e.isControlDown() && e.isShiftDown()) {
                    stopEditing();
                    csvTableEditor.tableEditorActions.addColumnBefore.actionPerformed(null);
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (e.isControlDown() && e.isShiftDown()) {
                    stopEditing();
                    csvTableEditor.tableEditorActions.addColumnAfter.actionPerformed(null);
                }
                break;
            case KeyEvent.VK_UP:
                if (e.isControlDown() && !e.isShiftDown()) {
                    stopEditing();
                    csvTableEditor.tableEditorActions.addRowBefore.actionPerformed(null);
                }
                break;
            case KeyEvent.VK_DOWN:
                if (e.isControlDown() && !e.isShiftDown()) {
                    stopEditing();
                    csvTableEditor.tableEditorActions.addRowAfter.actionPerformed(null);
                }
                break;
            case KeyEvent.VK_ENTER:
                if (csvTableEditor.isInCellEditMode()) {
                    if (!e.isControlDown() || e.isShiftDown()) {
                        stopEditing();
                    }
                    if (e.isControlDown() && e.isShiftDown()) {
                        selectNextRow(true);
                    }
                    if (!e.isControlDown() && e.isShiftDown()) {
                        startEditing();
                    }
                    if (!e.isControlDown() && !e.isShiftDown()) {
                        selectNextColumn(true);
                    }
                } else {
                    if (e.isControlDown() && !e.isShiftDown()) {
                        // do nothing?!
                    }
                    if (e.isControlDown() && e.isShiftDown()) {
                        selectNextRow(false);
                    }
                    if (!e.isControlDown() && e.isShiftDown()) {
                        // handled by default
                    }
                    if (!e.isControlDown() && !e.isShiftDown()) {
                        startEditing();
                    }
                }
                break;
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_BACK_SPACE:
                if (e.isControlDown() && e.isShiftDown()) {
                    stopEditing();
                    csvTableEditor.tableEditorActions.deleteColumn.actionPerformed(null);
                } else if (e.isControlDown()) {
                    stopEditing();
                    csvTableEditor.tableEditorActions.deleteRow.actionPerformed(null);
                } else if (!csvTableEditor.isInCellEditMode()) {
                    csvTableEditor.tableEditorActions.clearCells.actionPerformed(null);
                }
                break;
            case KeyEvent.VK_SPACE:
                if (e.isShiftDown() && e.isControlDown()) {
                    csvTableEditor.adjustAllColumnWidths();
                    csvTableEditor.updateEditorLayout();
                }
                break;
            default:
                break;
        }
    }
}
