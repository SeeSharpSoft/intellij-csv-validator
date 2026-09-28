package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class CsvPluginTest extends CsvBasePlatformTestCase {

    public void testProjectMaintenanceDoesNotInitializeFileAttributesService() {
        Project project = mock(Project.class);

        CsvPlugin.cleanupProjectAttributes(project, new EmptyProgressIndicator());

        verify(project).getServiceIfCreated(CsvFileAttributes.class);
        verify(project, never()).getService(CsvFileAttributes.class);
    }
}
