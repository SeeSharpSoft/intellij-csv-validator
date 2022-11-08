package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.Presentation;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;

public class CsvChangeEscapeCharacterActionTest extends CsvActionTestBase {

    public void testActionGroupVisibilityForCsv() {
        myFixture.configureByFiles("CommaSeparated.csv");

        Presentation presentation = testActionGroup(new CsvChangeEscapeCharacterActionGroup(), myFixture);
        assertTrue(presentation.isVisible());
        assertTrue(presentation.isEnabled());
    }

    public void testActionGroupVisibilityForTsv() {
        myFixture.configureByFiles("TabSeparated.tsv");

        Presentation presentation = testActionGroup(new CsvChangeEscapeCharacterActionGroup(), myFixture);
        assertTrue(presentation.isVisible());
        assertTrue(presentation.isEnabled());
    }

    public void testChangeEscapeCharacter() {
        myFixture.configureByFiles("CommaSeparated.csv");

        for (CsvEscapeCharacter escapeCharacter : CsvEscapeCharacter.values()) {
            Presentation presentation = myFixture.testAction(new CsvChangeEscapeCharacterAction(escapeCharacter));
            assertEquals(escapeCharacter.getDisplay(), presentation.getText());
            assertEquals(escapeCharacter, CsvHelper.getEscapeCharacter(myFixture.getFile()));
        }
    }

    public void testDefaultEscapeCharacterAction() {
        myFixture.configureByFiles("CommaSeparated.csv");

        CsvEscapeCharacter initialEscapeCharacter = CsvHelper.getEscapeCharacter(myFixture.getFile());

        myFixture.testAction(new CsvChangeEscapeCharacterAction(CsvEscapeCharacter.BACKSLASH));

        assertFalse("separator should not be initial", initialEscapeCharacter.equals(CsvHelper.getEscapeCharacter(myFixture.getFile())));

        myFixture.testAction(new CsvDefaultEscapeCharacterAction());

        assertEquals(initialEscapeCharacter, CsvHelper.getEscapeCharacter(myFixture.getFile()));
    }
}
