package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;

public class CsvChangeSeparatorActionTest extends LightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/actions";
    }

    public void testActionGroupVisibilityForCsv() {
        myFixture.configureByFiles("CommaSeparated.csv");

        Presentation presentation = myFixture.testAction(new CsvChangeSeparatorActionGroup());
        assertTrue(presentation.isVisible());
        assertTrue(presentation.isEnabled());
    }

    public void testActionGroupVisibilityForTsv() {
        myFixture.configureByFiles("TabSeparated.tsv");

        Presentation presentation = myFixture.testAction(new CsvChangeSeparatorActionGroup());
        assertFalse(presentation.isVisible());
        assertFalse(presentation.isEnabled());
    }

    public void testChangeSeparatorForCsv() {
        myFixture.configureByFiles("CommaSeparated.csv");

        for (int i = 0; i < CsvCodeStyleSettings.SUPPORTED_SEPARATORS.length; ++i) {
            String newSeparator = CsvCodeStyleSettings.SUPPORTED_SEPARATORS[i];
            Presentation presentation = myFixture.testAction(new CsvChangeSeparatorAction(newSeparator, CsvCodeStyleSettings.getSeparatorDisplayText(newSeparator)));
            assertEquals(CsvCodeStyleSettings.getSeparatorDisplayText(newSeparator), presentation.getText());
            assertEquals(newSeparator, CsvCodeStyleSettings.getCurrentSeparator(myFixture.getFile()));
        }
    }

    public void testChangeSeparatorForTsv() {
        myFixture.configureByFiles("TabSeparated.tsv");

        for (int i = 0; i < CsvCodeStyleSettings.SUPPORTED_SEPARATORS.length; ++i) {
            String newSeparator = CsvCodeStyleSettings.SUPPORTED_SEPARATORS[i];
            Presentation presentation = myFixture.testAction(new CsvChangeSeparatorAction(newSeparator, CsvCodeStyleSettings.getSeparatorDisplayText(newSeparator)));
            assertEquals(CsvCodeStyleSettings.getSeparatorDisplayText(newSeparator), presentation.getText());
            // for TSV files, the separator should always be a tab
            assertEquals("\t", CsvCodeStyleSettings.getCurrentSeparator(myFixture.getFile()));
        }
    }

    public void testDefaultSeparatorAction() {
        myFixture.configureByFiles("CommaSeparated.csv");

        String initialSeparator = CsvCodeStyleSettings.getCurrentSeparator(myFixture.getFile());

        myFixture.testAction(new CsvChangeSeparatorAction("|", CsvCodeStyleSettings.getSeparatorDisplayText("|")));

        assertFalse("separator should not be initial", initialSeparator.equals(CsvCodeStyleSettings.getCurrentSeparator(myFixture.getFile())));

        myFixture.testAction(new CsvDefaultSeparatorAction());

        assertEquals(initialSeparator, CsvCodeStyleSettings.getCurrentSeparator(myFixture.getFile()));
    }
}
