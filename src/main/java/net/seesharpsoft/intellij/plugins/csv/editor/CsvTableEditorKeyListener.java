package net.seesharpsoft.intellij.plugins.csv.editor;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class CsvTableEditorKeyListener extends CsvTableEditorUtilBase implements KeyListener {

    public CsvTableEditorKeyListener(CsvTableEditor csvTableEditorArg) {
        super(csvTableEditorArg);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (csvTableEditor.getTable().isEditing()) {
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER:
                if (e.isAltDown()) {
                    if (e.isShiftDown()) {
                        csvTableEditor.tableEditorActions.addColumnBefore.actionPerformed(null);
                    } else {
                        csvTableEditor.tableEditorActions.addColumnAfter.actionPerformed(null);
                    }
                } else if (e.isControlDown()) {
                    if (e.isShiftDown()) {
                        csvTableEditor.tableEditorActions.addRowBefore.actionPerformed(null);
                    } else {
                        csvTableEditor.tableEditorActions.addRowAfter.actionPerformed(null);
                    }
                }
                break;
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_BACK_SPACE:
                if (e.isAltDown()) {
                    csvTableEditor.tableEditorActions.deleteColumn.actionPerformed(null);
                } else if (e.isControlDown()) {
                    csvTableEditor.tableEditorActions.deleteRow.actionPerformed(null);
                }
                break;
        }
    }

    @Override
    protected void onEditorUpdated() {

    }
}
