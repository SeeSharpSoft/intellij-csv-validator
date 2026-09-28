package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;

public class CsvPluginTest extends CsvBasePlatformTestCase {

    public void testProjectMaintenanceDoesNotInitializeFileAttributesService() {
        assertNull(getProject().getServiceIfCreated(CsvFileAttributes.class));

        CsvPlugin.cleanupProjectAttributes(getProject(), new EmptyProgressIndicator());

        assertNull(getProject().getServiceIfCreated(CsvFileAttributes.class));
    }
}
