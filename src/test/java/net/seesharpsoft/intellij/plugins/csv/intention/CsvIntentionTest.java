package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class CsvIntentionTest extends CsvBasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/intention";
    }

    @Override
    protected void tearDown() throws Exception {
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEditorSettings.ESCAPE_CHARACTER_DEFAULT);
        super.tearDown();
    }

    protected void doTestIntention(String testName, String hint) throws Throwable {
        doTestIntention(testName, hint, false);
    }

    protected void doTestIntention(String testName, String hint, boolean expectError) throws Throwable {
        myFixture.configureByFile(testName + "/before.csv");
        final IntentionAction action = myFixture.filterAvailableIntentions(hint).stream()
                .filter(intentionAction -> intentionAction.getText().equals(hint))
                .findFirst().orElse(null);
        if (action == null) {
            assertTrue("action not found -> this was expected: " + expectError, expectError);
        } else {
            assertFalse("action was found -> this was expected: " + !expectError, expectError);
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
            assertTrue("Each intention action must produce not more than one document change", documentChanges.get() < 2);
        }
    }

    public void testErroneousCsv() throws Throwable {
        doTestIntention("Erroneous", "Quote All", true);
    }

    public void testErroneousBackslashCsv() throws Throwable {
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
        doTestIntention("ErroneousBackslash", "Quote All", true);
    }

    public void testQuoteAllIntention() throws Throwable {
        doTestIntention("QuoteAll", "Quote All");
    }

    public void testQuoteAllBackslashIntention() throws Throwable {
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
        doTestIntention("QuoteAllBackslash", "Quote All");
    }

    public void testUnquoteAllIntention() throws Throwable {
        doTestIntention("UnquoteAll", "Unquote All");
    }

    public void testUnquoteAllBackslashIntention() throws Throwable {
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
        doTestIntention("UnquoteAllBackslash", "Unquote All");
    }

    public void testQuoteIntention() throws Throwable {
        doTestIntention("QuoteValue", "Quote");
    }

    public void testUnquoteIntention() throws Throwable {
        doTestIntention("UnquoteValue", "Unquote");
    }

    public void testShiftColumnLeftIntention() throws Throwable {
        for (int i = 1; i < 5; ++i) {
            doTestIntention(String.format("ShiftColumnLeft%02d", i), "Shift Column Left");
        }
    }

    public void testShiftColumnRightIntention() throws Throwable {
        for (int i = 1; i < 5; ++i) {
            doTestIntention(String.format("ShiftColumnRight%02d", i), "Shift Column Right");
        }
    }
}
