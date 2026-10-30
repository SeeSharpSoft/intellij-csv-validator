package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.find.SearchReplaceComponent;
import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.impl.Utils;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;

import javax.swing.table.TableColumnModel;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CsvTableColumnMappingPerformanceTest extends CsvTableEditorSwingTestBase {
    @Override
    protected CsvTableEditorSwing createEditor() {
        return new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile()) {
            @Override
            protected CsvTable createTable(CsvTableModelSwing model) {
                return new CountingTable(model);
            }
        };
    }

    private CountingTable configure(String text) {
        fileEditor.dispose();
        myFixture.configureByText(CsvFileType.INSTANCE, text);
        fileEditor = createEditor();
        fileEditor.selectNotify();
        return (CountingTable) fileEditor.getTable();
    }

    public void testSearchSnapshotsThousandsOfReorderedAndHiddenColumnsWithoutLinearLookups() {
        int count = 4096;
        CountingTable table = configure("needle," + "other,".repeat(count - 3) + "hidden,needle\n");
        fileEditor.showSearch(false);
        SearchReplaceComponent bar = UIUtil.findComponentOfType(fileEditor.getComponent(), SearchReplaceComponent.class);
        assertNotNull(bar);
        CsvTableSearchSession search = (CsvTableSearchSession) SearchSession.KEY.getData(Utils.createAsyncDataContext(bar));
        query(search, "missing");
        TableColumnModel columns = table.getColumnModel();
        CsvTableHeader header = (CsvTableHeader) table.getTableHeader();
        header.setDraggedColumn(columns.getColumn(count - 1));
        try {
            columns.moveColumn(count - 1, 0);
            columns.moveColumn(1, count / 2);
            columns.removeColumn(columns.getColumn(count - 1));
            table.viewLookups = 0;
            query(search, "needle");
            assertEquals("1 of 2 matches", search.getComponent().getStatusText());
            assertEquals(0, table.getSelectedColumn());
            search.searchForward();
            assertEquals(count / 2, table.getSelectedColumn());
            query(search, "hidden");
            assertFalse(search.hasMatches());
            assertEquals("Search snapshots must not perform linear model-to-view lookups", 0, table.viewLookups);
        } finally {
            header.cancelColumnReordering();
        }
    }

    public void testNormalizingThousandsOfColumnsReusesTheColumnMapping() {
        int count = 2048;
        String original = IntStream.range(0, count).mapToObj(Integer::toString).collect(Collectors.joining(",")) + "\n";
        CountingTable table = configure(original);
        int[] widths = IntStream.range(0, count).map(column -> 80 + column % 100).toArray();
        fileEditor.getTableEditorState().setColumnWidths(widths);
        fileEditor.updateEditorLayout();
        TableColumnModel columns = table.getColumnModel();
        CsvTableHeader header = (CsvTableHeader) table.getTableHeader();
        header.setDraggedColumn(columns.getColumn(count - 1));
        columns.moveColumn(count - 1, 0);
        columns.moveColumn(count / 2, count / 4);
        columns.moveColumn(1, count / 2);
        int[] order = IntStream.range(0, count).map(column -> columns.getColumn(column).getModelIndex()).toArray();
        table.changeSelection(0, count / 2, false, false);
        table.viewLookups = 0;
        header.setDraggedColumn(null);
        assertEquals(Arrays.stream(order).mapToObj(Integer::toString).collect(Collectors.joining(",")) + "\n",
                myFixture.getFile().getText());
        assertEquals(count / 2, table.getSelectedColumn());
        for (int column = 0; column < count; column++) {
            assertEquals(column, table.getColumnModel().getColumn(column).getModelIndex());
        }
        int[] movedWidths = Arrays.stream(order).map(column -> widths[column]).toArray();
        assertTrue(Arrays.equals(movedWidths, fileEditor.getTableEditorState().getColumnWidths()));
        assertEquals("Normalization must not perform linear model-to-view lookups", 0, table.viewLookups);
    }

    private void query(CsvTableSearchSession search, String text) {
        search.getComponent().getSearchTextComponent().setText(text);
        search.searchFieldDocumentChanged();
        UIUtil.dispatchAllInvocationEvents();
        PlatformTestUtil.waitWithEventsDispatching("Waiting for wide-table search",
                () -> !search.isSearchInProgress(), 10000);
    }

    private static final class CountingTable extends CsvTable {
        private int viewLookups;

        private CountingTable(CsvTableModelSwing model) {
            super(model);
        }

        @Override
        public int convertColumnIndexToView(int column) {
            viewLookups++;
            return super.convertColumnIndexToView(column);
        }
    }
}
