package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandler;
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.TextRange;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvHighlightUsagesHandlerTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/highlighter";
    }

    private void assertHighlightedTexts(RangeHighlighter[] rangeHighlighters, String... texts) {
        assertSize(texts.length, rangeHighlighters);
        List<String> needles =  new ArrayList(Arrays.asList(texts));
        for (RangeHighlighter rangeHighlighter : rangeHighlighters) {
            needles.removeIf(text -> text.equals(rangeHighlighter.getDocument().getText(TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset()))));
        }
        assertSize(0, needles);
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

        assertHighlightedTexts(rangeHighlighters, " Header 2", " Value 2");
    }

    public void testHighlightUsages02() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData02.csv");

        assertHighlightedTexts(rangeHighlighters, " Header 2", " Value 2");
    }

    public void testHighlightUsages03() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData03.csv");

        assertHighlightedTexts(rangeHighlighters, "Header 1", "Value 1");
    }

    public void testHighlightUsages04() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData04.csv");

        assertHighlightedTexts(rangeHighlighters, " Value 3");
    }

    public void testHighlightUsages05() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData05.csv");

        assertHighlightedTexts(rangeHighlighters, " Header 2", " Value 2");
    }

    public void testHighlightUsages06() {
        RangeHighlighter[] rangeHighlighters = testHighlightUsages("HighlightUsagesTestData06.csv");

        assertHighlightedTexts(rangeHighlighters, " Value 3");
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
