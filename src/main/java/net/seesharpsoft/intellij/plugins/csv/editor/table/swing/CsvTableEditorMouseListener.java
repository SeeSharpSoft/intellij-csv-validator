package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class CsvTableEditorMouseListener extends CsvTableEditorUtilBase implements MouseListener {

    public static final String ROW_CONTEXT_MENU_ID = "CsvTableEditorRowContextMenu";
    public static final String COLUMN_CONTEXT_MENU_ID = "CsvTableEditorColumnContextMenu";

    private JPopupMenu rowPopupMenu;
    private JPopupMenu columnPopupMenu;

    public CsvTableEditorMouseListener(CsvTableEditorSwing csvTableEditorArg) {
        super(csvTableEditorArg);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // mouseClicked
    }

    protected JPopupMenu getRowPopupMenu() {
        if (rowPopupMenu == null) {
            rowPopupMenu = createPopupMenu(ROW_CONTEXT_MENU_ID);
        }
        return rowPopupMenu;
    }

    protected JPopupMenu getColumnPopupMenu() {
        if (columnPopupMenu == null) {
            columnPopupMenu = createPopupMenu(COLUMN_CONTEXT_MENU_ID);
        }
        return columnPopupMenu;
    }

    private static JPopupMenu createPopupMenu(String id) {
        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup contextMenu = (ActionGroup) actionManager.getAction(id);
        return actionManager.createActionPopupMenu(id, contextMenu).getComponent();
    }

    protected void showPopupMenu(JPopupMenu popupMenu, Component component, int x, int y) {
        if (!popupMenu.isShowing()) {
            popupMenu.show(component, x, y);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int currentColumn = e.getSource() instanceof JTableHeader header
                ? header.columnAtPoint(e.getPoint()) : csvTableEditor.getTable().columnAtPoint(e.getPoint());
        if (currentColumn == -1) {
            return;
        }
        JPopupMenu menu;
        if (e.getSource() instanceof JTableHeader header) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                boolean append = e.isControlDown() || e.isShiftDown() || e.isMetaDown() ||
                        (header.getDraggedColumn() != null && csvTableEditor.getTable().isColumnSelected(currentColumn));
                csvTableEditor.selectColumn(currentColumn, append);
            }
            menu = this.getColumnPopupMenu();
        } else {
            menu = this.getRowPopupMenu();
        }
        if (e.isPopupTrigger()) {
            showPopupMenu(menu, e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        JPopupMenu menu;
        if (e.getSource() instanceof JTableHeader) {
            menu = this.getColumnPopupMenu();
        } else {
            menu = this.getRowPopupMenu();
        }
        if (e.isPopupTrigger()) {
            showPopupMenu(menu, e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // mouseEntered
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // mouseExited
    }
}
