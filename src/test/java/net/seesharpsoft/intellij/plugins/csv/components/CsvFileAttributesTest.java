package net.seesharpsoft.intellij.plugins.csv.components;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import java.nio.file.Paths;

public class CsvFileAttributesTest extends BasePlatformTestCase {
    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/components";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Paths.get(this.getProject().getBasePath()).toFile().mkdirs();
        Paths.get(this.getProject().getBasePath(), "csv_file_test.csv").toFile().createNewFile();
        Paths.get(this.getProject().getBasePath(), "test").toFile().mkdir();
        Paths.get(this.getProject().getBasePath(), "test", "py_file_test.py").toFile().createNewFile();
    }

    @Override
    protected void tearDown() throws Exception {
        Paths.get(this.getProject().getBasePath(), "test", "py_file_test.py").toFile().delete();
        Paths.get(this.getProject().getBasePath(), "test").toFile().delete();
        Paths.get(this.getProject().getBasePath(), "csv_file_test.csv").toFile().delete();

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

    public void testCleanupAttributeMap() {
        CsvFileAttributes fileAttributes = CsvFileAttributes.getInstance(this.getProject());
        fileAttributes.attributeMap.put("/csv_file_test.csv", new CsvFileAttributes.Attribute());
        fileAttributes.attributeMap.put("/test/py_file_test.py", new CsvFileAttributes.Attribute());
        fileAttributes.attributeMap.put("/not_existing_csv_file_test.csv", new CsvFileAttributes.Attribute());
        fileAttributes.attributeMap.put("/<835d7ae1-4344-4666-bc29-31fb457b610e>\\raid\\someRemotePath\\someFileName.csv", new CsvFileAttributes.Attribute());
        fileAttributes.attributeMap.put("/<$.}>^\\ra*id\\someRemote \"Path\\someFileName.json", new CsvFileAttributes.Attribute());

        assertEquals(5, fileAttributes.attributeMap.size());

        fileAttributes.cleanupAttributeMap(this.getProject());

        assertEquals(1, fileAttributes.attributeMap.size());
        assertNotNull(fileAttributes.attributeMap.get("/csv_file_test.csv"));
    }

}
