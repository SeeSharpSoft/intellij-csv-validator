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
        Paths.get(this.getProject().getBasePath(), "test/py_file_test.py").toFile().createNewFile();
    }

    @Override
    protected void tearDown() throws Exception {
        // TODO delete or not delete, that's the question
        // Paths.get(this.getProject().getBasePath(), "csv_file_test.csv").toFile().delete();
        // Paths.get(this.getProject().getBasePath(), "test/py_file_test.py").toFile().delete();
        // Paths.get(this.getProject().getBasePath(), "test").toFile().delete();
        super.tearDown();
    }

    public void testGetFileInProjectExists() {
        VirtualFile vf = CsvStorageHelper.getFileInProject(this.getProject(), "csv_file_test.csv");
        assertNotNull(vf);
    }

    public void testGetFileInProjectDirectory() {
        VirtualFile vf = CsvStorageHelper.getFileInProject(this.getProject(), "\\test\\py_file_test.py");
        assertNotNull(vf);
    }

    public void testGetFileInProjectDoesNotExist() {
        VirtualFile vf = CsvStorageHelper.getFileInProject(this.getProject(), "do_not_exist_csv_file_test.csv");
        assertNull(vf);
    }

    public void testGetFileInProjectDirectoryDoesNotExist() {
        VirtualFile vf = CsvStorageHelper.getFileInProject(this.getProject(), "\\test2\\py_file_test.py");
        assertNull(vf);
    }
}
