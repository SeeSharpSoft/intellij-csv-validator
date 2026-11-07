package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.WeighedFileEditorProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Other plugins, like Big Data Tools (#937), may claim CSV files with {@link FileEditorPolicy#HIDE_OTHER_EDITORS},
 * which would remove the CSV text and table editors unless they claim precedence the same way.
 */
public class CsvEditorProviderPrecedenceTest extends CsvBasePlatformTestCase {

    private static final String HIDING_EDITOR_TYPE_ID = "bde-ide-default-editor";
    private static final String ADDITIONAL_EDITOR_TYPE_ID = "other-plugin-preview";

    /**
     * Mirrors the policy and weight of Big Data Tools' default editor provider, or an editor that is shown in addition.
     */
    private static final class OtherPluginEditorProvider extends WeighedFileEditorProvider implements DumbAware {
        private final String editorTypeId;
        private final FileEditorPolicy policy;

        private OtherPluginEditorProvider(String editorTypeId, FileEditorPolicy policy) {
            this.editorTypeId = editorTypeId;
            this.policy = policy;
        }

        @Override
        public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
            return true;
        }

        @Override
        public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
            throw new UnsupportedOperationException("provider resolution only");
        }

        @Override
        public @NotNull String getEditorTypeId() {
            return editorTypeId;
        }

        @Override
        public @NotNull FileEditorPolicy getPolicy() {
            return policy;
        }

        @Override
        public double getWeight() {
            return 4;
        }
    }

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        myFixture.configureByFiles("AnyFile.csv");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        } finally {
            super.tearDown();
        }
    }

    private void registerOtherPluginEditor(String editorTypeId, FileEditorPolicy policy) {
        FileEditorProvider.EP_FILE_EDITOR_PROVIDER.getPoint()
                .registerExtension(new OtherPluginEditorProvider(editorTypeId, policy), getTestRootDisposable());
    }

    private void enableHidingOtherEditors() {
        CsvEditorSettings.getInstance().setHideOtherEditors(true);
    }

    private void registerOtherPluginEditorHidingOtherEditors() {
        registerOtherPluginEditor(HIDING_EDITOR_TYPE_ID, FileEditorPolicy.HIDE_OTHER_EDITORS);
    }

    private List<String> resolveEditorTypeIds() {
        return FileEditorProviderManager.getInstance()
                .getProviderList(getProject(), myFixture.getFile().getVirtualFile())
                .stream()
                .map(FileEditorProvider::getEditorTypeId)
                .toList();
    }

    public void testTextAndTableEditorPrecedeEditorOfOtherPluginHidingOtherEditors() {
        enableHidingOtherEditors();
        registerOtherPluginEditorHidingOtherEditors();

        assertEquals(List.of(CsvFileEditorProviderBase.TEXT_EDITOR_TYPE_ID, CsvFileEditorProviderBase.TABLE_EDITOR_TYPE_ID, HIDING_EDITOR_TYPE_ID),
                resolveEditorTypeIds());
    }

    public void testTableEditorFirstPrecedesEditorOfOtherPluginHidingOtherEditors() {
        enableHidingOtherEditors();
        CsvEditorSettings.getInstance().setEditorPrio(CsvEditorSettings.EditorPrio.TABLE_FIRST);
        registerOtherPluginEditorHidingOtherEditors();

        assertEquals(List.of(CsvFileEditorProviderBase.TABLE_EDITOR_TYPE_ID, CsvFileEditorProviderBase.TEXT_EDITOR_TYPE_ID, HIDING_EDITOR_TYPE_ID),
                resolveEditorTypeIds());
    }

    public void testTextEditorOnlyPrecedesEditorOfOtherPluginHidingOtherEditors() {
        enableHidingOtherEditors();
        CsvEditorSettings.getInstance().setEditorPrio(CsvEditorSettings.EditorPrio.TEXT_ONLY);
        registerOtherPluginEditorHidingOtherEditors();

        assertEquals(List.of(CsvFileEditorProviderBase.TEXT_EDITOR_TYPE_ID, HIDING_EDITOR_TYPE_ID), resolveEditorTypeIds());
    }

    public void testOtherPluginHidesCsvEditorsByDefault() {
        registerOtherPluginEditorHidingOtherEditors();

        assertEquals(List.of(HIDING_EDITOR_TYPE_ID), resolveEditorTypeIds());
    }

    public void testEditorOfOtherPluginNotHidingOtherEditorsIsHidden() {
        enableHidingOtherEditors();
        registerOtherPluginEditor(ADDITIONAL_EDITOR_TYPE_ID, FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR);

        assertEquals(List.of(CsvFileEditorProviderBase.TEXT_EDITOR_TYPE_ID, CsvFileEditorProviderBase.TABLE_EDITOR_TYPE_ID),
                resolveEditorTypeIds());
    }

    public void testOtherPluginKeepsHidingOtherEditorsOfFilesNotHandledByCsvEditors() {
        enableHidingOtherEditors();
        registerOtherPluginEditorHidingOtherEditors();
        myFixture.configureByText("data.txt", "a,b");

        assertEquals(List.of(HIDING_EDITOR_TYPE_ID), resolveEditorTypeIds());
    }
}