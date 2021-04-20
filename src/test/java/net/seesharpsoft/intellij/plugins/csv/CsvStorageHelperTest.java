package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.nio.file.Paths;

public class CsvStorageHelperTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Paths.get(this.getProject().getBasePath(), "csv_file_test.csv").toFile().createNewFile();
        Paths.get(this.getProject().getBasePath(), "test").toFile().mkdir();
        Paths.get(this.getProject().getBasePath(), "test", "py_file_test.py").toFile().createNewFile();
    }

    @Override
    protected void tearDown() throws Exception {
        Paths.get(this.getProject().getBasePath(), "csv_file_test.csv").toFile().delete();
        Paths.get(this.getProject().getBasePath(), "test", "py_file_test.py").toFile().delete();
        Paths.get(this.getProject().getBasePath(), "test").toFile().delete();
        super.tearDown();
    }

    public void testGetFileInProjectExists() {
        VirtualFile vf = CsvStorageHelper.getFileInProject(this.getProject(), Paths.get("/csv_file_test.csv").toString());
        assertNotNull(vf);
    }

    public void testGetFileInProjectDirectory() {
        VirtualFile vf = CsvStorageHelper.getFileInProject(this.getProject(), Paths.get("/test/py_file_test.py").toString());
        assertNotNull(vf);
    }

    public void testGetFileInProjectDoesNotExist() {
        VirtualFile vf = CsvStorageHelper.getFileInProject(this.getProject(), Paths.get("/not_existing_csv_file_test.csv").toString());
        assertNull(vf);
    }

    public void testGetFileInProjectDirectoryDoesNotExist() {
        VirtualFile vf = CsvStorageHelper.getFileInProject(this.getProject(), Paths.get("/test2/py_file_test.py").toString());
        assertNull(vf);
    }
}
