package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import org.jetbrains.annotations.NotNull;

import static com.intellij.testFramework.TestActionEvent.createTestEvent;

public abstract class CsvActionTestBase extends CsvBasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/actions";
    }

    @Override
    protected void tearDown() throws Exception {
        CsvFileAttributes.getInstance(this.getProject()).reset();
        super.tearDown();
    }

    @NotNull
    public Presentation doTestActionGroup(@NotNull AnAction action, CodeInsightTestFixture myFixture) {
        AnActionEvent e = createTestEvent(action);
        if (ActionUtil.lastUpdateAndCheckDumb(action, e, true)) {
            action.update(e);
        }
        return e.getPresentation();
    }

}
