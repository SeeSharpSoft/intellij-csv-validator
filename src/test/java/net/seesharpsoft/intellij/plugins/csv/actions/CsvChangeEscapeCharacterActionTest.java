package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public class CsvChangeEscapeCharacterActionTest extends LightPlatformCodeInsightFixtureTestCase {

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

        for (CsvEditorSettings.EscapeCharacter escapeCharacter : CsvEditorSettings.EscapeCharacter.values()) {
            Presentation presentation = myFixture.testAction(new CsvChangeEscapeCharacterAction(escapeCharacter));
            assertEquals(escapeCharacter.getDisplay(), presentation.getText());
            assertEquals(escapeCharacter, CsvHelper.getCurrentEscapeCharacter(myFixture.getFile()));
        }
    }

    public void testDefaultEscapeCharacterAction() {
        myFixture.configureByFiles("CommaSeparated.csv");

        CsvEditorSettings.EscapeCharacter initialEscapeCharacter = CsvHelper.getCurrentEscapeCharacter(myFixture.getFile());

        myFixture.testAction(new CsvChangeEscapeCharacterAction(CsvEditorSettings.EscapeCharacter.BACKSLASH));

        assertFalse("separator should not be initial", initialEscapeCharacter.equals(CsvHelper.getCurrentEscapeCharacter(myFixture.getFile())));

        myFixture.testAction(new CsvDefaultEscapeCharacterAction());

        assertEquals(initialEscapeCharacter, CsvHelper.getCurrentEscapeCharacter(myFixture.getFile()));
    }
}
