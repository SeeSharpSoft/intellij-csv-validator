package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvFileEditorManagerTestCase;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorProvider;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTable;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableEditorSwing;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import javax.swing.SwingConstants;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class CsvEditorFieldSyncTest extends CsvFileEditorManagerTestCase {

    private static final String CONTENT = "name,value\nalpha,1\nbeta,2\n";

    private VirtualFile csvFile;

    @Override
    public void setUp() {
        System.setProperty("hidpi", "false");
        super.setUp();
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        csvFile = myFixture.addFileToProject("sample.csv", CONTENT).getVirtualFile();
    }

    @Override
    public void tearDown() throws Exception {
        try {
            CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        } finally {
            super.tearDown();
        }
    }

    public void testTableSelectsFieldAtTextCaretWhenShown() {
        CsvEditorTab tab = openCsvFile();
        moveTextCaret(tab, CONTENT.indexOf("2"));

        tab.show(CsvTableEditorProvider.EDITOR_TYPE_ID);

        awaitSelectedField(() -> tableEditor(tab).getSelectedField(), new CsvFieldPosition(2, 1));
    }

    public void testTextCaretMovesToSelectedTableCellWhenShown() {
        CsvEditorTab tab = openCsvFile();
        showTableAndAwait(tab);
        clickTableCell(tab, 1, 0);

        tab.show(CsvFileEditorProvider.EDITOR_TYPE_ID);

        PlatformTestUtil.waitWithEventsDispatching("text caret must move to the selected table cell",
                () -> textEditor(tab).getEditor().getCaretModel().getOffset() == CONTENT.indexOf("alpha"), 10);
    }

    public void testBottomTabSwitchSyncsSelectedField() {
        CsvEditorTab tab = openCsvFile();
        moveTextCaret(tab, CONTENT.indexOf("beta"));

        tab.composite().setSelectedEditor(CsvTableEditorProvider.EDITOR_TYPE_ID);

        awaitSelectedField(() -> tableEditor(tab).getSelectedField(), new CsvFieldPosition(2, 0));
    }

    public void testTextCaretStaysWithinUnchangedField() {
        CsvEditorTab tab = openCsvFile();
        int caretInsideAlpha = CONTENT.indexOf("alpha") + 2;
        moveTextCaret(tab, caretInsideAlpha);
        showTableAndAwait(tab);

        showTextAndAwait(tab);

        assertEquals(caretInsideAlpha, textEditor(tab).getEditor().getCaretModel().getOffset());
    }

    public void testTextCaretStaysWithinCommentRowSelectedInAnyTableColumn() {
        csvFile = myFixture.addFileToProject("commented.csv", "a,b,c\n#note\n").getVirtualFile();
        CsvEditorTab tab = openCsvFile();
        int caretInsideComment = "a,b,c\n#no".length();
        moveTextCaret(tab, caretInsideComment);
        showTableAndAwait(tab);
        clickTableCell(tab, 1, 2);

        showTextAndAwait(tab);

        assertEquals(caretInsideComment, textEditor(tab).getEditor().getCaretModel().getOffset());
    }

    public void testTableSelectsRowAddedWhileItWasHidden() {
        CsvEditorTab tab = openCsvFile();
        Document document = textEditor(tab).getEditor().getDocument();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(document.getTextLength(), "gamma,3\n"));
        moveTextCaret(tab, document.getText().indexOf("3"));

        tab.show(CsvTableEditorProvider.EDITOR_TYPE_ID);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

        awaitSelectedField(() -> tableEditor(tab).getSelectedField(), new CsvFieldPosition(3, 1));
    }

    public void testDisabledSyncKeepsEachEditorsSelection() {
        CsvEditorSettings.getInstance().setSyncSelectedField(false);
        CsvEditorTab tab = openCsvFile();
        moveTextCaret(tab, CONTENT.indexOf("2"));

        showTableAndAwait(tab);
        assertNull(tableEditor(tab).getSelectedField());
        clickTableCell(tab, 1, 0);
        showTextAndAwait(tab);

        assertEquals(CONTENT.indexOf("2"), textEditor(tab).getEditor().getCaretModel().getOffset());
    }

    public void testFocusingAnotherSplitWindowKeepsSelectionsOfBothWindows() {
        CsvEditorTab firstTab = openCsvFile();
        moveTextCaret(firstTab, CONTENT.indexOf("2"));
        manager.createSplitter(SwingConstants.VERTICAL, firstTab.window());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        EditorWindow secondWindow = Arrays.stream(manager.getWindows())
                .filter(window -> window != firstTab.window())
                .findFirst()
                .orElseThrow();
        CsvEditorTab secondTab = new CsvEditorTab(secondWindow, waitForEditors(secondWindow.getComposite(csvFile)));
        showTableAndAwait(secondTab);
        clickTableCell(secondTab, 1, 0);
        runAndAwaitSelectionChange(textEditor(firstTab), () -> firstTab.window().setAsCurrentWindow(false));

        runAndAwaitSelectionChange(tableEditor(secondTab), () -> secondWindow.setAsCurrentWindow(false));

        assertEquals(CONTENT.indexOf("2"), textEditor(firstTab).getEditor().getCaretModel().getOffset());
        assertEquals(new CsvFieldPosition(1, 0), tableEditor(secondTab).getSelectedField());
    }

    private CsvEditorTab openCsvFile() {
        manager.openFile(csvFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        EditorWindow window = manager.getCurrentWindow();
        assertNotNull(window);
        return new CsvEditorTab(window, waitForEditors(window.getComposite(csvFile)));
    }

    private static CsvTextEditor textEditor(CsvEditorTab tab) {
        return (CsvTextEditor) tab.composite().getAllEditors().get(0);
    }

    private static CsvTableEditorSwing tableEditor(CsvEditorTab tab) {
        return (CsvTableEditorSwing) tab.composite().getAllEditors().get(1);
    }

    private static void moveTextCaret(CsvEditorTab tab, int offset) {
        textEditor(tab).getEditor().getCaretModel().moveToOffset(offset);
    }

    private static void clickTableCell(CsvEditorTab tab, int row, int column) {
        CsvTable table = UIUtil.findComponentOfType(tableEditor(tab).getComponent(), CsvTable.class);
        assertNotNull(table);
        table.changeSelection(row, column, false, false);
    }

    private void showTableAndAwait(CsvEditorTab tab) {
        runAndAwaitSelectionChange(tableEditor(tab), () -> tab.show(CsvTableEditorProvider.EDITOR_TYPE_ID));
    }

    private void showTextAndAwait(CsvEditorTab tab) {
        runAndAwaitSelectionChange(textEditor(tab), () -> tab.show(CsvFileEditorProvider.EDITOR_TYPE_ID));
    }

    /** Waits until the IDE has notified its listeners, including the field sync, about the newly selected editor. */
    private void runAndAwaitSelectionChange(FileEditor newEditor, Runnable change) {
        AtomicBoolean notified = new AtomicBoolean();
        var disposable = Disposer.newDisposable();
        try {
            getProject().getMessageBus().connect(disposable).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                    new FileEditorManagerListener() {
                        @Override
                        public void selectionChanged(FileEditorManagerEvent event) {
                            if (event.getNewEditor() == newEditor) notified.set(true);
                        }
                    });
            change.run();
            PlatformTestUtil.waitWithEventsDispatching("editor selection must change", notified::get, 10);
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        } finally {
            Disposer.dispose(disposable);
        }
    }

    private static void awaitSelectedField(Supplier<CsvFieldPosition> selectedField, CsvFieldPosition expected) {
        PlatformTestUtil.waitWithEventsDispatching(() -> "expected " + expected + " but was " + selectedField.get(),
                () -> expected.equals(selectedField.get()), 10);
    }
}
