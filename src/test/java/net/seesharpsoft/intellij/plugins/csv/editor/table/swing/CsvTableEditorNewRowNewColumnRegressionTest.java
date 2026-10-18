package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public class CsvTableEditorNewRowNewColumnRegressionTest extends CsvBasePlatformTestCase {
    private CsvTableEditorSwing editor;

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        myFixture.configureByText(CsvFileType.INSTANCE,
                "Date,Time,Virtual Memory Committed [MB]\n" +
                "31.1.2026,11:11:56.690,12447\n" +
                "31.1.2026,11:12:0.686,12418\n");
        editor = new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile());
        editor.selectNotify();
    }

    @Override
    protected void tearDown() throws Exception {
        editor.dispose();
        super.tearDown();
    }

    public void testAddingRowThenColumnThroughTableActionsPreservesRows() {
        editor.getTable().setRowSelectionInterval(1, 1);
        editor.getTable().setColumnSelectionInterval(0, 0);

        editor.tableEditorActions.addRowBefore.actionPerformed(null);
        assertEquals("Date,Time,Virtual Memory Committed [MB]\n" +
                "\n" +
                "31.1.2026,11:11:56.690,12447\n" +
                "31.1.2026,11:12:0.686,12418\n", myFixture.getFile().getText());

        editor.tableEditorActions.addColumnAfter.actionPerformed(null);

        assertEquals("Date,,Time,Virtual Memory Committed [MB]\n" +
                ",\n" +
                "31.1.2026,,11:11:56.690,12447\n" +
                "31.1.2026,,11:12:0.686,12418\n", myFixture.getFile().getText());
    }

    public void testAddingRowBeforeFirstThenColumnAfterFirstPreservesRanges() {
        editor.getTable().setRowSelectionInterval(0, 0);
        editor.getTable().setColumnSelectionInterval(0, 0);

        editor.tableEditorActions.addRowBefore.actionPerformed(null);
        assertEquals("\n" +
                "Date,Time,Virtual Memory Committed [MB]\n" +
                "31.1.2026,11:11:56.690,12447\n" +
                "31.1.2026,11:12:0.686,12418\n", myFixture.getFile().getText());

        editor.tableEditorActions.addColumnAfter.actionPerformed(null);

        assertEquals(",\n" +
                "Date,,Time,Virtual Memory Committed [MB]\n" +
                "31.1.2026,,11:11:56.690,12447\n" +
                "31.1.2026,,11:12:0.686,12418\n", myFixture.getFile().getText());
    }

    public void testAddingRowBeforeFirstThenColumnAfterFirstWithoutTrailingLineBreakPreservesRanges() {
        editor.dispose();
        myFixture.configureByText(CsvFileType.INSTANCE,
                "Date,Time,Virtual Memory Committed [MB]\n" +
                "31.1.2026,11:11:56.690,12447\n" +
                "31.1.2026,11:12:0.686,12418");
        editor = new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile());
        editor.selectNotify();
        editor.getTable().setRowSelectionInterval(0, 0);
        editor.getTable().setColumnSelectionInterval(0, 0);

        editor.tableEditorActions.addRowBefore.actionPerformed(null);
        editor.tableEditorActions.addColumnAfter.actionPerformed(null);

        assertEquals(",\n" +
                "Date,,Time,Virtual Memory Committed [MB]\n" +
                "31.1.2026,,11:11:56.690,12447\n" +
                "31.1.2026,,11:12:0.686,12418", myFixture.getFile().getText());
    }

    public void testAddingRowBeforeFirstThenColumnAfterFirstWithCrLfPreservesRanges() {
        editor.dispose();
        myFixture.configureByText(CsvFileType.INSTANCE,
                "Date,Time,Virtual Memory Committed [MB]\r\n" +
                "31.1.2026,11:11:56.690,12447\r\n" +
                "31.1.2026,11:12:0.686,12418\r\n");
        editor = new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile());
        editor.selectNotify();
        editor.getTable().setRowSelectionInterval(0, 0);
        editor.getTable().setColumnSelectionInterval(0, 0);

        editor.tableEditorActions.addRowBefore.actionPerformed(null);
        editor.tableEditorActions.addColumnAfter.actionPerformed(null);

        assertEquals(",\n" +
                "Date,,Time,Virtual Memory Committed [MB]\n" +
                "31.1.2026,,11:11:56.690,12447\n" +
                "31.1.2026,,11:12:0.686,12418\n", myFixture.getFile().getText());
    }

    public void testAddingRowBeforeFirstThenColumnAfterFirstOnCrLfFilePreservesValues() {
        editor.dispose();
        myFixture.configureByFiles("TableEditorFile.csv");
        editor = new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile());
        editor.selectNotify();
        editor.getTable().setRowSelectionInterval(0, 0);
        editor.getTable().setColumnSelectionInterval(0, 0);

        editor.tableEditorActions.addRowBefore.actionPerformed(null);
        editor.tableEditorActions.addColumnAfter.actionPerformed(null);

        assertEquals(",\n" +
                "Header1,, header 2\n" +
                " \"this is column \"\"Header1\"\"\"  ,,this is column header 2\n" +
                " just another line with leading and trailing whitespaces  ,, \"  and one more value  \"\n",
                myFixture.getFile().getText());
    }
}
