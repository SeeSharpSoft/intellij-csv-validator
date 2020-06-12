package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;

public class CsvChangeEscapeCharacterActionTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/actions";
    }

    @Override
    protected void tearDown() throws Exception {
        CsvFileAttributes.getInstance(this.getProject()).reset();
        super.tearDown();
    }

    public void testActionGroupVisibilityForCsv() {
        myFixture.configureByFiles("CommaSeparated.csv");

        Presentation presentation = myFixture.testAction(new CsvChangeEscapeCharacterActionGroup());
        assertTrue(presentation.isVisible());
        assertTrue(presentation.isEnabled());
    }

    public void testActionGroupVisibilityForTsv() {
        myFixture.configureByFiles("TabSeparated.tsv");

        Presentation presentation = myFixture.testAction(new CsvChangeEscapeCharacterActionGroup());
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
