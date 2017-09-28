package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;

public class CsvIntentionTest extends LightCodeInsightFixtureTestCase {

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

    public void testUnquoteAllIntention() throws Throwable {
        doTestIntention("UnquoteAll", "Unquote All");
    }

    public void testQuoteIntention() throws Throwable {
        doTestIntention("QuoteValue", "Quote");
    }

    public void testUnquoteIntention() throws Throwable {
        doTestIntention("UnquoteValue", "Unquote");
    }
}
