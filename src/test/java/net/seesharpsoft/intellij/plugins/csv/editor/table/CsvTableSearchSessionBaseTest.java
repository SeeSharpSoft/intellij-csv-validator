package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.find.SearchSession;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import javax.swing.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CsvTableSearchSessionBaseTest extends CsvBasePlatformTestCase {
    private TestSearchSession search;

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
                () -> !search.isSearchInProgress(), 10);
    }

    public void testSearchAndNavigationWithoutSwingTable() {
        configure("alpha alpha,beta\nbeta,ALPHA\n");
        assertSame(search, search.getComponent().getData(SearchSession.KEY.getName()));
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

    private static final class TestSearchSession extends CsvTableSearchSessionBase<CsvTableEditor> {
        private int selectedRow = -1;
        private int selectedColumn = -1;
        private int matchStart;
        private int matchEnd;
        private boolean highlighted;
        private int[] viewRows;
        private int[] viewColumns;

        private TestSearchSession(CsvTableEditor editor) {
            super(editor);
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
        protected int convertColumnIndexToView(int column) {
            return viewColumns == null ? column : viewColumns[column];
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
