package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public class CsvIntentionTest extends BasePlatformTestCase {

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
            myFixture.launchAction(action);
            myFixture.checkResultByFile(testName + "/after.csv");
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
