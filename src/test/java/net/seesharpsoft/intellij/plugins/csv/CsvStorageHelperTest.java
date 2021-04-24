package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.PathUtil;

import java.nio.file.Paths;

public class CsvStorageHelperTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Paths.get(this.getProject().getBasePath()).toFile().mkdirs();
        Paths.get(this.getProject().getBasePath(), "csv_file_test.csv").toFile().createNewFile();
        Paths.get(this.getProject().getBasePath(), "test").toFile().mkdir();
        Paths.get(this.getProject().getBasePath(), "test", "py_file_test.py").toFile().createNewFile();
        Paths.get(this.getProject().getBasePath(), "test", "tsv_file_test.tab").toFile().createNewFile();
    }

    @Override
    protected void tearDown() throws Exception {
        Paths.get(this.getProject().getBasePath(), "csv_file_test.csv").toFile().delete();
        Paths.get(this.getProject().getBasePath(), "test", "tsv_file_test.tab").toFile().delete();
        Paths.get(this.getProject().getBasePath(), "test", "py_file_test.py").toFile().delete();
        Paths.get(this.getProject().getBasePath(), "test").toFile().delete();
        super.tearDown();
    }

    public void testCsvFileExists() {
        boolean exists = CsvStorageHelper.csvFileExists(this.getProject(), Paths.get("/csv_file_test.csv").toString());
        assertTrue(exists);
    }

    public void testCsvFileExistsDirectory() {
        boolean exists = CsvStorageHelper.csvFileExists(this.getProject(), Paths.get("/test/tsv_file_test.tab").toString());
        assertTrue(exists);
    }

    public void testCsvFileExistsPyLanguage() {
        boolean exists = CsvStorageHelper.csvFileExists(this.getProject(), Paths.get("/test/py_file_test.py").toString());
        assertFalse(exists);
    }

    public void testCsvFileExistsDoesNotExist() {
        boolean exists = CsvStorageHelper.csvFileExists(this.getProject(), Paths.get("/not_existing_csv_file_test.csv").toString());
        assertFalse(exists);
    }

    public void testCsvFileExistsDirectoryDoesNotExist() {
        boolean exists = CsvStorageHelper.csvFileExists(this.getProject(), Paths.get("/test2/py_file_test.csv").toString());
        assertFalse(exists);
    }

    public void testGetFilePathForSSH() {
        boolean exists = CsvStorageHelper.csvFileExists(this.getProject(), "<835d7ae1-4344-4666-bc29-31fb457b610e>\\raid/someRemotePath//someFileName.json");
        assertFalse(exists);
    }

    public void testGetRelativeFileUrl() {
        VirtualFile vf = new LightVirtualFile("<835d7ae1-4344-4666-bc29-31fb457b610e>\\raid/someRemotePath\\someFileName.json");
        String relativePath = CsvStorageHelper.getRelativeFilePath(this.getProject(), vf);
        assertEquals(PathUtil.getLocalPath("/<835d7ae1-4344-4666-bc29-31fb457b610e>\\raid\\someRemotePath\\someFileName.json"), relativePath);
    }

    public void testGetRelativeFileUrlFaulty() {
        VirtualFile vf = new LightVirtualFile("/<$.}>^\\ra*id\\someRemote \"Path\\someFileName.csv");
        String relativePath = CsvStorageHelper.getRelativeFilePath(this.getProject(), vf);
        assertEquals(PathUtil.getLocalPath("\\\\<$.}>^\\ra*id\\someRemote \"Path\\someFileName.csv"), relativePath);
    }

}
