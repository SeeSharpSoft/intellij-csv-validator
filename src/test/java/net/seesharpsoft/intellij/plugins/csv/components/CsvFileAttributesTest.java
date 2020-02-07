package net.seesharpsoft.intellij.plugins.csv.components;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public class CsvFileAttributesTest extends LightPlatformCodeInsightFixtureTestCase {
    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/components";
    }

    @Override
    protected void tearDown() throws Exception {
        CsvFileAttributes.getInstance(this.getProject()).reset();
        super.tearDown();
    }

    public void testDefaultEscapeCharacter() {
        myFixture.configureByFiles("AnyFile.csv");

        assertEquals(CsvEditorSettings.ESCAPE_CHARACTER_DEFAULT, CsvEditorSettings.getInstance().getDefaultEscapeCharacter());
    }

    public void testFileEscapeCharacter() {
        myFixture.configureByFiles("AnyFile.csv");

        assertEquals(CsvEditorSettings.ESCAPE_CHARACTER_DEFAULT, CsvFileAttributes.getInstance(this.getProject()).getEscapeCharacter(this.getProject(), myFixture.getFile().getOriginalFile().getVirtualFile()));
        assertEquals(CsvEditorSettings.ESCAPE_CHARACTER_DEFAULT, CsvHelper.getEscapeCharacter(myFixture.getFile()));
    }

    public void testSaveFileEscapeCharacter() {
        myFixture.configureByFiles("AnyFile.csv");

        CsvFileAttributes csvFileAttributes = CsvFileAttributes.getInstance(this.getProject());
        csvFileAttributes.setEscapeCharacter(myFixture.getFile(), CsvEscapeCharacter.BACKSLASH);

        assertEquals(CsvEscapeCharacter.BACKSLASH, csvFileAttributes.getEscapeCharacter(this.getProject(), myFixture.getFile().getOriginalFile().getVirtualFile()));
    }

}
