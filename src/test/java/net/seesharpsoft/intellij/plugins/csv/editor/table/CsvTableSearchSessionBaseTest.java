package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.impl.Utils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.CsvPsiUpdateTimer;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CsvTableSearchSessionBaseTest extends CsvBasePlatformTestCase {
    private TestSearchSession search;
    private final AtomicInteger fieldLookups = new AtomicInteger();

    private void configure(String text) {
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        myFixture.configureByText(CsvFileType.INSTANCE, text);
        CsvTableEditor editor = mock(CsvTableEditor.class);
        JPanel component = new JPanel();
        when(editor.getProject()).thenReturn(getProject());
        when(editor.getPsiFile()).thenReturn(myFixture.getFile());
        when(editor.getDocument()).thenReturn(myFixture.getEditor().getDocument());
        when(editor.getComponent()).thenReturn(component);
        when(editor.getPreferredFocusedComponent()).thenReturn(component);
        when(editor.isEditable()).thenReturn(true);
        Disposer.register(getTestRootDisposable(), editor);
        CsvTableModel model = new CsvTableModelBase<>(editor) {
            @Override
            public PsiElement getFieldAt(int row, int column) {
                fieldLookups.incrementAndGet();
                return super.getFieldAt(row, column);
            }

            @Override
            public void notifyUpdate() {
                super.notifyUpdate();
                if (search != null) search.tableChanged();
            }
        };
        when(editor.getTableModel()).thenReturn(model);
        Disposer.register(getTestRootDisposable(), model);
        search = new TestSearchSession(editor);
        Disposer.register(getTestRootDisposable(), search);
        search.show(true);
    }

    private void query(String text) {
        search.getComponent().getSearchTextComponent().setText(text);
        search.searchFieldDocumentChanged();
        awaitSearch();
    }

    private void replacement(String text) {
        search.getComponent().getReplaceTextComponent().setText(text);
        search.replaceFieldDocumentChanged();
    }

    private void awaitSearch() {
        UIUtil.dispatchAllInvocationEvents();
        PlatformTestUtil.waitWithEventsDispatching("Waiting for background table search",
                () -> !search.isSearchInProgress(), 10000);
    }

    private void onUi(Runnable operation) {
        ApplicationManager.getApplication().invokeAndWait(operation, ModalityState.defaultModalityState());
    }

    private FindManager spyFindManager() {
        FindManager manager = spy(FindManager.getInstance(getProject()));
        ServiceContainerUtil.replaceService(getProject(), FindManager.class, manager, getTestRootDisposable());
        return manager;
    }

    private AtomicInteger countDocumentChanges() {
        AtomicInteger changes = new AtomicInteger();
        myFixture.getEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                changes.incrementAndGet();
            }
        }, getTestRootDisposable());
        return changes;
    }

    public void testSearchAndNavigationWithoutSwingTable() {
        configure("alpha alpha,beta\nbeta,ALPHA\n");
        assertSame(search, SearchSession.KEY.getData(Utils.createAsyncDataContext(search.getComponent())));
        query("alpha");
        assertEquals("1 of 3 matches", search.getComponent().getStatusText());
        assertEquals(0, search.selectedRow);
        assertEquals(0, search.selectedColumn);
        assertEquals(0, search.matchStart);
        assertEquals(5, search.matchEnd);
        search.searchForward();
        assertEquals(6, search.matchStart);
        assertEquals(11, search.matchEnd);
        search.searchBackward();
        assertEquals(0, search.matchStart);
        search.selectedRow = 1;
        search.selectedColumn = 0;
        search.searchForward();
        assertEquals(1, search.selectedRow);
        assertEquals(1, search.selectedColumn);
        search.searchForward();
        assertEquals(0, search.selectedRow);
        assertEquals(0, search.selectedColumn);
        search.close();
        assertFalse(search.highlighted);
        assertFalse(search.getComponent().isVisible());
    }

    public void testReplacementAdvancesAndPreservesCsvQuotingWithoutSwingTable() {
        configure("cat cat,cat\n");
        query("cat");
        replacement("cat,fish");
        search.replaceCurrent();
        awaitSearch();
        assertEquals("\"cat,fish cat\",cat\n", myFixture.getFile().getText());
        assertEquals("2 of 3 matches", search.getComponent().getStatusText());
        assertEquals(9, search.matchStart);
        replacement("dog");
        search.replaceAll();
        awaitSearch();
        assertEquals("\"dog,fish dog\",dog\n", myFixture.getFile().getText());
        assertFalse(search.hasMatches());
    }

    public void testSearchAndReplacementRespectFilteredRowsAndReorderedColumns() {
        configure("cat,cat\ncat,other\nother,cat\n");
        search.viewRows = new int[]{-1, 1, 0, 2};
        search.viewColumns = new int[]{1, 0};
        query("cat");
        assertEquals("1 of 2 matches", search.getComponent().getStatusText());
        assertEquals(0, search.selectedRow);
        assertEquals(0, search.selectedColumn);
        replacement("dog");
        search.replaceCurrent();
        awaitSearch();
        assertEquals("cat,cat\ncat,other\nother,dog\n", myFixture.getFile().getText());
        assertEquals(1, search.selectedRow);
        assertEquals(1, search.selectedColumn);
        search.replaceAll();
        awaitSearch();
        assertEquals("cat,cat\ndog,other\nother,dog\n", myFixture.getFile().getText());
        assertFalse(search.hasMatches());
    }

    public void testWideReplaceAllPreparesOffEdtWithoutRandomCellLookups() throws Exception {
        FindManager manager = spyFindManager();
        AtomicBoolean preparedOnEdt = new AtomicBoolean();
        AtomicInteger replacements = new AtomicInteger();
        doAnswer(invocation -> {
            preparedOnEdt.compareAndSet(false, ApplicationManager.getApplication().isDispatchThread());
            assertFalse("Preparation must not hold a PSI read lock", ApplicationManager.getApplication().isReadAccessAllowed());
            replacements.incrementAndGet();
            return invocation.callRealMethod();
        }).when(manager).getStringToReplace(anyString(), any(FindModel.class), anyInt(), anyString());
        String original = ("cat,".repeat(2047) + "cat\n").repeat(2);
        configure(original);
        query("cat");
        replacement("dog");
        AtomicInteger changes = countDocumentChanges();
        AtomicBoolean uiRan = new AtomicBoolean();
        search.beforePreparation = () -> onUi(() -> {
            assertFalse(search.hasMatches());
            uiRan.set(true);
        });
        fieldLookups.set(0);
        CsvPsiUpdateTimer psiUpdate = CsvPsiUpdateTimer.start(getProject(), getTestRootDisposable());
        long start = System.nanoTime();
        search.replaceAll();
        long preparedAndApplied = System.nanoTime() - start - psiUpdate.takeNanos();
        awaitSearch();
        assertEquals(original.replace("cat", "dog"), myFixture.getFile().getText());
        assertEquals(4096, replacements.get());
        assertTrue("The UI must remain responsive during preparation", uiRan.get());
        assertEquals(1, changes.get());
        assertFalse("Replacement preparation must run off the EDT", preparedOnEdt.get());
        assertEquals("Replace All must traverse fields once, not look up each cell", 0, fieldLookups.get());
        long millis = preparedAndApplied / 1_000_000;
        assertTrue("PSI must be updated for the replacement", psiUpdate.getTotalMillis() > 0);
        assertTrue("Replacing 4,096 wide-table cells took " + millis + " ms besides " +
                psiUpdate.getTotalMillis() + " ms PSI update", millis < 5000);
        System.out.println("Wide Replace All prepare/apply: " + millis + " ms besides " +
                psiUpdate.getTotalMillis() + " ms PSI update");
    }

    public void testCancellationPartwayThroughWideReplacementDoesNotApplyPartialChanges() throws Exception {
        FindManager manager = spyFindManager();
        AtomicInteger prepared = new AtomicInteger();
        doAnswer(invocation -> {
            if (prepared.incrementAndGet() == 128) ProgressManager.getInstance().getProgressIndicator().cancel();
            return invocation.callRealMethod();
        }).when(manager).getStringToReplace(anyString(), any(FindModel.class), anyInt(), anyString());
        String original = "cat,".repeat(2047) + "cat\n";
        configure(original);
        query("cat");
        replacement("dog");
        AtomicInteger changes = countDocumentChanges();
        search.replaceAll();
        assertEquals(128, prepared.get());
        assertEquals(0, changes.get());
        assertEquals(original, myFixture.getFile().getText());
        assertTrue(search.hasMatches());
        search.replaceAll();
        awaitSearch();
        assertEquals(1, changes.get());
        assertEquals(original.replace("cat", "dog"), myFixture.getFile().getText());
    }

    public void testCancellationAfterPreparationDoesNotCreateAnUndoStep() {
        configure("cat,cat\n");
        query("cat");
        replacement("dog");
        AtomicInteger changes = countDocumentChanges();
        UndoManager undo = UndoManager.getInstance(getProject());
        var textEditor = TextEditorProvider.getInstance().getTextEditor(myFixture.getEditor());
        var undoAction = undo.getUndoActionNameAndDescription(textEditor);
        search.afterPreparation = () -> ProgressManager.getInstance().getProgressIndicator().cancel();
        search.replaceAll();
        assertEquals(0, changes.get());
        assertEquals("cat,cat\n", myFixture.getFile().getText());
        assertTrue(search.hasMatches());
        assertEquals(undoAction, undo.getUndoActionNameAndDescription(textEditor));
    }

    public void testExternalEditRejectsPreparedReplacement() {
        configure("cat,cat\n");
        query("cat");
        replacement("dog");
        search.afterPreparation = () -> onUi(() -> WriteCommandAction.runWriteCommandAction(getProject(), () ->
                myFixture.getEditor().getDocument().insertString(0, "# external edit\n")));
        search.replaceAll();
        awaitSearch();
        assertEquals("# external edit\ncat,cat\n", myFixture.getFile().getText());
        assertTrue(search.hasMatches());
    }

    public void testCancellationAfterApplyingStillRefreshesTheModel() {
        configure("cat,cat\n");
        query("cat");
        replacement("dog");
        AtomicReference<ProgressIndicator> indicator = new AtomicReference<>();
        search.beforePreparation = () -> indicator.set(ProgressManager.getInstance().getProgressIndicator());
        myFixture.getEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                indicator.get().cancel();
            }
        }, getTestRootDisposable());
        search.replaceAll();
        awaitSearch();
        assertEquals("dog,dog\n", myFixture.getFile().getText());
        assertFalse(search.editor.getTableModel().isSuspended());
        assertFalse(search.hasMatches());
        UndoManager.getInstance(getProject()).undo(TextEditorProvider.getInstance().getTextEditor(myFixture.getEditor()));
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals("cat,cat\n", myFixture.getFile().getText());
    }

    public void testChangedQueryRejectsPreparedReplacement() {
        configure("cat,other\n");
        query("cat");
        replacement("dog");
        search.afterPreparation = () -> onUi(() -> query("other"));
        search.replaceAll();
        awaitSearch();
        assertEquals("cat,other\n", myFixture.getFile().getText());
        assertEquals("1 of 1 matches", search.getComponent().getStatusText());
    }

    public void testUnchangedReplacementDoesNotModifyDocument() {
        configure("cat,cat\n");
        query("cat");
        replacement("cat");
        AtomicInteger changes = countDocumentChanges();
        search.replaceAll();
        awaitSearch();
        assertEquals(0, changes.get());
        assertEquals("cat,cat\n", myFixture.getFile().getText());
        assertTrue(search.hasMatches());
    }

    public void testChangedCsvSettingsRejectPreparedReplacement() {
        configure("cat,cat\n");
        query("cat");
        replacement("dog");
        search.afterPreparation = () -> onUi(() -> CsvEditorSettings.getInstance().setCommentIndicator("##"));
        try {
            search.replaceAll();
            assertEquals("cat,cat\n", myFixture.getFile().getText());
            assertTrue(search.hasMatches());
        } finally {
            CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        }
    }

    public void testReadOnlyDocumentRejectsPreparedReplacement() {
        configure("cat,cat\n");
        query("cat");
        replacement("dog");
        search.afterPreparation = () -> onUi(() -> myFixture.getEditor().getDocument().setReadOnly(true));
        try {
            search.replaceAll();
            assertEquals("cat,cat\n", myFixture.getFile().getText());
        } finally {
            myFixture.getEditor().getDocument().setReadOnly(false);
        }
    }

    public void testClosingSearchRejectsPreparedReplacement() {
        configure("cat,cat\n");
        query("cat");
        replacement("dog");
        search.afterPreparation = () -> onUi(search::close);
        search.replaceAll();
        assertEquals("cat,cat\n", myFixture.getFile().getText());
        assertFalse(search.getComponent().isVisible());
    }

    public void testDisposingSearchRejectsPreparedReplacement() {
        configure("cat,cat\n");
        query("cat");
        replacement("dog");
        search.afterPreparation = () -> onUi(search::dispose);
        search.replaceAll();
        assertEquals("cat,cat\n", myFixture.getFile().getText());
        assertFalse(search.hasMatches());
    }

    public void testPreparationFailureRestoresReplacementActions() {
        configure("cat,cat\n");
        query("cat");
        replacement("dog");
        search.beforePreparation = () -> {
            throw new IllegalStateException("Test preparation failure");
        };
        assertThrows(IllegalStateException.class, search::replaceAll);
        assertEquals("cat,cat\n", myFixture.getFile().getText());
        assertTrue(search.hasMatches());
    }

    public void testWideFilteredAndReorderedReplacementUsesSnapshotCoordinates() {
        String row = "cat,".repeat(1023) + "cat\n";
        configure(row.repeat(3));
        search.viewRows = new int[]{1, -1, 0, 2};
        search.viewColumns = new int[1024];
        for (int column = 0; column < search.viewColumns.length; column++) {
            search.viewColumns[column] = 1023 - column;
        }
        query("cat");
        replacement("dog");
        search.replaceAll();
        awaitSearch();
        assertEquals(row.replace("cat", "dog") + row + row.replace("cat", "dog"), myFixture.getFile().getText());
    }

    private static final class TestSearchSession extends CsvTableSearchSessionBase<CsvTableEditor> {
        private int selectedRow = -1;
        private int selectedColumn = -1;
        private int matchStart;
        private int matchEnd;
        private boolean highlighted;
        private int[] viewRows;
        private int[] viewColumns;
        private Runnable beforePreparation = () -> {
        };
        private Runnable afterPreparation = () -> {
        };

        private TestSearchSession(CsvTableEditor editor) {
            super(editor);
        }

        @Override
        protected boolean runReplaceAllWithProgress(Runnable operation) {
            return super.runReplaceAllWithProgress(() -> {
                assertFalse(ApplicationManager.getApplication().isDispatchThread());
                beforePreparation.run();
                operation.run();
                afterPreparation.run();
            });
        }

        @Override
        protected int getSelectedRow() {
            return selectedRow;
        }

        @Override
        protected int getSelectedColumn() {
            return selectedColumn;
        }

        @Override
        protected boolean stopEditing() {
            return true;
        }

        @Override
        protected void selectMatch(Match match) {
            selectedRow = match.row();
            selectedColumn = match.column();
            matchStart = match.start();
            matchEnd = match.end();
            highlighted = true;
        }

        @Override
        protected void clearMatchHighlight() {
            highlighted = false;
        }

        @Override
        protected int[] getViewRowIndices() {
            return viewRows == null ? null : viewRows.clone();
        }

        @Override
        protected int[] getViewColumnIndices() {
            return viewColumns == null ? super.getViewColumnIndices() : viewColumns.clone();
        }

        @Override
        protected int convertRowIndexToModel(int row) {
            return modelIndex(row, viewRows);
        }

        @Override
        protected int convertColumnIndexToModel(int column) {
            return modelIndex(column, viewColumns);
        }

        private static int modelIndex(int viewIndex, int[] viewIndices) {
            if (viewIndices == null) return viewIndex;
            for (int modelIndex = 0; modelIndex < viewIndices.length; modelIndex++) {
                if (viewIndices[modelIndex] == viewIndex) return modelIndex;
            }
            throw new IllegalArgumentException("No model index for view index " + viewIndex);
        }
    }
}
