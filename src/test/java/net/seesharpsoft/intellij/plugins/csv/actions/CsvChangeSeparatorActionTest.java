package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.Presentation;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;

public class CsvChangeSeparatorActionTest extends CsvActionTestBase {

    public void testActionGroupVisibilityForCsv() {
        myFixture.configureByFiles("CommaSeparated.csv");

        Presentation presentation = testActionGroup(new CsvChangeSeparatorActionGroup(), myFixture);
        assertTrue(presentation.isVisible());
        assertTrue(presentation.isEnabled());
    }

    public void testActionGroupVisibilityForTsv() {
        myFixture.configureByFiles("TabSeparated.tsv");

        Presentation presentation = testActionGroup(new CsvChangeSeparatorActionGroup(), myFixture);
        assertFalse(presentation.isVisible());
        assertFalse(presentation.isEnabled());
    }

    public void testChangeSeparatorForCsv() {
        myFixture.configureByFiles("CommaSeparated.csv");

        for (CsvValueSeparator newSeparator : CsvValueSeparator.values()) {
            Presentation presentation = myFixture.testAction(new CsvChangeSeparatorAction(newSeparator));
            assertEquals(newSeparator.getDisplay(), presentation.getText());
            assertEquals(newSeparator, CsvHelper.getValueSeparator(myFixture.getFile()));
        }
    }

    public void testChangeSeparatorForTsv() {
        myFixture.configureByFiles("TabSeparated.tsv");

        for (CsvValueSeparator newSeparator : CsvValueSeparator.values()) {
            Presentation presentation = myFixture.testAction(new CsvChangeSeparatorAction(newSeparator));
            assertEquals(newSeparator.getDisplay(), presentation.getText());
            // for TSV files, the separator should always be a tab
            assertEquals(CsvValueSeparator.TAB, CsvHelper.getValueSeparator(myFixture.getFile()));
        }
    }

    public void testDefaultSeparatorAction() {
        myFixture.configureByFiles("CommaSeparated.csv");

        CsvValueSeparator initialSeparator = CsvHelper.getValueSeparator(myFixture.getFile());

        myFixture.testAction(new CsvChangeSeparatorAction(CsvValueSeparator.PIPE));

        assertFalse("separator should not be initial", initialSeparator.equals(CsvHelper.getValueSeparator(myFixture.getFile())));

        myFixture.testAction(new CsvDefaultSeparatorAction());

        assertEquals(initialSeparator, CsvHelper.getValueSeparator(myFixture.getFile()));
    }
}
