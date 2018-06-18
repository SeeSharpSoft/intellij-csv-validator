package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;

public class CsvAnnotatorTest extends LightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources";
    }

    public void testAnnotator() {
        myFixture.testHighlighting("AnnotatorTestData.csv");
    }

}
