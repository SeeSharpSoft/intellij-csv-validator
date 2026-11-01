package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.TextAttributes;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import java.awt.Color;

public class CsvTabSeparatorHighlightingTest extends CsvBasePlatformTestCase {

    private static final Color TAB_COLOR = new Color(0x12, 0x34, 0x56);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        settings.loadState(new CsvEditorSettings.OptionSet());
        settings.setHighlightTabSeparator(true);
        settings.setTabHighlightColor(TAB_COLOR);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        } finally {
            super.tearDown();
        }
    }

    public void testTabSeparatorsUseTheConfiguredBackgroundEvenWithInfoBalloonDisabled() {
        CsvEditorSettings.getInstance().setShowInfoBalloon(false);
        myFixture.configureByText("test.tsv", "first\tsecond\n");

        assertEquals(TAB_COLOR, attributesAt(5).getBackgroundColor());
    }

    public void testTabSeparatorsKeepTheSeparatorForeground() {
        myFixture.configureByText("test.tsv", "first\tsecond\n");

        TextAttributes separator = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CsvTextAttributeKeys.COMMA);
        assertEquals(separator.getForegroundColor(), attributesAt(5).getForegroundColor());
    }

    public void testTabSeparatorsAreNotHighlightedWhenTheOptionIsDisabled() {
        CsvEditorSettings.getInstance().setHighlightTabSeparator(false);
        myFixture.configureByText("test.tsv", "first\tsecond\n");

        assertFalse(TAB_COLOR.equals(attributesAt(5).getBackgroundColor()));
    }

    public void testTabsInsideCommaSeparatedValuesAreNotHighlighted() {
        myFixture.configureByText("test.csv", "fi\trst,second\n");

        assertFalse(TAB_COLOR.equals(attributesAt(2).getBackgroundColor()));
    }

    private TextAttributes attributesAt(int offset) {
        return ((EditorEx) myFixture.getEditor()).getHighlighter().createIterator(offset).getTextAttributes();
    }
}
