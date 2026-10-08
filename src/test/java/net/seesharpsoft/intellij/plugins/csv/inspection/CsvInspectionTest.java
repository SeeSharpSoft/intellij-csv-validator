package net.seesharpsoft.intellij.plugins.csv.inspection;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.FileASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CsvInspectionTest extends CsvBasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/inspection";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.enableInspections(CsvValidationInspection.class);
    }

    protected void doTestIntention(String testName, String hint) throws Throwable {
        myFixture.configureByFile(testName + "/before.csv");
        final IntentionAction action = myFixture.filterAvailableIntentions(hint).stream()
                .filter(intentionAction -> intentionAction.getText().equals(hint))
                .findFirst().get();

        Document document = myFixture.getDocument(myFixture.getFile());
        AtomicInteger documentChanges = new AtomicInteger();
        DocumentListener listener = new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                documentChanges.incrementAndGet();
            }
        };
        document.addDocumentListener(listener);
        try {
            myFixture.launchAction(action);
        } finally {
            document.removeDocumentListener(listener);
        }
        myFixture.checkResultByFile(testName + "/after.csv");
        assertEquals("Each inspection fix must produce one document change", 1, documentChanges.get());
    }

    public void testAddClosingQuote() throws Throwable {
        doTestIntention("AddClosingQuote", "Add closing quote");
    }

    public void testAddMissingSeparator() throws Throwable {
        doTestIntention("AddMissingSeparator", "Add separator");
    }

    public void testSurroundWithQuotes() throws Throwable {
        doTestIntention("SurroundWithQuotes", "Surround with quotes");
    }

    public void testInvalidRange() throws Throwable {
        doTestIntention("AddMissingSeparator", "Add separator");
    }

    public void testInspectionWithMultipleCsvFilesInSameDirectory() {
        myFixture.addFileToProject("same-directory/first.csv", "first,value");
        myFixture.addFileToProject("same-directory/second.csv", "second,value");
        myFixture.configureByFile("same-directory/first.csv");

        myFixture.doHighlighting();
    }

    public void testInspectionDoesNotAccessFileSibling() {
        ProblemsHolder holder = mock(ProblemsHolder.class);
        PsiFile file = mock(PsiFile.class);
        FileASTNode node = mock(FileASTNode.class);
        when(holder.getFile()).thenReturn(file);
        when(file.getLanguage()).thenReturn(CsvLanguage.INSTANCE);
        when(file.getNode()).thenReturn(node);
        when(node.getElementType()).thenReturn(TokenType.WHITE_SPACE);
        doThrow(new AssertionError("A file node must not access its parent siblings"))
                .when(file).getNextSibling();

        new CsvValidationInspection().buildVisitor(holder, false).visitElement(file);
    }
}
