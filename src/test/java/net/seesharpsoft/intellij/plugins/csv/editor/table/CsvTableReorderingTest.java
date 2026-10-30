package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class CsvTableReorderingTest extends CsvBasePlatformTestCase implements PsiFileHolder {
    private CsvTableModel model;

    @Override
    public PsiFile getPsiFile() {
        return myFixture.getFile();
    }

    @Override
    public void dispose() {
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        CsvEditorSettings.getInstance().setAutoDetectValueSeparator(false);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (model != null) model.dispose();
            CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        } finally {
            super.tearDown();
        }
    }

    private void configure(String text) {
        if (model != null) model.dispose();
        myFixture.configureByText(getTestName(false) + ".csv", text);
        model = new CsvTableModelBase<>(this);
    }

    private void check(String text) {
        assertEquals(text, myFixture.getFile().getText());
        assertFalse(model.hasErrors());
        PsiTestUtil.checkFileStructure(myFixture.getFile());
    }

    public void testRowsPreserveQuotesWhitespaceCommentsAndTerminalNewline() {
        configure("a,b\n  \"multi\nline\"  , \"a,\"\"b\"\"\"\n# comment\n");
        model.reorderRows(new int[]{2, 0, 1, 3});
        check("# comment\na,b\n  \"multi\nline\"  , \"a,\"\"b\"\"\"\n");
        assertTrue(model.isCommentRow(0));
        assertEquals(3, model.getMovableRowCount());
    }

    public void testRowsWithoutTerminalNewlineAndWithBlankRows() {
        configure("a,b\n\nc,d");
        model.reorderRows(new int[]{2, 1, 0});
        check("c,d\n\na,b");
        assertEquals(3, model.getMovableRowCount());
    }

    public void testReadsStayValidWhileModelNotificationsAreSuspended() {
        configure("first,1\nsecond,2\nthird,3");
        assertEquals("third", model.getValue(2, 0));
        model.suspend();
        try {
            model.reorderRows(new int[]{2, 0, 1});
            assertEquals("second", model.getValue(2, 0));
            assertEquals("third", model.getValue(0, 0));
        } finally {
            model.resume();
        }
        check("third,3\nfirst,1\nsecond,2");
    }

    public void testColumnsPreserveRawFieldTextAndComments() {
        configure("a,b,c\n  \"multi\nline\"  , \"a,\"\"b\"\"\" ,last\n# comment\n");
        model.reorderColumns(new int[]{2, 0, 1});
        check("c,a,b\nlast,  \"multi\nline\"  , \"a,\"\"b\"\"\" \n# comment\n");
        assertEquals("multi\nline", model.getValue(1, 1));
    }

    public void testColumnsMaterializeMissingCellsOnlyWhereNeeded() {
        configure("a,b,c,d\n1,2\n\n3,4,5");
        model.reorderColumns(new int[]{2, 3, 0, 1});
        check("c,d,a,b\n,,1,2\n,,\n5,,3,4");
        assertEquals("1", model.getValue(1, 2));
        assertEquals("5", model.getValue(3, 0));
    }

    public void testMovingMissingColumnToEndDoesNotPadShortRows() {
        configure("a,b,c\n1,2\n");
        model.reorderColumns(new int[]{1, 0, 2});
        check("b,a,c\n2,1\n");
    }

    public void testMovingCommentLikeValueToFirstColumnQuotesIt() {
        configure("a, # not a comment,b\n# actual comment\n");
        model.reorderColumns(new int[]{1, 0, 2});
        check("\" # not a comment\",a,b\n# actual comment\n");
        assertFalse(model.isCommentRow(0));
        assertEquals(" # not a comment", model.getValue(0, 0));
    }

    public void testTsvColumns() {
        myFixture.configureByText("data.tsv", "a\tb\tc\n1\t\"two\tvalues\"\t3\n");
        model = new CsvTableModelBase<>(this);
        model.reorderColumns(new int[]{2, 0, 1});
        check("c\ta\tb\n3\t1\t\"two\tvalues\"\n");
    }

    public void testCustomSeparatorAndEscapeCharacter() {
        CsvEditorSettings.getInstance().setDefaultValueSeparator(new CsvValueSeparator("$~"));
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
        configure("a$~\"b\\\"c\"$~d\n1$~2$~3");
        model.reorderColumns(new int[]{1, 2, 0});
        check("\"b\\\"c\"$~d$~a\n2$~3$~1");
    }

    public void testIdentityOrderDoesNotChangeDocument() {
        configure("a,b\n1,2\n");
        AtomicInteger changes = countChanges();
        model.reorderColumns(new int[]{0, 1});
        model.reorderRows(new int[]{0, 1, 2});
        assertEquals(0, changes.get());
        check("a,b\n1,2\n");
    }

    public void testLexerAndModelAgreeOnEmptyAndCommentOnlyFiles() {
        for (String text : new String[]{"", "\n", "\n\n", "# comment", "# comment\n", "# comment\n\n", "a,b\n\n"}) {
            configure(text);
            model.reorderColumns(java.util.stream.IntStream.range(0, model.getColumnCount()).toArray());
            model.reorderRows(java.util.stream.IntStream.range(0, model.getRowCount()).toArray());
            check(text);
        }
    }

    public void testInvalidOrdersDoNotChangeDocument() {
        configure("a,b\n1,2\n");
        assertThrows(IllegalArgumentException.class, () -> model.reorderColumns(new int[]{1, 1}));
        assertThrows(IllegalArgumentException.class, () -> model.reorderColumns(new int[]{0}));
        assertThrows(IllegalArgumentException.class, () -> model.reorderRows(new int[]{2, 1, 0}));
        assertThrows(IllegalArgumentException.class, () -> model.reorderRows(new int[]{0, 1, 3}));
        check("a,b\n1,2\n");
    }

    public void testCommentOnlyColumnReorderKeepsTheSpanningColumn() {
        configure("# first\n# second");
        assertEquals(1, model.getColumnCount());
        model.reorderColumns(new int[]{0});
        check("# first\n# second");
        assertThrows(IllegalArgumentException.class, () -> model.reorderColumns(new int[]{1, 0}));
        check("# first\n# second");
    }

    public void testColumnReorderingIsOneDocumentEventAndUndoStep() {
        String original = "a,b,c\n1,2,3\n4,5,6\n";
        configure(original);
        AtomicInteger changes = countChanges();
        model.reorderColumns(new int[]{2, 0, 1});
        assertEquals(1, changes.get());
        check("c,a,b\n3,1,2\n6,4,5\n");
        undoAndRedo(original, myFixture.getFile().getText());
    }

    public void testRowReorderingIsOneDocumentEventAndUndoStep() {
        String original = "a,b\n1,2\n3,4\n";
        configure(original);
        AtomicInteger changes = countChanges();
        model.reorderRows(new int[]{2, 0, 1, 3});
        assertEquals(1, changes.get());
        check("3,4\na,b\n1,2\n");
        undoAndRedo(original, myFixture.getFile().getText());
    }

    private AtomicInteger countChanges() {
        AtomicInteger changes = new AtomicInteger();
        myFixture.getEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                changes.incrementAndGet();
            }
        }, getTestRootDisposable());
        return changes;
    }

    private void undoAndRedo(String original, String reordered) {
        UndoManager undo = UndoManager.getInstance(getProject());
        TextEditor textEditor = TextEditorProvider.getInstance().getTextEditor(myFixture.getEditor());
        undo.undo(textEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        check(original);
        undo.redo(textEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        check(reordered);
    }
}
