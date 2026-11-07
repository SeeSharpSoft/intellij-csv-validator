package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import org.jetbrains.plugins.github.util.GithubSettings;

public abstract class CsvGithubIssueSubmitterTestBase extends CsvBasePlatformTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ServiceContainerUtil.registerOrReplaceServiceInstance(ApplicationManager.getApplication(),
                GithubSettings.class, new GithubSettings(), getTestRootDisposable());
    }
}
