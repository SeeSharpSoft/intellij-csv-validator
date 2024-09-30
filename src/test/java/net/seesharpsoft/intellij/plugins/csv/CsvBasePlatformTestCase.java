package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public abstract class CsvBasePlatformTestCase extends BasePlatformTestCase {
    @Override
    protected void setUp() throws Exception {
        // TODO figure out "Must be precomputed" issue!
        System.setProperty("hidpi", "false");
        super.setUp();
    }
}
