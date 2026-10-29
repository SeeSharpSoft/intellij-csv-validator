package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.util.Disposer;
import net.seesharpsoft.intellij.plugins.csv.actions.CsvTableEditorActions;

import java.awt.event.KeyEvent;
import javax.swing.*;
import java.util.Arrays;

public class CsvTableKeymapTest extends CsvTableEditorSwingTestBase {

    private Keymap originalKeymap;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        KeymapManagerEx manager = KeymapManagerEx.getInstanceEx();
        originalKeymap = manager.getActiveKeymap();
        manager.setActiveKeymap(originalKeymap.deriveKeymap("CSV shortcut test"));
        fileEditor.getTable().changeSelection(1, 1, false, false);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            KeymapManagerEx.getInstanceEx().setActiveKeymap(originalKeymap);
        } finally {
            super.tearDown();
        }
    }

    public void testRemovingColumnInsertionShortcutStopsInsertingColumns() {
        KeymapManagerEx.getInstanceEx().getActiveKeymap().removeAllActionShortcuts(
                CsvTableEditorActions.AddColumnAfter.class.getName());
        KeyEvent event = new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_PRESSED, 0,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);

        performShortcut(event);

        assertEquals("a removed shortcut must not insert a column",
                myInitialColumnCount, getTableModel().getColumnCount());
    }

    public void testReassignedShortcutWorksWithoutReopeningEditor() {
        Keymap keymap = KeymapManagerEx.getInstanceEx().getActiveKeymap();
        String actionId = CsvTableEditorActions.AddColumnAfter.class.getName();
        keymap.removeAllActionShortcuts(actionId);
        keymap.addShortcut(actionId, new KeyboardShortcut(KeyStroke.getKeyStroke("alt F7"), null));

        assertTrue(performShortcut(new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_PRESSED, 0,
                KeyEvent.ALT_DOWN_MASK, KeyEvent.VK_F7, KeyEvent.CHAR_UNDEFINED)));

        assertEquals(myInitialColumnCount + 1, getTableModel().getColumnCount());
        assertEquals("", getTableModel().getValue(1, 2));
    }

    public void testSwitchingKeymapsUpdatesAlreadyOpenEditor() {
        Keymap keymap = KeymapManagerEx.getInstanceEx().getActiveKeymap().deriveKeymap("Other CSV shortcuts");
        String actionId = CsvTableEditorActions.AddRowAfter.class.getName();
        keymap.removeAllActionShortcuts(actionId);
        keymap.addShortcut(actionId, new KeyboardShortcut(KeyStroke.getKeyStroke("alt F8"), null));
        KeymapManagerEx.getInstanceEx().setActiveKeymap(keymap);

        assertTrue(performShortcut(new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_PRESSED, 0,
                KeyEvent.ALT_DOWN_MASK, KeyEvent.VK_F8, KeyEvent.CHAR_UNDEFINED)));

        assertEquals(myInitialRowCount + 1, getTableModel().getRowCount());
        assertEquals("", getTableModel().getValue(2, 0));
    }

    public void testTwoStrokeShortcutInvokesTableCommand() {
        String actionId = CsvTableEditorActions.AddColumnAfter.class.getName();
        KeyboardShortcut shortcut = new KeyboardShortcut(KeyStroke.getKeyStroke("control K"), KeyStroke.getKeyStroke("C"));
        Keymap keymap = KeymapManagerEx.getInstanceEx().getActiveKeymap();
        keymap.removeAllActionShortcuts(actionId);
        keymap.addShortcut(actionId, shortcut);

        assertTrue(performShortcut(new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_PRESSED, 0,
                0, KeyEvent.VK_C, KeyEvent.CHAR_UNDEFINED), shortcut));

        assertEquals(myInitialColumnCount + 1, getTableModel().getColumnCount());
    }

    public void testRemovingDeletionShortcutsLeavesDocumentUnchanged() {
        Keymap keymap = KeymapManagerEx.getInstanceEx().getActiveKeymap();
        for (Class<?> action : new Class<?>[]{CsvTableEditorActions.ClearSelectedCells.class,
                CsvTableEditorActions.DeleteSelectedRows.class, CsvTableEditorActions.DeleteSelectedColumns.class}) {
            keymap.removeAllActionShortcuts(action.getName());
        }
        String original = fileEditor.getDocument().getText();

        for (int modifiers : new int[]{0, KeyEvent.CTRL_DOWN_MASK, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK}) {
            for (int key : new int[]{KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE}) {
                performShortcut(new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_PRESSED, 0, modifiers, key, KeyEvent.CHAR_UNDEFINED));
                performShortcut(new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, 0, modifiers, key, KeyEvent.CHAR_UNDEFINED));
            }
        }

        assertEquals(original, fileEditor.getDocument().getText());
    }

    public void testInsertionShortcutCommitsActiveCellBeforeAddingRow() {
        assertTrue(fileEditor.getTable().editCellAt(1, 1));
        JTextArea text = (JTextArea) ((JScrollPane) fileEditor.getTable().getEditorComponent()).getViewport().getView();
        text.setText("changed cell");

        assertTrue(performShortcut(new KeyEvent(text, KeyEvent.KEY_PRESSED, 0,
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED)));

        assertFalse(fileEditor.getTable().isEditing());
        assertEquals("changed cell", getTableModel().getValue(1, 1));
        assertEquals("", getTableModel().getValue(2, 1));
        assertEquals(2, fileEditor.getTable().getSelectedRow());
    }

    public void testDeleteAndBackspaceStillEditCellText() {
        assertTrue(fileEditor.getTable().editCellAt(1, 1));
        JTextArea text = (JTextArea) ((JScrollPane) fileEditor.getTable().getEditorComponent()).getViewport().getView();
        text.setText("abcd");
        text.setCaretPosition(2);
        for (int key : new int[]{KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE}) {
            KeyEvent event = new KeyEvent(text, KeyEvent.KEY_PRESSED, 0, 0, key, KeyEvent.CHAR_UNDEFINED);
            if (!performShortcut(event)) {
                text.getActionForKeyStroke(KeyStroke.getKeyStrokeForEvent(event)).actionPerformed(
                        new java.awt.event.ActionEvent(text, java.awt.event.ActionEvent.ACTION_PERFORMED, null));
            }
        }
        assertEquals("ad", text.getText());
        assertTrue(fileEditor.getTable().isEditing());
        assertEquals("this is column header 2", getTableModel().getValue(1, 1));
    }

    public void testReadOnlyTableRejectsNativeEditingActions() {
        fileEditor.setEditable(false);
        String original = fileEditor.getDocument().getText();

        for (String shortcut : new String[]{"control UP", "control DOWN", "control shift LEFT", "control shift RIGHT",
                "control DELETE", "control BACK_SPACE", "control shift DELETE", "control shift BACK_SPACE", "DELETE", "BACK_SPACE"}) {
            KeyStroke key = KeyStroke.getKeyStroke(shortcut);
            performShortcut(new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_PRESSED, 0,
                    key.getModifiers(), key.getKeyCode(), KeyEvent.CHAR_UNDEFINED));
        }

        assertEquals(original, fileEditor.getDocument().getText());
    }

    public void testAutoFitShortcutCanBeRemovedAndReassigned() {
        String actionId = CsvTableEditorActions.AdjustColumnWidths.class.getName();
        Keymap keymap = KeymapManagerEx.getInstanceEx().getActiveKeymap();
        keymap.removeAllActionShortcuts(actionId);
        fileEditor.getTableEditorState().setColumnWidths(new int[]{40, 40});
        fileEditor.updateEditorLayout();

        performShortcut(new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_RELEASED, 0,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_SPACE, KeyEvent.CHAR_UNDEFINED));
        assertEquals(40, fileEditor.getTable().getColumnModel().getColumn(0).getWidth());

        keymap.addShortcut(actionId, new KeyboardShortcut(KeyStroke.getKeyStroke("alt F9"), null));
        assertTrue(performShortcut(new KeyEvent(fileEditor.getTable(), KeyEvent.KEY_PRESSED, 0,
                KeyEvent.ALT_DOWN_MASK, KeyEvent.VK_F9, KeyEvent.CHAR_UNDEFINED)));
        assertTrue(fileEditor.getTable().getColumnModel().getColumn(0).getWidth() > 40);
    }

    public void testContextMenuUsesSameActionAndCurrentShortcut() {
        String actionId = CsvTableEditorActions.AddRowAfter.class.getName();
        AnAction action = ActionManager.getInstance().getAction(actionId);
        assertTrue(ActionUtil.getActions(fileEditor.getTable()).contains(action));
        assertSame(fileEditor, DataManager.getDataProvider(fileEditor.getComponent()).getData(PlatformDataKeys.FILE_EDITOR.getName()));
        Keymap keymap = KeymapManagerEx.getInstanceEx().getActiveKeymap();
        keymap.removeAllActionShortcuts(actionId);
        KeyboardShortcut shortcut = new KeyboardShortcut(KeyStroke.getKeyStroke("alt F8"), null);
        keymap.addShortcut(actionId, shortcut);
        assertEquals(Arrays.asList(shortcut), Arrays.asList(action.getShortcutSet().getShortcuts()));
        assertEquals("New Row After", action.getTemplateText());
        DataContext context = SimpleDataContext.getSimpleContext(PlatformDataKeys.FILE_EDITOR, fileEditor);
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.POPUP, context);

        action.update(event);
        assertTrue(event.getPresentation().isEnabled());
        action.actionPerformed(event);

        assertEquals(myInitialRowCount + 1, getTableModel().getRowCount());
    }

    public void testEditorDisposalUnregistersLocalTableActions() {
        assertFalse(ActionUtil.getActions(fileEditor.getTable()).isEmpty());

        Disposer.dispose(fileEditor);

        assertEmpty(ActionUtil.getActions(fileEditor.getTable()));
    }
}
