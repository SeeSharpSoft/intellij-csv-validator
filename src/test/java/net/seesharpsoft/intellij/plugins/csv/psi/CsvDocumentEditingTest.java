package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.table.CsvTableModelBase;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;

public class CsvDocumentEditingTest extends CsvBasePlatformTestCase implements PsiFileHolder {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        resetFileAttributes();
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        CsvEditorSettings.getInstance().setAutoDetectValueSeparator(false);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            resetFileAttributes();
            CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        } finally {
            super.tearDown();
        }
    }

    private void resetFileAttributes() {
        CsvFileAttributes attributes = getProject().getServiceIfCreated(CsvFileAttributes.class);
        if (attributes != null) attributes.reset();
    }

    @Override
    public PsiFile getPsiFile() {
        return myFixture.getFile();
    }

    @Override
    public void dispose() {
    }

    public void testSelectedFieldsAreQuotedOnceAndTableValuesStaySynchronized() {
        String original = "a,,c\n# comment\n\"multi\nline\",tail,last";
        myFixture.configureByText(CsvFileType.INSTANCE, original);
        CsvTableModelBase<CsvDocumentEditingTest> model = new CsvTableModelBase<>(this);
        try {
            CsvField first = field(0, 0);
            CsvField empty = field(0, 1);
            CsvField tail = field(2, 1);
            AtomicInteger changes = countChanges();
            CsvPsiTreeUpdater.edit(getPsiFile(),
                    updater -> updater.setQuoted(List.of(tail, empty, first, tail), true));
            String quoted = "\"a\",\"\",c\n# comment\n\"multi\nline\",\"tail\",last";
            assertEquals(quoted, getPsiFile().getText());
            assertEquals(1, changes.get());
            assertEquals("a", model.getValue(0, 0));
            assertEquals("", model.getValue(0, 1));
            assertEquals("multi\nline", model.getValue(2, 0));
            assertTrue(model.isCommentRow(1));
            PsiTestUtil.checkFileStructure(getPsiFile());
            undoAndRedo(original, quoted);
        } finally {
            model.dispose();
        }
    }

    public void testQuoteAndUnquoteAllPreserveEscapedFieldsAndComments() {
        myFixture.configureByText(CsvFileType.INSTANCE,
                "a,,c\n# comment\n\"multi\nline\",\"a,\"\"b\"\"\",tail");
        AtomicInteger changes = countChanges();
        CsvPsiTreeUpdater.edit(getPsiFile(), updater -> updater.setQuoted(true));
        assertEquals("\"a\",\"\",\"c\"\n# comment\n\"multi\nline\",\"a,\"\"b\"\"\",\"tail\"",
                getPsiFile().getText());
        assertEquals(1, changes.get());
        CsvPsiTreeUpdater.edit(getPsiFile(), updater -> updater.setQuoted(false));
        assertEquals("a,,c\n# comment\n\"multi\nline\",\"a,\"\"b\"\"\",tail", getPsiFile().getText());
        assertEquals(2, changes.get());
        assertFalse(CsvPsiTreeUpdater.canUnquote(field(2, 0)));
        assertFalse(CsvPsiTreeUpdater.canUnquote(field(2, 1)));
        PsiTestUtil.checkFileStructure(getPsiFile());
    }

    public void testSelectedUnquotingUsesCustomSeparatorAndEscapeRules() {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        CsvValueSeparator previousSeparator = settings.getDefaultValueSeparator();
        CsvEscapeCharacter previousEscape = settings.getDefaultEscapeCharacter();
        boolean previousDetection = settings.isAutoDetectValueSeparator();
        try {
            settings.setAutoDetectValueSeparator(false);
            settings.setDefaultValueSeparator(new CsvValueSeparator("$~"));
            settings.setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
            myFixture.configureByText(CsvFileType.INSTANCE, "\"plain\"$~\"b\\\"c\"$~\"two$~values\"$~\"last\"\n");
            CsvPsiTreeUpdater.edit(getPsiFile(), updater ->
                    updater.setQuoted(List.of(field(0, 3), field(0, 2), field(0, 1), field(0, 0)), false));
            assertEquals("plain$~\"b\\\"c\"$~\"two$~values\"$~last\n", getPsiFile().getText());
            PsiTestUtil.checkFileStructure(getPsiFile());
        } finally {
            settings.setDefaultValueSeparator(previousSeparator);
            settings.setDefaultEscapeCharacter(previousEscape);
            settings.setAutoDetectValueSeparator(previousDetection);
        }
    }

    public void testAlreadyQuotedFieldsAndUnsafeUnquotingDoNotChangeDocument() {
        myFixture.configureByText(CsvFileType.INSTANCE, "\"a,b\",\"multi\nline\",\"escaped\"\"quote\"");
        AtomicInteger changes = countChanges();
        CsvPsiTreeUpdater.edit(getPsiFile(), updater -> updater.setQuoted(true));
        CsvPsiTreeUpdater.edit(getPsiFile(), updater -> updater.setQuoted(false));
        CsvPsiTreeUpdater.edit(getPsiFile(), updater -> updater.setQuoted(List.of(), true));
        assertEquals(0, changes.get());
    }

    public void testReadOnlyDocumentIsNotQuoted() {
        myFixture.configureByText(CsvFileType.INSTANCE, "a,b");
        var document = myFixture.getEditor().getDocument();
        document.setReadOnly(true);
        try {
            CsvPsiTreeUpdater.edit(getPsiFile(), updater -> updater.setQuoted(true));
            assertEquals("a,b", document.getText());
        } finally {
            document.setReadOnly(false);
        }
    }

    public void testFieldsFromAnotherFileAreRejectedBeforeChangingDocument() {
        myFixture.configureByText("other.csv", "a,b");
        CsvField other = field(0, 0);
        myFixture.configureByText("edited.csv", "c,d");
        assertThrows(IllegalArgumentException.class, () ->
                CsvPsiTreeUpdater.edit(getPsiFile(), updater -> updater.setQuoted(List.of(other), true)));
        assertEquals("c,d", getPsiFile().getText());
    }

    public void testMoveOrderMovesContiguousAndDisjointSelectionsTogether() {
        assertArrayEquals(new int[]{0, 2, 3, 1, 4},
                CsvDocumentReorder.moveOrder(5, new int[]{2, 3}, -1));
        assertArrayEquals(new int[]{0, 3, 1, 2, 4},
                CsvDocumentReorder.moveOrder(5, new int[]{1, 2}, 1));
        assertArrayEquals(new int[]{1, 0, 3, 2, 4},
                CsvDocumentReorder.moveOrder(5, new int[]{1, 3}, -1));
        assertThrows(IllegalArgumentException.class, () -> CsvDocumentReorder.moveOrder(5, new int[]{0}, -1));
        assertThrows(IllegalArgumentException.class, () -> CsvDocumentReorder.moveOrder(5, new int[]{4}, 1));
        assertThrows(IllegalArgumentException.class, () -> CsvDocumentReorder.moveOrder(5, new int[]{5}, -1));
        assertThrows(IllegalArgumentException.class, () -> CsvDocumentReorder.moveOrder(5, new int[]{2, 1}, -1));
    }

    private CsvField field(int row, int column) {
        CsvRecord record = PsiHelper.getNthChildOfType(getPsiFile(), row, CsvRecord.class);
        assertNotNull(record);
        return record.getFieldList().get(column);
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

    private void undoAndRedo(String original, String changed) {
        UndoManager manager = UndoManager.getInstance(getProject());
        TextEditor editor = TextEditorProvider.getInstance().getTextEditor(myFixture.getEditor());
        manager.undo(editor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(original, getPsiFile().getText());
        manager.redo(editor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(changed, getPsiFile().getText());
    }
}
