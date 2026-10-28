package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.Disposer;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModelBase.ModelState;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CsvTableModelSwingTest extends CsvBasePlatformTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        CsvEditorSettings.getInstance().setAutoDetectValueSeparator(false);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        } finally {
            super.tearDown();
        }
    }

    public void testSnapshotMatchesOrdinaryRefreshWithCommentsAndRaggedRows() {
        assertSnapshotParity("# leading\n\" First \"\"header\"\" \", second ,\none\n# between\na,b,c,d",
                5, 4, Set.of(0, 3), List.of("First \"header\"", "second", ""), false);
    }

    public void testSnapshotMatchesOrdinaryRefreshWithCustomSettings() {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        settings.setDefaultValueSeparator(new CsvValueSeparator("$~"));
        settings.setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
        settings.setCommentIndicator("///");
        settings.setZeroBasedColumnNumbering(true);
        assertSnapshotParity("/// leading\n\" First \\\"header\\\" \"$~\"multi\nline\"$~\nvalue",
                3, 3, Set.of(0), List.of("First \"header\"", "multi\nline", ""), false);
    }

    public void testSnapshotMatchesOrdinaryRefreshWithOnlyComments() {
        assertSnapshotParity("# first\n# second", 2, 0, Set.of(0, 1), List.of(), false);
    }

    public void testSnapshotMatchesOrdinaryRefreshWithEmptyFile() {
        assertSnapshotParity("", 1, 1, Set.of(), List.of(""), false);
    }

    public void testSnapshotMatchesOrdinaryRefreshWithBlankFirstAndLastRows() {
        assertSnapshotParity("\nheader,value\n1,2\n", 4, 2, Set.of(), List.of(""), false);
    }

    public void testSnapshotMatchesOrdinaryRefreshWithParseError() {
        assertSnapshotParity("head,other\n\"unterminated",
                2, 2, Set.of(), List.of("head", "other"), true);
    }

    private void assertSnapshotParity(String text, int rows, int columns, Set<Integer> comments,
                                      List<String> headers, boolean errors) {
        myFixture.configureByText(getTestName(false) + ".csv", text);
        CsvTableEditor holder = mock(CsvTableEditor.class);
        when(holder.getPsiFile()).thenReturn(myFixture.getFile());
        CsvTableModelSwing model = new CsvTableModelSwing(holder) {
            @Override
            protected boolean applyState(ModelState state) {
                boolean applied = super.applyState(state);
                assertTrue("A fresh snapshot must be applied rather than falling back to an ordinary refresh", applied);
                return applied;
            }
        };
        try {
            ModelState state = ReadAction.compute(model::captureState);
            assertEquals(state.toString(), rows, state.rows());
            assertEquals(state.toString(), columns, state.columns());
            assertEquals(comments, state.comments());
            assertEquals(headers, state.headers());
            assertEquals(errors, state.errors());

            model.notifyUpdate();
            assertMatches(state, model);
            List<String> ordinaryColumnNames = readColumnNames(model);

            model.prepareUpdate(state);
            model.updateTableImmediately();
            assertMatches(state, model);
            assertEquals(ordinaryColumnNames, readColumnNames(model));

            model.notifyUpdate();
            assertMatches(state, model);
            assertEquals(ordinaryColumnNames, readColumnNames(model));
        } finally {
            model.dispose();
            Disposer.dispose(holder);
        }
    }

    private void assertMatches(ModelState state, CsvTableModelSwing model) {
        assertEquals(state.rows(), model.getRowCount());
        assertEquals(state.columns(), model.getColumnCount());
        assertEquals(state.errors(), model.hasErrors());
        for (int row = 0; row < state.rows(); row++) {
            assertEquals("Comment row " + row, state.comments().contains(row), model.isCommentRow(row));
        }
        for (int column = 0; column < state.columns(); column++) {
            String header = column < state.headers().size() ? state.headers().get(column) : "";
            int displayedColumn = CsvEditorSettings.getInstance().isZeroBasedColumnNumbering() ? column : column + 1;
            assertEquals(header + " (" + displayedColumn + ")", model.getColumnName(column));
        }
    }

    private List<String> readColumnNames(CsvTableModelSwing model) {
        List<String> names = new ArrayList<>();
        for (int column = 0; column < model.getColumnCount(); column++) {
            names.add(model.getColumnName(column));
        }
        return names;
    }
}
