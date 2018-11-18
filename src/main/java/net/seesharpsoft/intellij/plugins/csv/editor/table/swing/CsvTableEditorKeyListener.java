package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class CsvTableEditorKeyListener extends CsvTableEditorUtilBase implements KeyListener {

    private final ActionListener startEditing = new StartCellEditingActionListener();
    private final ActionListener stopEditing = new StopCellEditingActionListener();

    public CsvTableEditorKeyListener(CsvTableEditorSwing csvTableEditorArg) {
        super(csvTableEditorArg);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // keyTyped
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // keyPressed
    }

    @Override
    public void keyReleased(KeyEvent e) {
//        if (csvTableEditor.getTable().isEditing()) {
//            return;
//        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                if (e.isControlDown()) {
                    csvTableEditor.tableEditorActions.addColumnBefore.actionPerformed(null);
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (e.isControlDown()) {
                    csvTableEditor.tableEditorActions.addColumnAfter.actionPerformed(null);
                }
                break;
            case KeyEvent.VK_UP:
                if (e.isControlDown()) {
                    csvTableEditor.tableEditorActions.addRowBefore.actionPerformed(null);
                }
                break;
            case KeyEvent.VK_DOWN:
                if (e.isControlDown()) {
                    csvTableEditor.tableEditorActions.addRowAfter.actionPerformed(null);
                }
                break;
            case KeyEvent.VK_ENTER:
                if (e.isControlDown()) {
                    stopEditing.actionPerformed(null);
                } else {
                    startEditing.actionPerformed(null);
                }
                break;
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_BACK_SPACE:
                if (e.isControlDown() && e.isShiftDown()) {
                    csvTableEditor.tableEditorActions.deleteColumn.actionPerformed(null);
                } else if (e.isControlDown()) {
                    csvTableEditor.tableEditorActions.deleteRow.actionPerformed(null);
                }
                break;
            default:
                break;
        }
    }

    private class StartCellEditingActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JTable table = CsvTableEditorKeyListener.this.csvTableEditor.getTable();
            if (table.getSelectedRow() != -1 && table.getSelectedRow() != -1) {
                table.editCellAt(table.getSelectedRow(), table.getSelectedColumn());
            }
        }
    }

    private class StopCellEditingActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            CellEditor editor = CsvTableEditorKeyListener.this.csvTableEditor.getTable().getCellEditor();
            if (editor != null) {
                CsvTableEditorKeyListener.this.csvTableEditor.getTable().getCellEditor().stopCellEditing();
            }
        }
    }
}
