package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.impl.UndoManagerImpl;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.CurrentEditorProvider;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.util.ManualUpdateScheduler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvTableClipboardTest extends CsvTableEditorSwingTestBase {
    @Override
    protected void initializeEditorSettings(CsvEditorSettings settings) {
        super.initializeEditorSettings(settings);
        settings.setAutoDetectValueSeparator(false);
    }

    protected void configure(String text) {
        fileEditor.dispose();
        myFixture.configureByText(getTestName(false) + ".csv", text);
        fileEditor = createEditor();
        fileEditor.selectNotify();
        fileEditor.getTable().changeSelection(0, 0, false, false);
    }

    private boolean paste(String text) {
        CsvTable table = fileEditor.getTable();
        return table.getTransferHandler().importData(new TransferHandler.TransferSupport(table, new StringSelection(text)));
    }

    private Transferable copy() {
        Clipboard clipboard = new Clipboard("CSV test");
        CsvTable table = fileEditor.getTable();
        table.getTransferHandler().exportToClipboard(table, clipboard, TransferHandler.COPY);
        return clipboard.getContents(null);
    }

    private void useTableAsCurrentEditor() {
        UndoManagerImpl undo = (UndoManagerImpl) UndoManager.getInstance(getProject());
        undo.setOverriddenEditorProvider(new CurrentEditorProvider() {
            @Override
            public FileEditor getCurrentEditor(Project project) {
                return fileEditor;
            }
        });
        Disposer.register(getTestRootDisposable(), () -> undo.setOverriddenEditorProvider(null));
    }

    public void testCopyAlreadyProvidesTextAndHtml() throws Exception {
        configure("a,b\n1,2");
        fileEditor.getTable().setRowSelectionInterval(0, 1);
        fileEditor.getTable().setColumnSelectionInterval(0, 1);
        Transferable copied = copy();
        assertEquals("a,b\n1,2", copied.getTransferData(DataFlavor.stringFlavor));
        DataFlavor html = new DataFlavor("text/html;class=java.lang.String");
        assertTrue(copied.isDataFlavorSupported(html));
        assertTrue(((String) copied.getTransferData(html)).contains("<table>"));
    }

    public void testCopiedPlainTextIsQuotedCsvInEveryTextFlavor() throws Exception {
        configure("\"a\u00e4,b\",\"a\"\"b\",\"multi\nline\",");
        fileEditor.getTable().setColumnSelectionInterval(0, 3);
        Transferable copied = copy();
        String expected = "\"a\u00e4,b\",\"a\"\"b\",\"multi\nline\",";
        assertEquals(expected, copied.getTransferData(DataFlavor.stringFlavor));
        for (DataFlavor flavor : copied.getTransferDataFlavors()) {
            if (String.class.equals(flavor.getRepresentationClass()) && !flavor.isMimeTypeEqual("text/html")) {
                assertEquals(flavor.toString(), expected, copied.getTransferData(flavor));
            }
            if (!flavor.isMimeTypeEqual("text/plain")) continue;
            try (var reader = flavor.getReaderForText(copied)) {
                var text = new java.io.StringWriter();
                reader.transferTo(text);
                assertEquals(flavor.toString(), expected, text.toString());
            }
        }
    }

    public void testCopyUsesFileSeparatorAndEscapeCharacter() throws Exception {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        settings.setDefaultValueSeparator(new CsvValueSeparator("$~"));
        settings.setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
        configure("\"x$~y\"$~\"a\\\"b\"");
        fileEditor.getTable().setColumnSelectionInterval(0, 1);
        assertEquals("\"x$~y\"$~\"a\\\"b\"", copy().getTransferData(DataFlavor.stringFlavor));
    }

    public void testCopyUsesTabSeparatorAndQuotesEmbeddedTabs() throws Exception {
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.TAB);
        configure("a\t\"b\tc\"\t");
        fileEditor.getTable().setColumnSelectionInterval(0, 2);
        assertEquals("a\t\"b\tc\"\t", copy().getTransferData(DataFlavor.stringFlavor));
    }

    public void testCopyRespectsEnforcedQuoting() throws Exception {
        CsvEditorSettings.getInstance().setQuotingEnforced(true);
        configure("a,b");
        fileEditor.getTable().setColumnSelectionInterval(0, 1);
        assertEquals("\"a\",\"b\"", copy().getTransferData(DataFlavor.stringFlavor));
    }

    public void testCopyQuotesCommentLikeFirstCell() throws Exception {
        CsvEditorSettings.getInstance().setCommentIndicator("#");
        configure("a,#value");
        fileEditor.getTable().changeSelection(0, 1, false, false);
        assertEquals("\"#value\"", copy().getTransferData(DataFlavor.stringFlavor));
    }

    public void testPasteOverwritesWithoutInsertingRowsOrColumns() {
        configure("a,b,c\n1,2,3\n4,5,6");
        fileEditor.getTable().changeSelection(1, 1, false, false);
        assertTrue(paste("x,y\nz,w"));
        assertEquals("a,b,c\n1,x,y\n4,z,w", myFixture.getFile().getText());
        assertEquals(3, getTableModel().getRowCount());
        assertEquals(3, getTableModel().getColumnCount());
        assertFalse(fileEditor.getTable().isEditing());
    }

    public void testPasteStartsAtLeadCellRatherThanFirstSelectedCell() {
        configure("a,b,c\n1,2,3\n4,5,6");
        fileEditor.getTable().changeSelection(0, 0, false, false);
        fileEditor.getTable().changeSelection(1, 1, false, true);
        assertTrue(paste("x"));
        assertEquals("a,b,c\n1,x,3\n4,5,6", myFixture.getFile().getText());
    }

    public void testPasteGrowsOnlyPastExistingBounds() {
        configure("a,b\n1,2");
        fileEditor.getTable().changeSelection(1, 1, false, false);
        assertTrue(paste("x,y\nz,w"));
        assertEquals("a,b\n1,x,y\n,z,w", myFixture.getFile().getText());
        assertEquals(3, getTableModel().getRowCount());
        assertEquals(3, getTableModel().getColumnCount());
    }

    public void testPasteIntoRaggedRowsPreservesUntouchedValues() {
        configure("a,b,c,d\n1\n4,5,6,7");
        fileEditor.getTable().changeSelection(1, 2, false, false);
        assertTrue(paste("x,y\nz"));
        assertEquals("a,b,c,d\n1,,x,y\n4,5,z,7", myFixture.getFile().getText());
    }

    public void testPasteQuotedCsvAndWindowsLineEndings() {
        configure("a,b,c\n1,2,3");
        fileEditor.getTable().changeSelection(1, 0, false, false);
        assertTrue(paste("\"x,y\",\"two \"\"quotes\"\"\",\"multi\r\nline\"\r\n"));
        assertEquals("a,b,c\n\"x,y\",\"two \"\"quotes\"\"\",\"multi\nline\"", myFixture.getFile().getText());
        assertEquals(2, getTableModel().getRowCount());
    }

    public void testPasteEmptyFieldsAndBlankRows() {
        configure("a,b,c\n1,2,3\n4,5,6");
        assertTrue(paste("x,,\n\nz,,"));
        assertEquals("x,,\n,2,3\nz,,", myFixture.getFile().getText());
    }

    public void testPasteEmptyTextClearsOneCell() {
        configure("a,b");
        assertTrue(paste(""));
        assertEquals(",b", myFixture.getFile().getText());
    }

    public void testPasteIntoEmptyFile() {
        configure("");
        assertTrue(paste("a,b\n1,2"));
        assertEquals("a,b\n1,2", myFixture.getFile().getText());
    }

    public void testPasteUsesExistingTrailingBlankRow() {
        configure("a,b\n");
        fileEditor.getTable().changeSelection(1, 0, false, false);
        assertTrue(paste("1,2\n3,4\n"));
        assertEquals("a,b\n1,2\n3,4", myFixture.getFile().getText());
    }

    public void testPasteSpreadsheetTsv() {
        configure("a,b\n1,2");
        assertTrue(paste("x\ty\nz\tw"));
        assertEquals("x,y\nz,w", myFixture.getFile().getText());
    }

    public void testSpreadsheetTsvKeepsCommasInsideCells() {
        configure("a,b\n1,2");
        assertTrue(paste("x,y\tz\np\tq,r"));
        assertEquals("\"x,y\",z\np,\"q,r\"", myFixture.getFile().getText());
    }

    public void testQuotedTabDoesNotChangeCsvSeparator() {
        configure("a,b");
        assertTrue(paste("\"x\ty\",z"));
        assertEquals("x\ty,z", myFixture.getFile().getText());
    }

    public void testPasteUsesFileSeparatorAndEscapeSettings() {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        settings.setAutoDetectValueSeparator(false);
        settings.setDefaultValueSeparator(new CsvValueSeparator("$~"));
        settings.setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
        configure("a$~b\n1$~2");
        assertTrue(paste("\"x$~y\"$~\"a\\\"b\""));
        assertEquals("\"x$~y\"$~\"a\\\"b\"\n1$~2", myFixture.getFile().getText());
    }

    public void testCopyPasteRoundTripKeepsQuotesTabsAndNewlines() {
        configure("\"a,b\",\"a\"\"b\",\"x\ty\",\"multi\nline\"\n1,2,3,4");
        fileEditor.getTable().setColumnSelectionInterval(0, 3);
        Transferable copied = copy();
        fileEditor.getTable().changeSelection(1, 0, false, false);
        assertTrue(fileEditor.getTable().getTransferHandler().importData(fileEditor.getTable(), copied));
        for (int column = 0; column < 4; column++) {
            assertEquals(getTableModel().getValue(0, column), getTableModel().getValue(1, column));
        }
    }

    public void testCellEditorPasteStaysWithinCell() {
        configure("a,b\n1,2");
        CsvTable table = fileEditor.getTable();
        assertTrue(table.editCellAt(1, 0));
        JTextArea text = (JTextArea) ((JScrollPane) table.getEditorComponent()).getViewport().getView();
        text.selectAll();
        assertTrue(text.getTransferHandler().importData(text, new StringSelection("x,y\nz,w")));
        assertTrue(table.getCellEditor().stopCellEditing());
        assertEquals("a,b\n\"x,y\nz,w\",2", myFixture.getFile().getText());
    }

    public void testReadOnlyFileAllowsCellAndTextCopyButNotPaste() throws Exception {
        configure("a,b\n1,2");
        WriteAction.run(() -> fileEditor.getFile().setWritable(false));
        try {
            fileEditor.updateEditorLayout();
            CsvTable table = fileEditor.getTable();
            assertTrue(table.isEnabled());
            table.changeSelection(1, 0, false, false);
            assertEquals("1", copy().getTransferData(DataFlavor.stringFlavor));
            assertFalse(paste("changed"));
            assertTrue(table.editCellAt(1, 0));
            JTextArea text = (JTextArea) ((JScrollPane) table.getEditorComponent()).getViewport().getView();
            assertFalse(text.isEditable());
            text.selectAll();
            assertEquals("1", text.getSelectedText());
            Clipboard clipboard = new Clipboard("Read-only cell text");
            text.getTransferHandler().exportToClipboard(text, clipboard, TransferHandler.COPY);
            assertEquals("1", clipboard.getData(DataFlavor.stringFlavor));
            table.getCellEditor().stopCellEditing();
            assertEquals("a,b\n1,2", myFixture.getFile().getText());
        } finally {
            WriteAction.run(() -> fileEditor.getFile().setWritable(true));
        }
    }

    public void testSyntaxErrorsStillAllowCopyButNotPaste() throws Exception {
        configure("a,b\n\"unterminated");
        assertTrue(fileEditor.getTable().isEnabled());
        assertFalse(fileEditor.isEditable());
        assertEquals("a", copy().getTransferData(DataFlavor.stringFlavor));
        assertFalse(paste("changed"));
        assertEquals("a,b\n\"unterminated", myFixture.getFile().getText());
    }

    public void testReadOnlyCellViewerCannotCommitChanges() {
        configure("a,b");
        fileEditor.setEditable(false);
        CsvTable table = fileEditor.getTable();
        assertTrue(table.editCellAt(0, 0));
        JTextArea text = (JTextArea) ((JScrollPane) table.getEditorComponent()).getViewport().getView();
        text.setText("changed programmatically");
        table.getCellEditor().stopCellEditing();
        assertEquals("a,b", myFixture.getFile().getText());
    }

    public void testReadOnlyKeyboardActionsDoNotQueueMutations() {
        configure("a,b\n1,2");
        fileEditor.setEditable(false);
        CsvTable table = fileEditor.getTable();
        for (int modifiers : new int[]{0, KeyEvent.CTRL_DOWN_MASK, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK}) {
            fileEditor.tableEditorKeyListener.keyReleased(
                    new KeyEvent(table, KeyEvent.KEY_RELEASED, 0, modifiers, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED));
        }
        fileEditor.tableEditorActions.addRowBefore.actionPerformed(null);
        fileEditor.tableEditorActions.addColumnAfter.actionPerformed(null);
        assertEquals("a,b\n1,2", myFixture.getFile().getText());
        fileEditor.setEditable(true);
        getTableModel().setValue("x", 1, 0);
        assertEquals("a,b\nx,2", myFixture.getFile().getText());
    }

    public void testReadOnlyDocumentCannotBePastedInto() {
        configure("a,b");
        fileEditor.getDocument().setReadOnly(true);
        try {
            assertFalse(paste("x,y"));
            assertTrue(fileEditor.getTable().editCellAt(0, 0));
            JTextArea text = (JTextArea) ((JScrollPane) fileEditor.getTable().getEditorComponent()).getViewport().getView();
            assertFalse(text.isEditable());
            fileEditor.getTable().getCellEditor().stopCellEditing();
            assertEquals("a,b", myFixture.getFile().getText());
        } finally {
            fileEditor.getDocument().setReadOnly(false);
        }
    }

    public void testReadOnlyColumnHeaderCopy() throws Exception {
        configure("a,b\n1,2");
        fileEditor.setEditable(false);
        fileEditor.selectColumn(1, false);
        assertEquals("b\n2", copy().getTransferData(DataFlavor.stringFlavor));
    }

    public void testPastePlainTextReaderFlavor() throws Exception {
        configure("a,b");
        DataFlavor plainText = new DataFlavor("text/plain;class=java.io.Reader");
        Transferable clipboard = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{plainText};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return plainText.equals(flavor);
            }

            @Override
            public @NotNull Object getTransferData(DataFlavor flavor) {
                return new StringReader("x,y");
            }
        };
        assertTrue(fileEditor.getTable().getTransferHandler().importData(fileEditor.getTable(), clipboard));
        assertEquals("x,y", myFixture.getFile().getText());
    }

    public void testCommentRowsAcceptSingleValuesWithoutLosingTheirMarker() {
        configure("a,b\n# old\n1,2");
        fileEditor.getTable().changeSelection(1, 0, false, false);
        assertTrue(paste(" changed"));
        assertEquals("a,b\n# changed\n1,2", myFixture.getFile().getText());
    }

    public void testCopyCommentRowIncludesMarkerOnlyOnce() throws Exception {
        configure("a,b,c\n# note, \"text\"\n1,2,3");
        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(0, 2);
        assertEquals("# note, \"text\"", copy().getTransferData(DataFlavor.stringFlavor));
    }

    public void testCopyCommentFromNonFirstColumnInReadOnlyFile() throws Exception {
        configure("a,b,c\n# note\n1,2,3");
        fileEditor.setEditable(false);
        fileEditor.getTable().changeSelection(1, 2, false, false);
        assertEquals("# note", copy().getTransferData(DataFlavor.stringFlavor));
    }

    public void testCopyCommentHtmlSpansSelectionWithoutDuplicates() throws Exception {
        configure("a,b,c\n# note <tag> & \"text\"\n1,2,3");
        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(0, 2);
        Transferable copied = copy();
        int htmlFlavors = 0;
        for (DataFlavor flavor : copied.getTransferDataFlavors()) {
            if (!flavor.isMimeTypeEqual("text/html")) continue;
            htmlFlavors++;
            try (var reader = flavor.getReaderForText(copied)) {
                var text = new java.io.StringWriter();
                reader.transferTo(text);
                String html = text.toString();
                assertTrue(html.contains("<td colspan=\"3\">"));
                assertTrue(html.contains("# note &lt;tag&gt; &amp;"));
                assertEquals(1, html.split("# note", -1).length - 1);
                assertEquals(1, html.split("<td", -1).length - 1);
            }
        }
        assertTrue(htmlFlavors > 0);
    }

    public void testCopyPasteCommentRowRemainsAComment() {
        configure("a,b,c\n# note, \"text\"\n1,2,3");
        fileEditor.getTable().setRowSelectionInterval(1, 1);
        fileEditor.getTable().setColumnSelectionInterval(0, 2);
        Transferable copied = copy();
        fileEditor.getTable().changeSelection(2, 0, false, false);
        assertTrue(fileEditor.getTable().getTransferHandler().importData(fileEditor.getTable(), copied));
        assertEquals("a,b,c\n# note, \"text\"\n# note, \"text\"", myFixture.getFile().getText());
        assertTrue(getTableModel().isCommentRow(2));
    }

    public void testPasteCommentTextIntoCommentRowDoesNotDuplicateMarker() {
        configure("a,b\n# old\n1,2");
        fileEditor.getTable().changeSelection(1, 0, false, false);
        assertTrue(paste("# replacement"));
        assertEquals("a,b\n# replacement\n1,2", myFixture.getFile().getText());
    }

    public void testPasteCsvWithCommentRowsPreservesComments() {
        configure("a,b\n1,2\n3,4");
        assertTrue(paste("x,y\n# note, \"text\"\nz,w"));
        assertEquals("x,y\n# note, \"text\"\nz,w", myFixture.getFile().getText());
        assertTrue(getTableModel().isCommentRow(1));
    }

    public void testMixedCopyPastePreservesCustomCommentsAndGrowsRows() throws Exception {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        settings.setCommentIndicator("//");
        settings.setDefaultValueSeparator(new CsvValueSeparator("$~"));
        String selection = "a$~b\n// note$~\t\"text\"\nx$~y";
        configure(selection);
        fileEditor.getTable().setRowSelectionInterval(0, 2);
        fileEditor.getTable().setColumnSelectionInterval(0, 1);
        Transferable copied = copy();
        assertEquals(selection, copied.getTransferData(DataFlavor.stringFlavor));
        fileEditor.getTable().changeSelection(2, 0, false, false);
        assertTrue(fileEditor.getTable().getTransferHandler().importData(fileEditor.getTable(), copied));
        assertEquals("a$~b\n// note$~\t\"text\"\n" + selection, myFixture.getFile().getText());
        assertTrue(getTableModel().isCommentRow(3));
        assertEquals(2, getTableModel().getColumnCount());
    }

    public void testPastedCommentsDoNotParseTabsDelimitersOrQuotesAsValues() {
        configure("a,b\n1,2\n3,4");
        assertTrue(paste("x,y\n## note,\twith,an,\"unmatched quote\nz,w\n"));
        assertEquals("x,y\n## note,\twith,an,\"unmatched quote\nz,w", myFixture.getFile().getText());
        assertTrue(getTableModel().isCommentRow(1));
        assertEquals(2, getTableModel().getColumnCount());
        assertEquals(3, getTableModel().getRowCount());
    }

    public void testPastedCommentOutsideFirstColumnIsRejectedAtomically() {
        configure("a,b,c\n1,2,3");
        fileEditor.getTable().changeSelection(0, 1, false, false);
        assertFalse(paste("x,y\n# note"));
        assertEquals("a,b,c\n1,2,3", myFixture.getFile().getText());
    }

    public void testPastedHashRemainsAValueWhenCommentsAreDisabled() {
        CsvEditorSettings.getInstance().setCommentIndicator("");
        configure("a,b");
        assertTrue(paste("# value,other"));
        assertEquals("# value,other", myFixture.getFile().getText());
        assertFalse(getTableModel().isCommentRow(0));
    }

    public void testPasteLeavingOnlyCommentsStillAllowsSelectingAndCopying() throws Exception {
        configure("a,b");
        assertTrue(paste("# only comment"));
        assertEquals("# only comment", myFixture.getFile().getText());
        assertTrue(getTableModel().isCommentRow(0));
        assertEquals(1, getTableModel().getColumnCount());
        fileEditor.getTable().changeSelection(0, 0, false, false);
        assertEquals("# only comment", copy().getTransferData(DataFlavor.stringFlavor));
    }

    public void testCopyFileContainingOnlyEmptyAndNestedComments() throws Exception {
        configure("#\n## nested");
        fileEditor.getTable().setRowSelectionInterval(0, 1);
        assertEquals("#\n## nested", copy().getTransferData(DataFlavor.stringFlavor));
    }

    public void testPasteAcrossCommentRowIsAtomic() {
        configure("a,b\n# old\n1,2");
        assertFalse(paste("x,y\nz,w"));
        assertEquals("a,b\n# old\n1,2", myFixture.getFile().getText());
    }

    public void testPastedCommentLikeValueStaysAValue() {
        configure("a,b");
        assertTrue(paste("\"# value\",other"));
        assertEquals("\"# value\",other", myFixture.getFile().getText());
        assertFalse(getTableModel().isCommentRow(0));
    }

    public void testPastePreservesUnchangedFieldFormatting() {
        configure("\"a\" , b ,\"c\"  \n1,2,3");
        fileEditor.getTable().changeSelection(0, 1, false, false);
        assertTrue(paste("x"));
        assertEquals("\"a\" ,x,\"c\"  \n1,2,3", myFixture.getFile().getText());
    }

    public void testPasteIsOneUndoableEdit() {
        configure("a,b\n1,2");
        var editor = TextEditorProvider.getInstance().getTextEditor(myFixture.getEditor());
        var undo = UndoManager.getInstance(getProject());
        fileEditor.getTable().changeSelection(1, 1, false, false);
        assertTrue(paste("x,y\nz,w"));
        undo.undo(editor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals("a,b\n1,2", myFixture.getFile().getText());
        undo.redo(editor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals("a,b\n1,x,y\n,z,w", myFixture.getFile().getText());
    }

    public void testPasteIsUndoneOnFirstUndoFromTableEditor() {
        configure("a,b\n1,2");
        useTableAsCurrentEditor();
        fileEditor.getTable().changeSelection(1, 1, false, false);
        CommandProcessor.getInstance().executeCommand(getProject(), () -> assertTrue(paste("x,y\nz,w")), "Paste", null);
        var undo = UndoManager.getInstance(getProject());
        assertTrue(undo.isUndoAvailable(fileEditor));
        undo.undo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals("a,b\n1,2", myFixture.getFile().getText());
        assertEquals("2", getTableModel().getValue(1, 1));
        undo.redo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals("a,b\n1,x,y\n,z,w", myFixture.getFile().getText());
    }

    public void testPasteIsUndoneOnFirstUndoAfterDelayedRefresh() {
        ManualUpdateScheduler scheduler = new ManualUpdateScheduler();
        updateScheduler = scheduler;
        configure("a,b\n1,2");
        scheduler.advanceBy(50);
        useTableAsCurrentEditor();
        fileEditor.getTable().changeSelection(1, 1, false, false);
        CommandProcessor.getInstance().executeCommand(getProject(), () -> assertTrue(paste("x,y\nz,w")), "Paste", null);
        scheduler.advanceBy(50);
        UndoManager.getInstance(getProject()).undo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        scheduler.advanceBy(50);
        assertEquals("a,b\n1,2", myFixture.getFile().getText());
        assertEquals(2, getTableModel().getRowCount());
        assertEquals(2, getTableModel().getColumnCount());
        assertEquals("2", getTableModel().getValue(1, 1));
    }

    public void testPasteRefreshesOnceWithProductionDelay() {
        ManualUpdateScheduler scheduler = new ManualUpdateScheduler();
        updateScheduler = scheduler;
        configure("a,b\n1,2");
        scheduler.advanceBy(50);
        AtomicInteger updates = new AtomicInteger();
        fileEditor.getTable().getModel().addTableModelListener(event -> updates.incrementAndGet());
        assertTrue(paste("x,y\nz,w\np,q"));
        scheduler.advanceBy(50);
        assertEquals(1, updates.get());
        assertEquals(3, getTableModel().getRowCount());
    }

    public void testPasteSurvivesLookAndFeelRefreshAndFrozenColumns() {
        configure("a,b,c\n1,2,3");
        fileEditor.setFrozenColumnCount(1);
        fileEditor.getTable().updateUI();
        fileEditor.getTable().changeSelection(1, 1, false, false);
        assertTrue(paste("x,y"));
        assertEquals("a,b,c\n1,x,y", myFixture.getFile().getText());
    }

    public void testPasteWithoutSelectionIsUnavailable() {
        configure("a,b");
        fileEditor.getTable().clearSelection();
        assertFalse(paste("x,y"));
        assertEquals("a,b", myFixture.getFile().getText());
    }

    public void testMalformedClipboardIsRejectedWithoutChangingFile() {
        configure("a,b");
        assertFalse(paste("\"unterminated"));
        assertEquals("a,b", myFixture.getFile().getText());
    }

    public void testFailedEditorInitializationCancelsPendingModelUpdates() {
        ManualUpdateScheduler scheduler = new ManualUpdateScheduler();
        CsvTableModelSwing[] model = new CsvTableModelSwing[1];
        try {
            assertThrows(IllegalStateException.class, () -> new CsvTableEditorSwing(getProject(), fileEditor.getFile()) {
                @Override
                protected CsvTableModelSwing createTableModel() {
                    model[0] = new CsvTableModelSwing(this, scheduler);
                    model[0].notifyUpdate();
                    return model[0];
                }

                @Override
                protected CsvMultiLineCellRenderer createCellRenderer(boolean comment) {
                    throw new IllegalStateException("Simulated editor initialization failure");
                }
            });
            scheduler.advanceBy(50);
            assertTrue(model[0].isDisposed());
            assertEquals(0, scheduler.pendingCount());
        } finally {
            if (model[0] != null) model[0].dispose();
        }
    }
}
