package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.ui.JBPopupMenu;

import javax.swing.table.JTableHeader;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class CsvTableEditorMouseListener extends CsvTableEditorUtilBase implements MouseListener {

    public CsvTableEditorMouseListener(CsvTableEditor csvTableEditorArg) {
        super(csvTableEditorArg);
    }

    @Override
    protected void onEditorUpdated() {

    }


    @Override
    public void mouseClicked(MouseEvent e) {

    }

    private void selectCurrentColumn(int currentColumn, boolean append) {
        if (!append) {
            csvTableEditor.getTable().clearSelection();
        }
        csvTableEditor.getTable().addColumnSelectionInterval(currentColumn, currentColumn);
        int currentRowCount = csvTableEditor.getTable().getRowCount();
        if (currentRowCount > 0) {
            csvTableEditor.getTable().addRowSelectionInterval(0, currentRowCount - 1);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int currentColumn = csvTableEditor.getTable().columnAtPoint(e.getPoint());
        JBPopupMenu menu;
        if (e.getSource() instanceof JTableHeader) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                selectCurrentColumn(currentColumn, (e.isControlDown() || e.isShiftDown()));
            }
            menu = csvTableEditor.getColumnPopupMenu();
        } else {
            menu = csvTableEditor.getRowPopupMenu();
        }
        if (e.isPopupTrigger() && !menu.isShowing()) {
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        csvTableEditor.syncTableModelWithUI();
        JBPopupMenu menu;
        if (e.getSource() instanceof JTableHeader) {
            menu = csvTableEditor.getColumnPopupMenu();
        } else {
            menu = csvTableEditor.getRowPopupMenu();
        }
        if (e.isPopupTrigger() && !menu.isShowing()) {
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
