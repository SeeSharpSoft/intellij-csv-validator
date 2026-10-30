package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.keymap.impl.ActionProcessor;
import com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher;
import com.intellij.openapi.util.Disposer;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.util.UpdateScheduler;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public abstract class CsvTableEditorSwingTestBase extends CsvBasePlatformTestCase {

    protected CsvTableEditorSwing fileEditor;
    UpdateScheduler updateScheduler;

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    protected String getTestFile() {
        return "TableEditorFile.csv";
    }

    protected int myInitialRowCount;
    protected int myInitialColumnCount;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activateDefaultKeymap();
        initializeEditorSettings(CsvEditorSettings.getInstance());
        myFixture.configureByFiles(getTestFile());

        fileEditor = createEditor();
        fileEditor.selectNotify();

        myInitialRowCount = getTableModel().getRowCount();
        myInitialColumnCount = getTableModel().getColumnCount();
    }

    @Override
    protected void tearDown() throws Exception {
        fileEditor.dispose();

        super.tearDown();
    }

    // Tests send plugin.xml's $default shortcuts; OS keymaps convert them (macOS maps Ctrl to Cmd).
    private void activateDefaultKeymap() {
        KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
        Keymap platformKeymap = keymapManager.getActiveKeymap();
        keymapManager.setActiveKeymap(keymapManager.getKeymap(KeymapManager.DEFAULT_IDEA_KEYMAP));
        Disposer.register(getTestRootDisposable(), () -> keymapManager.setActiveKeymap(platformKeymap));
    }

    protected CsvTableModel getTableModel() {
        return fileEditor.getTableModel();
    }

    protected boolean performShortcut(KeyEvent event) {
        return performShortcut(event, new KeyboardShortcut(KeyStroke.getKeyStrokeForEvent(event), null));
    }

    protected boolean performShortcut(KeyEvent event, KeyboardShortcut shortcut) {
        // Supply focus explicitly because these editors have no native window in fixture tests.
        IdeKeyEventDispatcher dispatcher = new IdeKeyEventDispatcher(null);
        Component source = event.getComponent();
        DataContext editorContext = DataManager.getDataProvider(fileEditor.getComponent())::getData;
        dispatcher.getContext().setDataContext(SimpleDataContext.builder()
                .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, source)
                .add(PlatformDataKeys.FILE_EDITOR, PlatformDataKeys.FILE_EDITOR.getData(editorContext))
                .add(CommonDataKeys.PROJECT, CommonDataKeys.PROJECT.getData(editorContext))
                .add(CommonDataKeys.VIRTUAL_FILE, CommonDataKeys.VIRTUAL_FILE.getData(editorContext))
                .build());
        dispatcher.getContext().setFocusOwner(source);
        dispatcher.getContext().setInputEvent(event);
        dispatcher.getContext().setProject(getProject());
        dispatcher.updateCurrentContext(source, shortcut);
        boolean handled = dispatcher.getContext().getShortcut() != null &&
                dispatcher.processAction(event, new ActionProcessor() { });
        if (!handled) {
            for (KeyListener listener : source.getKeyListeners()) {
                if (event.getID() == KeyEvent.KEY_PRESSED) listener.keyPressed(event);
                else if (event.getID() == KeyEvent.KEY_RELEASED) listener.keyReleased(event);
            }
        }
        return handled || event.isConsumed();
    }

    protected CsvTableEditorSwing createEditor() {
        if (updateScheduler == null) {
            return new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile());
        }
        return new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile()) {
            @Override
            protected CsvTableModelSwing createTableModel() {
                return createTableModelForTest(this);
            }
        };
    }

    protected CsvTableModelSwing createTableModelForTest(CsvTableEditorSwing editor) {
        return updateScheduler == null ? new CsvTableModelSwing(editor) : new CsvTableModelSwing(editor, updateScheduler);
    }

    protected void initializeEditorSettings(CsvEditorSettings instance) {
        instance.loadState(new CsvEditorSettings.OptionSet());
    }
}
