package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;

public class CsvEditorSettingsTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/settings";
    }

    public void testDefaultValueSeparator() {
        myFixture.configureByFiles("AnyFile.csv");

        assertEquals(CsvEditorSettings.VALUE_SEPARATOR_DEFAULT, CsvHelper.getValueSeparator(myFixture.getFile()));
    }

    public void testDefaultEscapeCharacter() {
        myFixture.configureByFiles("AnyFile.csv");

        assertEquals(CsvEditorSettings.ESCAPE_CHARACTER_DEFAULT, CsvHelper.getEscapeCharacter(myFixture.getFile()));
    }
}
