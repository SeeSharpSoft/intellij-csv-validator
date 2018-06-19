package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.TextRange;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;

public class CsvHighlightUsagesHandlerTest extends LightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/highlighter";
    }

    private void assertHighlightedText(RangeHighlighter rangeHighlighter, String text) {
        assertEquals(text,rangeHighlighter.getDocument().getText(TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset())));
    }

    public void testHighlightUsages01() {
        RangeHighlighter[] rangeHighlighters = myFixture.testHighlightUsages("HighlightUsagesTestData01.csv");

        assertSize(2, rangeHighlighters);
        assertHighlightedText(rangeHighlighters[0], "Header 2");
        assertHighlightedText(rangeHighlighters[1], "Value 2");
    }

    public void testHighlightUsages02() {
        RangeHighlighter[] rangeHighlighters = myFixture.testHighlightUsages("HighlightUsagesTestData02.csv");

        assertSize(2, rangeHighlighters);
        assertHighlightedText(rangeHighlighters[0], "Header 2");
        assertHighlightedText(rangeHighlighters[1], "Value 2");
    }

    public void testHighlightUsages03() {
        RangeHighlighter[] rangeHighlighters = myFixture.testHighlightUsages("HighlightUsagesTestData03.csv");

        assertSize(2, rangeHighlighters);
        assertHighlightedText(rangeHighlighters[0], "Header 1");
        assertHighlightedText(rangeHighlighters[1], "Value 1");
    }

    public void testHighlightUsages04() {
        RangeHighlighter[] rangeHighlighters = myFixture.testHighlightUsages("HighlightUsagesTestData04.csv");

        assertSize(1, rangeHighlighters);
        assertHighlightedText(rangeHighlighters[0], "Value 3");
    }

    public void testHighlightUsages05() {
        RangeHighlighter[] rangeHighlighters = myFixture.testHighlightUsages("HighlightUsagesTestData05.csv");

        assertSize(2, rangeHighlighters);
        assertHighlightedText(rangeHighlighters[0], "Header 2");
        assertHighlightedText(rangeHighlighters[1], "Value 2");
    }

    public void testHighlightUsages06() {
        RangeHighlighter[] rangeHighlighters = myFixture.testHighlightUsages("HighlightUsagesTestData06.csv");

        assertSize(0, rangeHighlighters);
    }
}
