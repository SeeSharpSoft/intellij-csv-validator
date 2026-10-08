package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.ThrowableRunnable;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.LoggedErrorProcessor;
import com.intellij.testFramework.PsiTestUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings.COMMENT_INDICATOR_DEFAULT;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import org.mockito.MockedStatic;

public class CsvTableModelBaseTest extends CsvBasePlatformTestCase implements PsiFileHolder {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor/table/default";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings.getInstance().setAutoDetectValueSeparator(false);
        CsvCodeStyleSettings csvCodeStyleSettings = CodeStyleSettingsManager.getInstance(getProject()).getTemporarySettings().getCustomSettings(CsvCodeStyleSettings.class);
        csvCodeStyleSettings.SPACE_BEFORE_SEPARATOR = false;
        csvCodeStyleSettings.SPACE_AFTER_SEPARATOR = false;
        csvCodeStyleSettings.TRIM_LEADING_WHITE_SPACES = false;
        csvCodeStyleSettings.TRIM_TRAILING_WHITE_SPACES = false;
    }

    @Override
    protected void tearDown() throws Exception {
        CsvEditorSettings.getInstance().setAutoDetectValueSeparator(true);
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvEditorSettings.VALUE_SEPARATOR_DEFAULT);
        CsvEditorSettings.getInstance().setCommentIndicator(COMMENT_INDICATOR_DEFAULT);
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEscapeCharacter.QUOTE);
        super.tearDown();
    }

    protected void autoCheck(Consumer<CsvTableModel> runnable) {
        autoCheck(runnable, getTestName(false));
    }

    protected void autoCheck(Consumer<CsvTableModel> runnable, String testName) {
        autoCheck(runnable, testName, null);
    }

    protected void autoCheck(Consumer<CsvTableModel> runnable, String testName, String relativeTargetPath) {
        if (relativeTargetPath == null || relativeTargetPath.isEmpty()) {
            relativeTargetPath = ".";
        }

        myFixture.configureByFiles(relativeTargetPath + "/Original.csv");

        CsvTableModel model = new CsvTableModelBase(this);
        runnable.accept(model);
        model.dispose();

        Document doc = this.myFixture.getDocument(getPsiFile());
//        PsiDocumentManager.getInstance(getProject()).doPostponedOperationsAndUnblockDocument(doc);
        PsiTestUtil.checkFileStructure(getPsiFile());

        myFixture.checkResultByFile(relativeTargetPath + String.format("/%s.csv", testName));
    }

    protected void manualCheck(@NotNull Consumer<CsvTableModel> verifier) {
        manualCheck(null, verifier, null);
    }

    protected void manualCheck(@Nullable Consumer<CsvTableModel> executor, @NotNull Consumer<CsvTableModel> verifier) {
        manualCheck(executor, verifier, null);
    }

    protected void manualCheck(@Nullable Consumer<CsvTableModel> runnable, @NotNull Consumer<CsvTableModel> verifier, @Nullable String relativeTargetPath) {
        if (relativeTargetPath == null || relativeTargetPath.isEmpty()) {
            relativeTargetPath = ".";
        }

        myFixture.configureByFiles(relativeTargetPath + "/Original.csv");

        CsvTableModel model = new CsvTableModelBase(this);

        if (runnable != null) runnable.accept(model);

        Document doc = this.myFixture.getDocument(getPsiFile());
        PsiDocumentManager.getInstance(getProject()).doPostponedOperationsAndUnblockDocument(doc);
        PsiTestUtil.checkFileStructure(getPsiFile());

        verifier.accept(model);

        model.dispose();
    }

    public void testColumnCount() {
        manualCheck(csvTableModel -> assertEquals(9, csvTableModel.getColumnCount()));
    }

    public void testRowCount() {
        manualCheck(csvTableModel -> assertEquals(9, csvTableModel.getRowCount()));
    }

    public void testNotifyUpdateFromEdt() throws Exception {
        myFixture.configureByFiles("Original.csv");
        CsvTableModel model = new CsvTableModelBase(this);
        try {
            EdtTestUtil.runInEdtAndWait(() -> {
                assertFalse(model.isCommentRow(0));
                assertEquals("Header 1", model.getValue(0, 0));
            });
        } finally {
            model.dispose();
        }
    }

    public void testNotifyUpdateRequiresReadAction() throws Exception {
        PsiFileHolder holder = mock(PsiFileHolder.class);
        when(holder.getPsiFile()).thenReturn(mock(PsiFile.class));
        CsvTableModel model = new CsvTableModelBase(holder);
        try (MockedStatic<ReadAction> readAction = mockStatic(ReadAction.class)) {
            readAction.when(() -> ReadAction.run(any(ThrowableRunnable.class)))
                    .thenAnswer(invocation -> {
                        invocation.<ThrowableRunnable<?>>getArgument(0).run();
                        return null;
                    });
            readAction.when(() -> ReadAction.compute(any(ThrowableComputable.class)))
                    .thenAnswer(invocation -> ((ThrowableComputable<?, ?>) invocation.getArgument(0)).compute());
            model.notifyUpdate();
            readAction.verify(() -> ReadAction.run(any(ThrowableRunnable.class)));
        } finally {
            model.dispose();
        }
    }

    public void testReadOperationsRequireReadAction() throws Exception {
        PsiFileHolder holder = mock(PsiFileHolder.class);
        PsiFile file = mock(PsiFile.class);
        CsvRecord record = mock(CsvRecord.class);
        when(holder.getPsiFile()).thenReturn(file);
        when(file.getFirstChild()).thenReturn(record);
        when(record.getFirstChild()).thenReturn(null);
        CsvTableModel model = new CsvTableModelBase(holder);
        try (MockedStatic<ReadAction> readAction = mockStatic(ReadAction.class)) {
            readAction.when(() -> ReadAction.compute(any(ThrowableComputable.class)))
                    .thenAnswer(invocation -> ((ThrowableComputable<?, ?>) invocation.getArgument(0)).compute());
            assertFalse(model.isCommentRow(0));
            model.getFieldAt(0, 0);
            readAction.verify(() -> ReadAction.compute(any(ThrowableComputable.class)));
        } finally {
            model.dispose();
        }
    }

    public void testIsCommentRow() {
        manualCheck(csvTableModel -> {
            assertEquals(true, csvTableModel.isCommentRow(5));
            assertEquals(false, csvTableModel.isCommentRow(4));
        });
    }

    // TODO setValue/getValue!!

    public void testGetValue() {
        manualCheck(csvTableModel -> {
            assertEquals("Header 1", csvTableModel.getValue(0, 0));
            assertEquals("", csvTableModel.getValue(100, 100));
            assertEquals("Header 2", csvTableModel.getValue(0, 1));
            assertEquals("Header, \"5\"", csvTableModel.getValue(0, 4));
            assertEquals("  Value 3 ", csvTableModel.getValue(1, 2));
            assertEquals(" Value 4", csvTableModel.getValue(1, 3));
            assertEquals("", csvTableModel.getValue(2, 1));
            assertEquals(" I am a comment ,;|\" nothing happens ,,,,,,,,,,,", csvTableModel.getValue(5, 3));
            assertEquals("#not a comment", csvTableModel.getValue(7, 4));
            assertEquals(";:|\n\tvalue 2", csvTableModel.getValue(8, 3));
            assertEquals("\\\\\\\\end", csvTableModel.getValue(8, 5));
        });
    }

    public void testSetValues() {
        autoCheck(csvTableModel -> {
            csvTableModel.setValue("New Header", 0, 0);
            csvTableModel.setValue("Other value with comma, and \"quotes\"", 0, 1);
            csvTableModel.setValue("  Value 3 ", 1, 2);
            csvTableModel.setValue(" Value 4 ", 1, 3);
            csvTableModel.setValue("BO\nOM", 2, 1);
            csvTableModel.setValue("Just another comment,,\nall is normal", 5, 3);
            csvTableModel.setValue(null, 7, 4);
            csvTableModel.setValue("", 8, 3);
            csvTableModel.setValue(";:|\\\tvalue 2", 8, 5);

            assertEquals("New Header", csvTableModel.getValue(0, 0));
            assertEquals("Other value with comma, and \"quotes\"", csvTableModel.getValue(0, 1));
            assertEquals("  Value 3 ", csvTableModel.getValue(1, 2));
            assertEquals(" Value 4 ", csvTableModel.getValue(1, 3));
            assertEquals("BO\nOM", csvTableModel.getValue(2, 1));
            assertEquals("Just another comment,,", csvTableModel.getValue(5, 3));
            assertEquals("", csvTableModel.getValue(7, 4));
            assertEquals("", csvTableModel.getValue(8, 3));
            assertEquals(";:|\\\tvalue 2", csvTableModel.getValue(8, 5));
        });
    }

    public void testSetQuotedValueFromEdt() throws Exception {
        myFixture.configureByFiles("Original.csv");
        CsvTableModel model = new CsvTableModelBase<>(this);
        try {
            LoggedErrorProcessor.executeWith(new LoggedErrorProcessor() {
                @Override
                public @NotNull Set<Action> processError(@NotNull String category,
                                                          @NotNull String message,
                                                          String @NotNull [] details,
                                                          @Nullable Throwable t) {
                    return Set.of(Action.RETHROW);
                }
            }, () -> EdtTestUtil.runInEdtAndWait(() -> model.setValue("New Header, 5", 0, 4)));
            assertEquals("New Header, 5", model.getValue(0, 4));
        } finally {
            model.dispose();
        }
    }

    public void testAddColumnAfterLast() {
        autoCheck(csvTableModel -> csvTableModel.addColumn(csvTableModel.getColumnCount() - 1, false));
    }

    public void testAddColumnAfterMiddle() {
        autoCheck((csvTableModel -> csvTableModel.addColumn(3, false)));
    }

    public void testAddColumnBeforeFirst() {
        autoCheck((csvTableModel -> csvTableModel.addColumn(0, true)));
    }

    public void testAddRowAfterComment() {
        autoCheck((csvTableModel -> csvTableModel.addRow(5, false)));
    }

    public void testAddRowAfterEmpty() {
        autoCheck((csvTableModel -> csvTableModel.addRow(4, false)));
    }

    public void testAddRowAfterLast() {
        autoCheck((csvTableModel -> csvTableModel.addRow(csvTableModel.getRowCount() - 1, false)));
    }

    public void testAddRowBeforeEmpty() {
        autoCheck((csvTableModel -> csvTableModel.addRow(4, true)));
    }

    public void testAddRowBeforeFirst() {
        autoCheck((csvTableModel -> csvTableModel.addRow(0, true)));
    }

    public void testDeleteAllColumns() {
        autoCheck((csvTableModel -> csvTableModel.removeColumns(Arrays.asList(1, 0, 4, 2, 3, 5, 6, 7, 8))));
    }

    public void testDeleteAllColumnsExtra() {
        // provide more column indices than available -> will not trigger deletion of all content
        autoCheck((csvTableModel -> csvTableModel.removeColumns(Arrays.asList(1, 0, 4, 2, 3, 5, 6, 7, 8, 9))));
    }

    public void testDeleteAllRows() {
        autoCheck((csvTableModel -> csvTableModel.removeRows(Arrays.asList(1, 0, 2, 3, 6, 4, 5, 7, 8))));
    }

    public void testDeleteCommentRow() {
        autoCheck((csvTableModel -> csvTableModel.removeRow(5)));
    }

    public void testDeleteEmptyRow() {
        autoCheck((csvTableModel -> csvTableModel.removeRow(4)));
    }

    public void testDeleteFirstColumn() {
        autoCheck((csvTableModel -> csvTableModel.removeColumn(0)));
    }

    public void testDeleteFirstRow() {
        autoCheck((csvTableModel -> csvTableModel.removeRow(0)));
    }

    public void testDeleteLastColumn() {
        autoCheck((csvTableModel -> csvTableModel.removeColumn(csvTableModel.getColumnCount() - 1)));
    }

    public void testDeleteLastRow() {
        autoCheck((csvTableModel -> csvTableModel.removeRow(csvTableModel.getRowCount() - 1)));
    }

    public void testDeleteMultipleConnectedColumns() {
        autoCheck((csvTableModel -> csvTableModel.removeColumns(Arrays.asList(2, 0, 1))));
    }

    public void testDeleteMultipleConnectedRows() {
        autoCheck((csvTableModel -> csvTableModel.removeRows(Arrays.asList(2, 0, 1))));
    }

    public void testDeleteMultipleColumns() {
        autoCheck((csvTableModel -> csvTableModel.removeColumns(Arrays.asList(csvTableModel.getColumnCount() - 1, 0, 5))));
    }

    public void testDeleteColumnsUsesSingleDocumentChange() {
        String separator = CsvEditorSettings.getInstance().getDefaultValueSeparator().getCharacter();
        String comment = CsvEditorSettings.getInstance().getCommentIndicator() + "keep this comment\n";
        String row = String.join(separator, "first", "second", "third", "fourth", "") + "\n";
        String expectedRow = String.join(separator, "first", "third", "") + "\n";
        checkSingleDocumentChange(comment + row.repeat(1000) + "short\n",
                comment + expectedRow.repeat(1000) + "short\n",
                model -> model.removeColumns(Arrays.asList(3, 1, 1)));
    }

    public void testAddColumnUsesSingleDocumentChange() {
        String separator = CsvEditorSettings.getInstance().getDefaultValueSeparator().getCharacter();
        String comment = CsvEditorSettings.getInstance().getCommentIndicator() + "keep this comment\n";
        String row = String.join(separator, "first", "second", "third") + "\n";
        String expectedRow = String.join(separator, "first", "", "second", "third") + "\n";
        checkSingleDocumentChange(comment + row.repeat(1000), comment + expectedRow.repeat(1000) + separator,
                model -> model.addColumn(1, true));
    }

    private void checkSingleDocumentChange(String original, String expected, Consumer<CsvTableModel> operation) {
        myFixture.configureByText(CsvFileType.INSTANCE, original);
        Document document = myFixture.getDocument(getPsiFile());
        AtomicInteger changes = new AtomicInteger();
        DocumentListener listener = new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                changes.incrementAndGet();
            }
        };
        CsvTableModel model = new CsvTableModelBase(this);
        document.addDocumentListener(listener);
        try {
            operation.accept(model);
            assertEquals(expected, document.getText());
            assertEquals(expected, getPsiFile().getText());
            assertEquals("Column operations must notify document listeners only once", 1, changes.get());
            PsiTestUtil.checkFileStructure(getPsiFile());
        } finally {
            document.removeDocumentListener(listener);
            model.dispose();
        }
    }

    public void testDeleteMultipleRows() {
        autoCheck((csvTableModel -> csvTableModel.removeRows(Arrays.asList(csvTableModel.getRowCount() - 1, 0, 6))));
    }

    @Override
    public PsiFile getPsiFile() {
        return myFixture == null ? null : myFixture.getFile();
    }

    @Override
    public void dispose() {

    }
}
