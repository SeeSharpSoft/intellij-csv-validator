package net.seesharpsoft.intellij.plugins.csv.inspection;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class CsvInspectionTest extends BasePlatformTestCase {

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

        myFixture.launchAction(action);
        myFixture.checkResultByFile(testName + "/after.csv");
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
}
