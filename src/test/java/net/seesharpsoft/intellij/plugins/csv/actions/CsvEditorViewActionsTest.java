package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.ide.DataManager;
import com.intellij.ide.impl.HeadlessDataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.FileEditorManagerTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvTextEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.util.Arrays;

public class CsvEditorViewActionsTest extends FileEditorManagerTestCase {

    private static final String SHOW_TEXT_EDITOR = "net.seesharpsoft.intellij.plugins.csv.actions.CsvEditorViewActions$ShowTextEditor";
    private static final String SHOW_TABLE_EDITOR = "net.seesharpsoft.intellij.plugins.csv.actions.CsvEditorViewActions$ShowTableEditor";

    private VirtualFile csvFile;

    @Override
    public void setUp() {
        System.setProperty("hidpi", "false");
        super.setUp();
        // Resolve action data from the editor Swing components only, like the IDE does. The fixture's
        // test data provider would otherwise report the current window's editor for every split window.
        HeadlessDataManager.fallbackToProductionDataManager(getTestRootDisposable());
        ((HeadlessDataManager) DataManager.getInstance()).setTestDataProvider(null, getTestRootDisposable());
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        csvFile = myFixture.addFileToProject("sample.csv", "name,value\nalpha,1\n").getVirtualFile();
    }

    @Override
    public void tearDown() throws Exception {
        try {
            CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        } finally {
            super.tearDown();
        }
    }

    public void testBothCsvEditorsShowViewSwitchInTheirEditorTab() {
        EditorComposite composite = openCsvFile();
        ActionGroup viewActions = CsvEditorViewActions.getEditorViewActionGroup();

        assertNotNull(viewActions);
        assertInstanceOf(composite.getAllEditors().get(0), CsvTextEditor.class);
        assertInstanceOf(composite.getAllEditors().get(0), PsiAwareTextEditorImpl.class);
        assertInstanceOf(composite.getAllEditors().get(1), CsvTableEditor.class);
        for (FileEditor editor : composite.getAllEditors()) {
            assertSame(viewActions, editor.getTabActions());
        }
        PlatformTestUtil.waitWithEventsDispatching("view switch must be shown in the editor tab",
                () -> viewActions == currentWindow().getTabbedPane().getTabs().getSelectedInfo().getTabPaneActions(), 10);
    }

    public void testTextEditorButtonIsSelectedWhileTextEditorIsShown() {
        openCsvFile();

        assertSwitchState(currentWindow(), true, false);
    }

    public void testTableEditorButtonShowsTableEditorInTheSameEditorTab() {
        EditorComposite composite = openCsvFile();

        performFromEditorTab(SHOW_TABLE_EDITOR, currentWindow());

        assertInstanceOf(composite.getSelectedEditor(), CsvTableEditor.class);
        assertSame(composite.getSelectedEditor(), manager.getSelectedEditor(csvFile));
        assertSwitchState(currentWindow(), false, true);
    }

    public void testTextEditorButtonShowsTextEditorAgain() {
        EditorComposite composite = openCsvFile();
        performFromEditorTab(SHOW_TABLE_EDITOR, currentWindow());

        performFromEditorTab(SHOW_TEXT_EDITOR, currentWindow());

        assertInstanceOf(composite.getSelectedEditor(), CsvTextEditor.class);
        assertSwitchState(currentWindow(), true, false);
    }

    public void testShownEditorButtonKeepsItsEditorShown() {
        EditorComposite composite = openCsvFile();

        performFromEditorTab(SHOW_TEXT_EDITOR, currentWindow());

        assertInstanceOf(composite.getSelectedEditor(), CsvTextEditor.class);
        assertSwitchState(currentWindow(), true, false);
    }

    public void testViewSwitchIsHiddenWhenOnlyTheTextEditorIsEnabled() {
        CsvEditorSettings.getInstance().setEditorPrio(CsvEditorSettings.EditorPrio.TEXT_ONLY);
        EditorComposite composite = openCsvFile();

        assertSize(1, composite.getAllEditors());
        assertFalse(updateFromEditorTab(SHOW_TEXT_EDITOR, currentWindow()).getPresentation().isVisible());
        assertFalse(updateFromEditorTab(SHOW_TABLE_EDITOR, currentWindow()).getPresentation().isVisible());
    }

    public void testViewSwitchOnlyChangesTheSplitWindowWhoseTabIsUsed() {
        EditorComposite firstComposite = openCsvFile();
        EditorWindow firstWindow = currentWindow();
        manager.createSplitter(SwingConstants.VERTICAL, firstWindow);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        EditorWindow secondWindow = Arrays.stream(manager.getWindows())
                .filter(window -> window != firstWindow)
                .findFirst()
                .orElseThrow();
        EditorComposite secondComposite = secondWindow.getComposite(csvFile);
        firstWindow.setAsCurrentWindow(false);

        performFromEditorTab(SHOW_TABLE_EDITOR, secondWindow);

        assertInstanceOf(firstComposite.getSelectedEditor(), CsvTextEditor.class);
        assertInstanceOf(secondComposite.getSelectedEditor(), CsvTableEditor.class);
        assertSame(secondWindow, currentWindow());
        assertSwitchState(firstWindow, true, false);
        assertSwitchState(secondWindow, false, true);
    }

    public void testViewSwitchWorksFromInsideTheShownEditor() {
        EditorComposite composite = openCsvFile();
        JComponent textEditorComponent = composite.getSelectedEditor().getPreferredFocusedComponent();
        assertNotNull(textEditorComponent);

        perform(SHOW_TABLE_EDITOR, textEditorComponent, ActionPlaces.KEYBOARD_SHORTCUT, ActionUiKind.NONE);

        assertInstanceOf(composite.getSelectedEditor(), CsvTableEditor.class);
    }

    private EditorComposite openCsvFile() {
        manager.openFile(csvFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        EditorComposite composite = currentWindow().getComposite(csvFile);
        assertNotNull(composite);
        return composite;
    }

    private EditorWindow currentWindow() {
        EditorWindow window = manager.getCurrentWindow();
        assertNotNull(window);
        return window;
    }

    private void assertSwitchState(EditorWindow window, boolean textEditorShown, boolean tableEditorShown) {
        assertSwitchButton(updateFromEditorTab(SHOW_TEXT_EDITOR, window), textEditorShown);
        assertSwitchButton(updateFromEditorTab(SHOW_TABLE_EDITOR, window), tableEditorShown);
    }

    private static void assertSwitchButton(AnActionEvent event, boolean selected) {
        Presentation presentation = event.getPresentation();
        assertTrue(presentation.isEnabledAndVisible());
        assertEquals(selected, Toggleable.isSelected(presentation));
    }

    private AnActionEvent updateFromEditorTab(String actionId, EditorWindow window) {
        AnAction action = ActionManager.getInstance().getAction(actionId);
        AnActionEvent event = createEvent(action, window.getTabbedPane().getComponent(), ActionPlaces.EDITOR_TAB, ActionUiKind.TOOLBAR);
        ActionUtil.updateAction(action, event);
        return event;
    }

    private void performFromEditorTab(String actionId, EditorWindow window) {
        perform(actionId, window.getTabbedPane().getComponent(), ActionPlaces.EDITOR_TAB, ActionUiKind.TOOLBAR);
    }

    private void perform(String actionId, Component component, String place, ActionUiKind uiKind) {
        AnAction action = ActionManager.getInstance().getAction(actionId);
        AnActionEvent event = createEvent(action, component, place, uiKind);
        ActionUtil.updateAction(action, event);
        assertTrue(actionId + " must be available", event.getPresentation().isEnabledAndVisible());
        ActionUtil.performAction(action, event);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    }

    private static AnActionEvent createEvent(AnAction action, Component component, String place, ActionUiKind uiKind) {
        DataContext dataContext = DataManager.getInstance().getDataContext(component);
        return AnActionEvent.createEvent(action, dataContext, null, place, uiKind, null);
    }
}
