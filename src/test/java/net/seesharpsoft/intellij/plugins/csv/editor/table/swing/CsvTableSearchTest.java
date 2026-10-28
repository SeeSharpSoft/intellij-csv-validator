package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.find.SearchReplaceComponent;
import com.intellij.find.SearchSession;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.editorHeaderActions.NextOccurrenceAction;
import com.intellij.find.editorHeaderActions.PrevOccurrenceAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.util.ManualUpdateScheduler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CsvTableSearchTest extends CsvTableEditorSwingTestBase {
    private CsvTableSearchSession search;

    private void configure(String text) {
        fileEditor.dispose();
        myFixture.configureByText(CsvFileType.INSTANCE, text);
        fileEditor = createEditor();
        fileEditor.selectNotify();
        fileEditor.showSearch(true);
        SearchReplaceComponent bar = UIUtil.findComponentOfType(fileEditor.getComponent(), SearchReplaceComponent.class);
        assertNotNull(bar);
        search = (CsvTableSearchSession) bar.getData(SearchSession.KEY.getName());
    }

    private ManualUpdateScheduler configureDelayed(String text) {
        ManualUpdateScheduler scheduler = new ManualUpdateScheduler();
        updateScheduler = scheduler;
        configure(text);
        scheduler.advanceBy(50);
        assertEquals(0, scheduler.pendingCount());
        return scheduler;
    }

    private AtomicInteger trackTableUpdates() {
        AtomicInteger updates = new AtomicInteger();
        fileEditor.getTable().getModel().addTableModelListener(event -> updates.incrementAndGet());
        return updates;
    }

    private void commitText(String text) {
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            myFixture.getEditor().getDocument().setText(text);
            PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        });
    }

    private void query(String text) {
        setQuery(text);
        awaitSearch();
    }

    private void setQuery(String text) {
        search.getComponent().getSearchTextComponent().setText(text);
        search.searchFieldDocumentChanged();
    }

    private void awaitSearch() {
        UIUtil.dispatchAllInvocationEvents();
        PlatformTestUtil.waitWithEventsDispatching("Waiting for background table search", () -> !search.isSearchInProgress(), 10000);
    }

    private void replaceCurrent() {
        search.replaceCurrent();
        awaitSearch();
    }

    private void replaceAll() {
        search.replaceAll();
        awaitSearch();
    }

    private void replacement(String text) {
        search.getComponent().getReplaceTextComponent().setText(text);
        search.replaceFieldDocumentChanged();
    }

    private void assertCell(int row, int column) {
        assertEquals(row, fileEditor.getTable().getSelectedRow());
        assertEquals(column, fileEditor.getTable().getSelectedColumn());
    }

    public void testCellSearchRunsOffTheUiThread() {
        FindManager manager = spyFindManager();
        AtomicBoolean scannedOnUi = new AtomicBoolean();
        AtomicInteger scannedCells = new AtomicInteger();
        doAnswer(invocation -> {
            scannedOnUi.compareAndSet(false, ApplicationManager.getApplication().isDispatchThread());
            scannedCells.incrementAndGet();
            return invocation.callRealMethod();
        }).when(manager).findString(any(CharSequence.class), anyInt(), any(FindModel.class));
        configure("alpha,beta\n".repeat(1000));
        long started = System.nanoTime();
        setQuery("alpha");
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        awaitSearch();
        assertTrue("Search should examine real cells", scannedCells.get() > 0);
        assertFalse("Scanned " + scannedCells.get() + " cells on the UI thread in " + elapsedMillis + " ms", scannedOnUi.get());
    }

    private FindManager spyFindManager() {
        FindManager manager = spy(FindManager.getInstance(getProject()));
        ServiceContainerUtil.replaceService(getProject(), FindManager.class, manager, getTestRootDisposable());
        return manager;
    }

    public void testRapidTypingPublishesOnlyLatestQuery() {
        FindManager manager = spyFindManager();
        configure("alpha,beta\n");
        setQuery("a");
        setQuery("al");
        setQuery("alpha");
        assertTrue(search.isSearchInProgress());
        assertEquals("Searching...", search.getComponent().getStatusText());
        awaitSearch();
        verify(manager, atLeastOnce()).findString(any(CharSequence.class), anyInt(), argThat(model -> "alpha".equals(model.getStringToFind())));
        assertEquals("alpha", search.getFindModel().getStringToFind());
        assertEquals("1 of 1 matches", search.getComponent().getStatusText());
    }

    public void testSearchStartsImmediatelyWithoutWaitingForAnotherUiEvent() throws Exception {
        try (BlockedSearch blocked = new BlockedSearch("slow")) {
            configure("initial,slow\n");
            setQuery("slow");
            assertTrue("Search must start immediately without a typing timer", blocked.started.await(10, TimeUnit.SECONDS));
            search.close();
            blocked.awaitCanceled();
        }
    }

    public void testBlockedBackgroundSearchLeavesUiResponsiveAndNewQueryCancelsIt() {
        try (BlockedSearch blocked = new BlockedSearch("slow")) {
            configure("initial,fast\nslow,end\n");
            setQuery("slow");
            blocked.awaitStarted();
            AtomicBoolean uiEventRan = new AtomicBoolean();
            SwingUtilities.invokeLater(() -> uiEventRan.set(true));
            UIUtil.dispatchAllInvocationEvents();
            assertTrue("UI events must run while the scan is blocked", uiEventRan.get());
            assertTrue(search.isSearchInProgress());
            assertFalse(search.hasMatches());
            query("fast");
            blocked.awaitCanceled();
            assertEquals("fast", search.getFindModel().getStringToFind());
            assertEquals("1 of 1 matches", search.getComponent().getStatusText());
            assertCell(0, 1);
        }
    }

    public void testClosingSearchCancelsBackgroundWork() {
        try (BlockedSearch blocked = new BlockedSearch("slow")) {
            configure("initial,fast\nslow,end\n");
            setQuery("slow");
            blocked.awaitStarted();
            search.close();
            blocked.awaitCanceled();
            assertFalse(search.isSearchInProgress());
            assertFalse(search.getComponent().isVisible());
            fileEditor.showSearch(false);
            awaitSearch();
            assertEquals("1 of 1 matches", search.getComponent().getStatusText());
            assertCell(1, 0);
        }
    }

    public void testDisposingEditorCancelsBackgroundWork() {
        try (BlockedSearch blocked = new BlockedSearch("slow")) {
            configure("initial,slow\n");
            setQuery("slow");
            blocked.awaitStarted();
            fileEditor.dispose();
            blocked.awaitCanceled();
            assertFalse(search.isSearchInProgress());
            assertFalse(search.hasMatches());
        }
    }

    public void testDocumentChangeDuringSearchCannotPublishOldCellPositions() {
        try (BlockedSearch blocked = new BlockedSearch("slow")) {
            configure("initial,slow\n");
            setQuery("slow");
            blocked.awaitStarted();
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                myFixture.getEditor().getDocument().setText("slow,new\n");
                PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
            });
            awaitSearch();
            blocked.awaitCanceled();
            assertEquals("1 of 1 matches", search.getComponent().getStatusText());
            assertCell(0, 0);
        }
    }

    public void testChangingReplacementDoesNotRescanCells() {
        FindManager manager = spyFindManager();
        configure("cat,cat\n");
        query("cat");
        clearInvocations(manager);
        replacement("dog");
        awaitSearch();
        verify(manager, never()).findString(any(CharSequence.class), anyInt(), any(FindModel.class));
        assertEquals("1 of 2 matches", search.getComponent().getStatusText());
    }

    private final class BlockedSearch implements AutoCloseable {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicBoolean finished = new AtomicBoolean();
        private final AtomicBoolean blocked = new AtomicBoolean();

        private BlockedSearch(String query) {
            FindManager manager = spyFindManager();
            doAnswer(invocation -> {
                FindModel model = invocation.getArgument(2);
                if (query.equals(model.getStringToFind()) && blocked.compareAndSet(false, true)) {
                    started.countDown();
                    try {
                        while (!release.await(10, TimeUnit.MILLISECONDS)) ProgressManager.checkCanceled();
                    } finally {
                        finished.set(true);
                    }
                }
                return invocation.callRealMethod();
            }).when(manager).findString(any(CharSequence.class), anyInt(), any(FindModel.class));
        }

        private void awaitStarted() {
            PlatformTestUtil.waitWithEventsDispatching("Waiting for scan to start", () -> started.getCount() == 0, 10000);
        }

        private void awaitCanceled() {
            PlatformTestUtil.waitWithEventsDispatching("Waiting for canceled scan to stop", finished::get, 10000);
        }

        @Override
        public void close() {
            release.countDown();
        }
    }

    public void testTotalMatchCountIsVisibleInSearchBar() {
        configure("alpha alpha,beta\nbeta,ALPHA\n");
        query("alpha");
        assertTrue("Match count must be rendered, not only stored in SearchReplaceComponent",
                UIUtil.findComponentsOfType(search.getComponent(), JLabel.class).stream()
                        .anyMatch(label -> label.isVisible() && "1 of 3 matches".equals(label.getText())));
    }

    public void testSearchControlsStayVisibleAndLeftAlignedWhenResized() {
        configure("alpha alpha,beta\nbeta,ALPHA\n");
        query("alpha");
        SearchReplaceComponent bar = search.getComponent();
        OnePixelSplitter splitter = UIUtil.findComponentOfType(bar, OnePixelSplitter.class);
        assertNotNull(splitter);
        JComponent controls = splitter.getSecondComponent();
        for (boolean replace : new boolean[]{false, true}) {
            search.getFindModel().setReplaceState(replace);
            for (ActionToolbarImpl toolbar : UIUtil.findComponentsOfType(controls, ActionToolbarImpl.class)) {
                Future<?> update = toolbar.updateActionsAsync();
                PlatformTestUtil.waitWithEventsDispatching("Waiting for search controls", update::isDone, 10000);
            }
            for (int width : new int[]{1000, 500, 320}) {
                bar.setSize(width, bar.getPreferredSize().height);
                layoutRecursively(bar);
                assertTrue("Controls must retain their minimum width", controls.getWidth() >= controls.getMinimumSize().width);
                JLabel count = UIUtil.findComponentsOfType(controls, JLabel.class).stream()
                        .filter(label -> "1 of 3 matches".equals(label.getText())).findFirst().orElseThrow();
                ActionButton next = UIUtil.findComponentsOfType(controls, ActionButton.class).stream()
                        .filter(button -> button.getAction() instanceof NextOccurrenceAction).findFirst().orElseThrow();
                ActionButton previous = UIUtil.findComponentsOfType(controls, ActionButton.class).stream()
                        .filter(button -> button.getAction() instanceof PrevOccurrenceAction).findFirst().orElseThrow();
                Component close = UIUtil.findComponentsOfType(controls, ActionButton.class).stream()
                        .filter(button -> "CloseAction".equals(button.getAction().getClass().getSimpleName()))
                        .map(button -> (Component) button).findFirst().orElseGet(() ->
                                UIUtil.findComponentsOfType(controls, JLabel.class).stream()
                                        .filter(label -> label.getIcon() == AllIcons.Actions.Close).findFirst().orElseThrow());
                Rectangle countBounds = SwingUtilities.convertRectangle(count.getParent(), count.getBounds(), controls);
                Rectangle nextBounds = SwingUtilities.convertRectangle(next.getParent(), next.getBounds(), controls);
                Rectangle previousBounds = SwingUtilities.convertRectangle(previous.getParent(), previous.getBounds(), controls);
                Rectangle closeBounds = SwingUtilities.convertRectangle(close.getParent(), close.getBounds(), controls);
                assertEquals(controls.getInsets().left, countBounds.x);
                assertEquals(SwingConstants.LEFT, count.getHorizontalAlignment());
                assertTrue(countBounds.x + countBounds.width <= nextBounds.x);
                assertTrue(nextBounds.x + nextBounds.width <= previousBounds.x);
                assertTrue(previousBounds.x + previousBounds.width <= closeBounds.x);
                assertTrue("Close must follow the arrows without stretching to the right edge",
                        closeBounds.x - previousBounds.x - previousBounds.width <= 12);
                assertTrue("Close must remain inside the controls", closeBounds.x + closeBounds.width <= controls.getWidth());
                assertTrue("Controls must fit inside the search bar", controls.getX() + controls.getWidth() <= splitter.getWidth());
            }
        }
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layoutRecursively(nested);
        }
    }

    public void testSearchNavigatesAndReplacesAcrossFrozenBoundary() {
        configure("id,name,city,age,role,team\n1,Alice,Berlin,30,Developer,Platform\n");
        fileEditor.getTableEditorState().setColumnWidths(new int[]{100, 100, 100, 100, 100, 100});
        fileEditor.updateEditorLayout();
        JPanel root = new JPanel(new BorderLayout());
        root.add(fileEditor.getComponent());
        root.setSize(460, 220);
        layoutRecursively(root);
        fileEditor.setFrozenColumnCount(2);
        JViewport viewport = fileEditor.getTableScrollPane().getViewport();
        viewport.setViewPosition(new Point(230, 0));
        query("Alice");
        assertCell(1, 1);
        assertEquals(230, viewport.getViewPosition().x);
        replacement("Bob");
        replaceCurrent();
        assertEquals("Bob", fileEditor.getTableModel().getValue(1, 1));
        query("Berlin");
        assertCell(1, 2);
        assertEquals(0, viewport.getViewPosition().x);
        replacement("Paris");
        replaceCurrent();
        assertEquals("Paris", fileEditor.getTableModel().getValue(1, 2));
        assertEquals(2, fileEditor.getFrozenColumnCount());
    }

    public void testFindAndReplaceUseIdeShortcuts() {
        for (String actionId : new String[]{IdeActions.ACTION_FIND, IdeActions.ACTION_REPLACE}) {
            AnAction action = ActionUtil.getActions(fileEditor.getComponent()).stream()
                    .filter(candidate -> Arrays.equals(candidate.getShortcutSet().getShortcuts(),
                            ActionManager.getInstance().getAction(actionId).getShortcutSet().getShortcuts()))
                    .findFirst().orElseThrow();
            action.actionPerformed(TestActionEvent.createTestEvent());
            SearchReplaceComponent bar = UIUtil.findComponentOfType(fileEditor.getComponent(), SearchReplaceComponent.class);
            assertNotNull(bar);
            SearchSession session = (SearchSession) bar.getData(SearchSession.KEY.getName());
            assertNotNull(session);
            assertEquals(IdeActions.ACTION_REPLACE.equals(actionId), session.getFindModel().isReplaceState());
            assertTrue(bar.isVisible());
        }
    }

    public void testNavigationWrapsAcrossRowsColumnsAndOccurrences() {
        configure("alpha alpha,beta\nbeta,ALPHA\n");
        query("alpha");
        assertCell(0, 0);
        assertEquals("1 of 3 matches", search.getComponent().getStatusText());
        search.searchForward();
        assertCell(0, 0);
        assertEquals("2 of 3 matches", search.getComponent().getStatusText());
        search.searchForward();
        assertCell(1, 1);
        search.searchForward();
        assertCell(0, 0);
        search.searchBackward();
        assertCell(1, 1);
    }

    public void testNavigationSelectsEachOccurrenceInsideTheCell() {
        configure("alpha alpha,end\n");
        query("alpha");
        assertMatchSelection(0, 0, 0, 5);
        search.searchForward();
        assertMatchSelection(0, 0, 6, 11);
        search.searchBackward();
        assertMatchSelection(0, 0, 0, 5);
    }

    private void assertMatchSelection(int row, int column, int start, int end) {
        assertCell(row, column);
        JTable table = fileEditor.getTable();
        assertFalse("Finding text must not start cell editing", table.isEditing());
        JComponent rendered = (JComponent) table.prepareRenderer(table.getCellRenderer(row, column), row, column);
        JTextArea area = UIUtil.findComponentOfType(rendered, JTextArea.class);
        assertNotNull(area);
        assertEquals(start, area.getSelectionStart());
        assertEquals(end, area.getSelectionEnd());
        assertEquals("Find must retain the whole-cell selection background",
                table.getSelectionBackground(), area.getBackground());
        assertTrue("The substring selection must remain visible while the search field has focus",
                area.getCaret().isSelectionVisible());
        assertFalse("The matching text must remain distinct from the cell background",
                area.getSelectionColor().equals(area.getBackground()));
    }

    public void testSearchStartsAtSelectedCellAndFindsQuotedMultilineValue() {
        configure("alpha,beta\nother,\"first\nalpha\"\n");
        fileEditor.getTable().changeSelection(1, 0, false, false);
        query("alpha");
        assertCell(1, 1);
        search.searchBackward();
        assertCell(0, 0);
        query("first\nalpha");
        assertEquals("first\nalpha", search.getFindModel().getStringToFind());
        assertCell(1, 1);
        assertEquals("1 of 1 matches", search.getComponent().getStatusText());
    }

    public void testMatchCaseAndWholeWords() {
        configure("cat,CAT,scatter\n");
        query("cat");
        assertEquals("1 of 3 matches", search.getComponent().getStatusText());
        search.getFindModel().setCaseSensitive(true);
        awaitSearch();
        assertEquals("1 of 2 matches", search.getComponent().getStatusText());
        search.getFindModel().setWholeWordsOnly(true);
        awaitSearch();
        assertEquals("1 of 1 matches", search.getComponent().getStatusText());
    }

    public void testNoMatchesAndEmptyQueryPreserveSelection() {
        configure("one,two\n");
        fileEditor.getTable().changeSelection(0, 1, false, false);
        query("absent");
        assertFalse(search.hasMatches());
        assertEquals("No matches", search.getComponent().getStatusText());
        search.searchForward();
        assertCell(0, 1);
        query("");
        assertFalse(search.hasMatches());
        assertEquals("", search.getComponent().getStatusText());
    }

    public void testReplaceCurrentAdvancesPastInsertedQuery() {
        configure("cat cat,cat\n");
        query("cat");
        replacement("catfish");
        replaceCurrent();
        assertEquals("catfish cat,cat\n", myFixture.getFile().getText());
        assertEquals("2 of 3 matches", search.getComponent().getStatusText());
        replaceCurrent();
        assertEquals("catfish catfish,cat\n", myFixture.getFile().getText());
        assertCell(0, 1);
    }

    public void testEmptyReplacementDoesNotSkipAdjacentOccurrence() {
        configure("catcat,end\n");
        query("cat");
        replacement("");
        replaceCurrent();
        assertEquals("cat,end\n", myFixture.getFile().getText());
        replaceCurrent();
        assertEquals(",end\n", myFixture.getFile().getText());
        assertFalse(search.hasMatches());
    }

    public void testDelayedReplacementAdvancesPastInsertedQueryOnlyAfterDelivery() {
        ManualUpdateScheduler scheduler = configureDelayed("cat cat,cat\n");
        query("cat");
        replacement("catfish");
        AtomicInteger updates = trackTableUpdates();
        replaceCurrent();
        assertEquals("catfish cat,cat\n", myFixture.getFile().getText());
        assertFalse(search.hasMatches());
        assertEquals(1, scheduler.pendingCount());
        scheduler.advanceBy(49);
        assertEquals(0, updates.get());
        assertFalse(search.hasMatches());
        scheduler.advanceBy(1);
        awaitSearch();
        assertEquals(1, updates.get());
        assertEquals(0, scheduler.pendingCount());
        assertEquals("2 of 3 matches", search.getComponent().getStatusText());
        assertMatchSelection(0, 0, 8, 11);

        replaceCurrent();
        assertFalse(search.hasMatches());
        scheduler.advanceBy(50);
        awaitSearch();
        assertEquals("catfish catfish,cat\n", myFixture.getFile().getText());
        assertEquals(2, updates.get());
        assertMatchSelection(0, 1, 0, 3);
    }

    public void testDelayedEmptyReplacementDoesNotSkipAdjacentOccurrence() {
        ManualUpdateScheduler scheduler = configureDelayed("catcat,end\n");
        query("cat");
        replacement("");
        replaceCurrent();
        assertFalse(search.hasMatches());
        scheduler.advanceBy(50);
        awaitSearch();
        assertMatchSelection(0, 0, 0, 3);
        replaceCurrent();
        scheduler.advanceBy(50);
        awaitSearch();
        assertEquals(",end\n", myFixture.getFile().getText());
        assertFalse(search.hasMatches());
        assertEquals("No matches", search.getComponent().getStatusText());
    }

    public void testInterveningCommittedEditPreservesReplacementAnchor() {
        ManualUpdateScheduler scheduler = configureDelayed("cat cat,cat\n");
        query("cat");
        replacement("catfish");
        AtomicInteger updates = trackTableUpdates();
        replaceCurrent();
        scheduler.advanceBy(25);
        String latest = "catfish cat,cat,cat\ncat,new,end\n";
        commitText(latest);
        assertEquals(1, scheduler.pendingCount());
        assertFalse(search.hasMatches());
        scheduler.advanceBy(25);
        assertEquals(0, updates.get());
        scheduler.advanceBy(24);
        assertEquals(0, updates.get());
        scheduler.advanceBy(1);
        awaitSearch();
        assertEquals(1, updates.get());
        assertEquals(latest, myFixture.getFile().getText());
        assertEquals(3, getTableModel().getRowCount());
        assertEquals(3, getTableModel().getColumnCount());
        assertEquals("2 of 5 matches", search.getComponent().getStatusText());
        assertMatchSelection(0, 0, 8, 11);
    }

    public void testDelayedReplaceAllAndUndoRefreshCommittedValues() {
        String original = "cat cat,cat\ncat,end\n";
        ManualUpdateScheduler scheduler = configureDelayed(original);
        query("cat");
        replacement("a,\"b\"\nc");
        AtomicInteger updates = trackTableUpdates();
        replaceAll();
        assertEquals(1, scheduler.pendingCount());
        assertFalse(search.hasMatches());
        scheduler.advanceBy(49);
        assertEquals(0, updates.get());
        scheduler.advanceBy(1);
        awaitSearch();
        assertEquals(1, updates.get());
        assertEquals("a,\"b\"\nc a,\"b\"\nc", getTableModel().getValue(0, 0));
        assertEquals("a,\"b\"\nc", getTableModel().getValue(1, 0));
        assertFalse(getTableModel().hasErrors());
        assertFalse(search.hasMatches());

        TextEditor textEditor = TextEditorProvider.getInstance().getTextEditor(myFixture.getEditor());
        UndoManager.getInstance(getProject()).undo(textEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(original, myFixture.getFile().getText());
        assertEquals(1, scheduler.pendingCount());
        assertEquals(1, updates.get());
        scheduler.advanceBy(50);
        awaitSearch();
        assertEquals(2, updates.get());
        assertEquals(0, scheduler.pendingCount());
        assertEquals("cat cat", getTableModel().getValue(0, 0));
        assertEquals("1 of 4 matches", search.getComponent().getStatusText());
        assertMatchSelection(0, 0, 0, 3);
    }

    public void testDisposingEditorCancelsDelayedReplacementRefresh() {
        FindManager manager = spyFindManager();
        ManualUpdateScheduler scheduler = configureDelayed("cat cat,cat\n");
        query("cat");
        replacement("catfish");
        AtomicInteger updates = trackTableUpdates();
        replaceCurrent();
        Runnable queued = scheduler.pendingUpdate();
        clearInvocations(manager);
        fileEditor.dispose();
        assertEquals(0, scheduler.pendingCount());
        scheduler.advanceBy(50);
        queued.run(); // A callback already dispatched before disposal must also be harmless.
        UIUtil.dispatchAllInvocationEvents();
        assertEquals(0, updates.get());
        assertFalse(search.isSearchInProgress());
        assertFalse(search.hasMatches());
        assertEquals("catfish cat,cat\n", myFixture.getFile().getText());
        verify(manager, never()).findString(any(CharSequence.class), anyInt(), any(FindModel.class));
    }

    public void testClosingSearchBeforeDelayedRefreshDoesNotRestartIt() {
        FindManager manager = spyFindManager();
        ManualUpdateScheduler scheduler = configureDelayed("cat,end\n");
        query("cat");
        commitText("dog,cat\n");
        search.close();
        clearInvocations(manager);
        scheduler.advanceBy(50);
        awaitSearch();
        assertFalse(search.getComponent().isVisible());
        assertFalse(search.isSearchInProgress());
        verify(manager, never()).findString(any(CharSequence.class), anyInt(), any(FindModel.class));
        fileEditor.showSearch(false);
        awaitSearch();
        assertMatchSelection(0, 1, 0, 3);
        assertEquals("1 of 1 matches", search.getComponent().getStatusText());
    }

    public void testReplaceAllQuotesSeparatorsQuotesAndNewlinesAndIsOneUndoStep() {
        String original = "cat cat,cat\ncat,end\n";
        configure(original);
        query("cat");
        replacement("a,\"b\"\nc");
        AtomicInteger changes = new AtomicInteger();
        myFixture.getEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                changes.incrementAndGet();
            }
        }, getTestRootDisposable());
        replaceAll();
        assertEquals(1, changes.get());
        assertEquals("a,\"b\"\nc a,\"b\"\nc", getTableModel().getValue(0, 0));
        assertEquals("a,\"b\"\nc", getTableModel().getValue(1, 0));
        assertFalse(getTableModel().hasErrors());
        assertFalse(search.hasMatches());
        UndoManager undo = UndoManager.getInstance(getProject());
        TextEditor textEditor = TextEditorProvider.getInstance().getTextEditor(myFixture.getEditor());
        undo.undo(textEditor);
        assertEquals(original, myFixture.getEditor().getDocument().getText());
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        awaitSearch();
        assertEquals(original, myFixture.getFile().getText());
        assertTrue(search.hasMatches());
    }

    public void testRegexReplacementUsesCaptureGroups() {
        configure("item12,item34\n");
        search.getFindModel().setRegularExpressions(true);
        query("item(\\d+)");
        replacement("value$1");
        replaceAll();
        assertEquals("value12,value34\n", myFixture.getFile().getText());
    }

    public void testInvalidRegexAndReplacementDoNotModifyFile() {
        String original = "item12,item34\n";
        configure(original);
        search.getFindModel().setRegularExpressions(true);
        query("[");
        assertFalse(search.hasMatches());
        replaceAll();
        assertEquals(original, myFixture.getFile().getText());
        query("item(\\d+)");
        replacement("$9");
        replaceAll();
        assertEquals(original, myFixture.getFile().getText());
    }

    public void testZeroWidthRegexTerminatesAndReplacesOncePerPosition() {
        configure("ab,cd\n");
        search.getFindModel().setRegularExpressions(true);
        query("^");
        replacement("prefix");
        replaceAll();
        assertEquals("prefixab,prefixcd\n", myFixture.getFile().getText());
    }

    public void testLiteralReplacementPreservesDollarAndBackslash() {
        configure("cat,cat\n");
        query("cat");
        replacement("$1\\path");
        replaceAll();
        assertEquals("$1\\path,$1\\path\n", myFixture.getFile().getText());
    }

    public void testCommentIsSearchedAndReplacedOnlyOnce() {
        configure("# cat\ncat,cat\n");
        query("cat");
        assertEquals("1 of 3 matches", search.getComponent().getStatusText());
        replacement("dog");
        replaceAll();
        assertEquals("# dog\ndog,dog\n", myFixture.getFile().getText());
    }

    public void testReplaceCurrentPreservesLeadingCommentIndicatorInContent() {
        assertReplacementPreservesCommentContent("#", false);
    }

    public void testReplaceAllPreservesLeadingCommentIndicatorInContent() {
        assertReplacementPreservesCommentContent("#", true);
    }

    public void testReplaceCurrentPreservesLeadingCustomCommentIndicatorInContent() {
        assertReplacementPreservesCommentContent("///", false);
    }

    public void testReplaceAllPreservesLeadingCustomCommentIndicatorInContent() {
        assertReplacementPreservesCommentContent("///", true);
    }

    private void assertReplacementPreservesCommentContent(String indicator, boolean all) {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        String previousIndicator = settings.getCommentIndicator();
        settings.setCommentIndicator(indicator);
        try {
            for (String replacement : Arrays.asList("dog", indicator + "dog", "cat")) {
                for (String original : Arrays.asList(indicator + indicator + "cat\n",
                        indicator + indicator + "cat cat\n" + indicator + " " + indicator + "cat\nuntouched,end\n")) {
                    configure(original);
                    query("cat");
                    assertTrue(search.hasMatches());
                    replacement(replacement);
                    if (all) replaceAll();
                    else replaceCurrent();
                    int offset = original.indexOf("cat");
                    String expected = all ? original.replace("cat", replacement)
                            : original.substring(0, offset) + replacement + original.substring(offset + 3);
                    assertEquals(expected, myFixture.getFile().getText());
                }
            }
        } finally {
            settings.setCommentIndicator(previousIndicator);
        }
    }

    public void testFirstColumnReplacementCannotTurnDataIntoComment() {
        configure("cat,end\n");
        query("cat");
        replacement("# data");
        replaceAll();
        assertEquals("\"# data\",end\n", myFixture.getFile().getText());
        assertFalse(getTableModel().isCommentRow(0));
    }

    public void testReadOnlyTableCanSearchButCannotReplace() throws Exception {
        configure("cat,end\n");
        WriteAction.run(() -> fileEditor.getFile().setWritable(false));
        try {
            query("cat");
            assertTrue(search.hasMatches());
            replacement("dog");
            replaceCurrent();
            replaceAll();
            assertEquals("cat,end\n", myFixture.getFile().getText());
        } finally {
            WriteAction.run(() -> fileEditor.getFile().setWritable(true));
        }
    }

    public void testExternalDocumentChangesRefreshResults() {
        configure("cat,end\n");
        query("cat");
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            myFixture.getEditor().getDocument().setText("dog,cat\n");
            PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        });
        awaitSearch();
        assertCell(0, 1);
        assertEquals("1 of 1 matches", search.getComponent().getStatusText());
    }

    public void testCommittedEditsCoalesceUntilTheLatestRefreshDeadline() {
        ManualUpdateScheduler scheduler = configureDelayed("cat,end\n");
        query("cat");
        AtomicInteger updates = trackTableUpdates();
        commitText("dog,cat\n");
        scheduler.advanceBy(25);
        String latest = "one,two,three\nfour,five,cat\nsix,seven,cat\n";
        commitText(latest);
        assertEquals(1, scheduler.pendingCount());
        assertFalse(search.hasMatches());
        scheduler.advanceBy(25);
        assertEquals(0, updates.get());
        scheduler.advanceBy(24);
        assertEquals(0, updates.get());
        assertEquals(2, getTableModel().getRowCount());
        assertEquals(2, getTableModel().getColumnCount());
        scheduler.advanceBy(1);
        awaitSearch();
        assertEquals(1, updates.get());
        assertEquals(0, scheduler.pendingCount());
        assertEquals(4, getTableModel().getRowCount());
        assertEquals(3, getTableModel().getColumnCount());
        assertEquals("three", getTableModel().getValue(0, 2));
        assertMatchSelection(1, 2, 0, 3);
        assertEquals("1 of 2 matches", search.getComponent().getStatusText());
    }

    public void testOpeningSearchCommitsActiveCellAndCloseKeepsResultSelected() {
        ManualUpdateScheduler scheduler = configureDelayed("cat,end\n");
        search.close();
        AtomicInteger updates = trackTableUpdates();
        JTable table = fileEditor.getTable();
        assertTrue(table.editCellAt(0, 1));
        JTextArea area = UIUtil.findComponentOfType((JComponent) table.getEditorComponent(), JTextArea.class);
        assertNotNull(area);
        area.setText("dog");
        fileEditor.showSearch(false);
        assertFalse(table.isEditing());
        assertEquals("cat,dog\n", myFixture.getFile().getText());
        assertEquals(1, scheduler.pendingCount());
        assertEquals(0, updates.get());
        scheduler.advanceBy(50);
        query("dog");
        assertEquals(1, updates.get());
        assertCell(0, 1);
        search.close();
        assertFalse(search.getComponent().isVisible());
        assertCell(0, 1);
        fileEditor.showSearch(true);
        assertTrue(search.getComponent().isVisible());
        assertEquals("dog", search.getFindModel().getStringToFind());
    }
}
