package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.PsiTestUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;

import static net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings.COMMENT_INDICATOR_DEFAULT;

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
