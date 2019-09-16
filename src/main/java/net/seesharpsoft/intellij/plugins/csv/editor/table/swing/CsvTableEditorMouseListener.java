package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;

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
            ActionManager actionManager = ActionManager.getInstance();
            ActionGroup rowContextMenu = (ActionGroup) actionManager.getAction(ROW_CONTEXT_MENU_ID);
            ActionPopupMenu popupMenu = actionManager.createActionPopupMenu(ROW_CONTEXT_MENU_ID, rowContextMenu);
            // popupMenu.setTargetComponent(csvTableEditor.getComponent());
            rowPopupMenu = popupMenu.getComponent();
        }
        return rowPopupMenu;
    }

    protected JPopupMenu getColumnPopupMenu() {
        if (columnPopupMenu == null) {
            ActionManager actionManager = ActionManager.getInstance();
            ActionGroup columnContextMenu = (ActionGroup) actionManager.getAction(COLUMN_CONTEXT_MENU_ID);
            ActionPopupMenu popupMenu = actionManager.createActionPopupMenu(COLUMN_CONTEXT_MENU_ID, columnContextMenu);
            // popupMenu.setTargetComponent(csvTableEditor.getComponent());
            columnPopupMenu = popupMenu.getComponent();
        }
        return columnPopupMenu;
    }

    protected void showPopupMenu(JPopupMenu popupMenu, Component component, int x, int y) {
        if (!popupMenu.isShowing()) {
            popupMenu.show(component, x, y);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int currentColumn = csvTableEditor.getTable().columnAtPoint(e.getPoint());
        if (currentColumn == -1) {
            return;
        }
        JPopupMenu menu;
        if (e.getSource() instanceof JTableHeader) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                csvTableEditor.selectColumn(currentColumn, (e.isControlDown() || e.isShiftDown()));
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
