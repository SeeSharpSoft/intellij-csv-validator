package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorSettings;

public class CsvIntentionTest extends LightPlatformCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/intention";
    }

    protected void doTestIntention(String testName, String hint) throws Throwable {
        myFixture.configureByFile(testName + "/before.csv");
        final IntentionAction action = myFixture.filterAvailableIntentions(hint).stream()
                .filter(intentionAction -> intentionAction.getText().equals(hint))
                .findFirst().get();
        myFixture.launchAction(action);
        myFixture.checkResultByFile(testName + "/after.csv");
    }

    public void testQuoteAllIntention() throws Throwable {
        doTestIntention("QuoteAll", "Quote All");
    }

    public void testQuoteAllBackslashIntention() throws Throwable {
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEditorSettings.EscapeCharacter.BACKSLASH);
        doTestIntention("QuoteAllBackslash", "Quote All");
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEditorSettings.ESCAPE_CHARACTER_DEFAULT);
    }

    public void testUnquoteAllIntention() throws Throwable {
        doTestIntention("UnquoteAll", "Unquote All");
    }

    public void testUnquoteAllBackslashIntention() throws Throwable {
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEditorSettings.EscapeCharacter.BACKSLASH);
        doTestIntention("UnquoteAllBackslash", "Unquote All");
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEditorSettings.ESCAPE_CHARACTER_DEFAULT);
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
