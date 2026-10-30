package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.testFramework.FileEditorManagerTestCase;
import com.intellij.testFramework.PlatformTestUtil;

public abstract class CsvFileEditorManagerTestCase extends FileEditorManagerTestCase {

    /**
     * Waits until the composite has created its file editors. Editor windows show a composite before its editors exist,
     * e.g. after opening a file or splitting a window, and dispatching pending events does not wait for them.
     */
    protected static EditorComposite waitForEditors(EditorComposite composite) {
        assertNotNull(composite);
        PlatformTestUtil.waitWithEventsDispatching("file editors must be created",
                () -> !composite.getAllEditors().isEmpty(), 10);
        return composite;
    }
}
