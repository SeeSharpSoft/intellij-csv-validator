package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandler;
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.TextRange;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class CsvHighlightUsagesHandlerTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/highlighter";
    }

    private void assertHighlightedText(RangeHighlighter rangeHighlighter, String text) {
        assertEquals(text, rangeHighlighter.getDocument().getText(TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset())));
    }

    private RangeHighlighter[] testHighlightUsages(String... fileNames) {
        myFixture.configureByFiles(fileNames);

        HighlightUsagesHandlerBase handler = HighlightUsagesHandler.createCustomHandler(myFixture.getEditor(), myFixture.getFile());

        String featureId = handler.getFeatureId();
        if (featureId != null) {
            FeatureUsageTracker.getInstance().triggerFeatureUsed(featureId);
        }

        handler.highlightUsages();

        Editor editor = myFixture.getEditor();
        return editor.getMarkupModel().getAllHighlighters();
    }

    public void testHighlightUsages01() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData01.csv");

        assertSize(2, rangeHighlighters);
        assertHighlightedText(rangeHighlighters[0], " Header 2");
        assertHighlightedText(rangeHighlighters[1], " Value 2");
    }

    public void testHighlightUsages02() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData02.csv");

        assertSize(2, rangeHighlighters);
        assertHighlightedText(rangeHighlighters[0], " Header 2");
        assertHighlightedText(rangeHighlighters[1], " Value 2");
    }

    public void testHighlightUsages03() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData03.csv");

        assertSize(2, rangeHighlighters);
        assertHighlightedText(rangeHighlighters[0], "Header 1");
        assertHighlightedText(rangeHighlighters[1], "Value 1");
    }

    public void testHighlightUsages04() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData04.csv");

        assertSize(1, rangeHighlighters);
        assertHighlightedText(rangeHighlighters[0], " Value 3");
    }

    public void testHighlightUsages05() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData05.csv");

        assertSize(2, rangeHighlighters);
        assertHighlightedText(rangeHighlighters[0], " Header 2");
        assertHighlightedText(rangeHighlighters[1], " Value 2");
    }

    public void testHighlightUsages06() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData06.csv");

        assertSize(1, rangeHighlighters);
        assertHighlightedText(rangeHighlighters[0], " Value 3");
    }

    public void testHighlightUsages07() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData07.csv");

        assertSize(0, rangeHighlighters);
    }

    public void testHighlightUsages08() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData08.csv");

        assertSize(0, rangeHighlighters);
    }
}
