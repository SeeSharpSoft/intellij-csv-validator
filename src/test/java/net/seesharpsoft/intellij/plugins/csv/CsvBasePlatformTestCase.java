package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.nio.file.Paths;

public abstract class CsvBasePlatformTestCase extends BasePlatformTestCase {
    @Override
    protected void setUp() throws Exception {
        // TODO figure out "Must be precomputed" issue!
        System.setProperty("hidpi", "false");
        super.setUp();
        VfsRootAccess.allowRootAccess(getTestRootDisposable(),
                Paths.get(getTestDataPath()).toAbsolutePath().normalize().toString());
    }
}
