package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.diff.editor.DiffVirtualFile;
import com.intellij.diff.impl.DiffRequestProcessor;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.fileEditor.impl.text.TextEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class CsvFileEditorTest extends CsvBasePlatformTestCase {

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

    protected void disposeTextEditor(FileEditor fileEditor) {
        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        fileEditorProviders[1].disposeEditor(fileEditor);
    }

    public void testCsvFileEditorProviderIsAvailableAndHasCorrectNameAndPolicy() {
        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        assertEquals(2, fileEditorProviders.length);
        assertInstanceOf(fileEditorProviders[1], CsvFileEditorProvider.class);

        FileEditorProvider fileEditorProvider = fileEditorProviders[1];
        assertEquals(CsvFileEditorProvider.EDITOR_TYPE_ID, fileEditorProvider.getEditorTypeId());
        assertEquals(FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR, fileEditorProvider.getPolicy());

        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        csvEditorSettings.setEditorPrio(CsvEditorSettings.EditorPrio.TEXT_ONLY);
        assertEquals(FileEditorPolicy.HIDE_DEFAULT_EDITOR, fileEditorProvider.getPolicy());

        csvEditorSettings.setEditorPrio(CsvEditorSettings.EditorPrio.TEXT_FIRST);
        assertEquals(FileEditorPolicy.HIDE_DEFAULT_EDITOR, fileEditorProvider.getPolicy());
    }

    public void testCsvEditorIsTextEditorWithInitialCsvEditorSettings() {
        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        FileEditor fileEditor = fileEditorProviders[1].createEditor(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        assertInstanceOf(fileEditor, TextEditor.class);

        TextEditor textEditor = (TextEditor) fileEditor;

        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        EditorSettings editorSettings = textEditor.getEditor().getSettings();
        assertEquals(csvEditorSettings.isCaretRowShown(), editorSettings.isCaretRowShown());
        assertEquals(csvEditorSettings.isUseSoftWraps(), editorSettings.isUseSoftWraps());

        disposeTextEditor(textEditor);
    }

    private TextEditor getCurrentTextEditor() {
        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        return (TextEditor) fileEditorProviders[1].createEditor(myFixture.getProject(), myFixture.getFile().getVirtualFile());
    }

    public void testCsvEditorSettingsAreApplied() {
        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        csvEditorSettings.setCaretRowShown(false);
        csvEditorSettings.setUseSoftWraps(true);

        TextEditor textEditor = getCurrentTextEditor();

        EditorSettings editorSettings = textEditor.getEditor().getSettings();
        assertEquals(csvEditorSettings.isCaretRowShown(), editorSettings.isCaretRowShown());
        assertEquals(csvEditorSettings.isUseSoftWraps(), editorSettings.isUseSoftWraps());

        disposeTextEditor(textEditor);
    }

    public void testCsvEditorStateReadsAndWritesStates() {
        TextEditor textEditor = getCurrentTextEditor();

        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        CsvFileEditorProvider fileEditorProvider = (CsvFileEditorProvider) fileEditorProviders[1];
        Element dummy = new Element("dummy");

        FileEditorState state = fileEditorProvider.readState(dummy, this.getProject(), myFixture.getFile().getVirtualFile());
        assertInstanceOf(state, TextEditorState.class);
        textEditor.setState(state);
        fileEditorProvider.writeState(state, this.getProject(), dummy);

        disposeTextEditor(textEditor);
    }

    private static class DiffVirtualFileDummy extends DiffVirtualFile {
        public DiffVirtualFileDummy(@NotNull String name) {
            super(name);
        }

        @Override
        public DiffRequestProcessor createProcessor(@NotNull Project project) {
            return null;
        }
    }

    public void testAcceptCsvFile() {
        assertTrue(CsvFileEditorProvider.acceptCsvFile(myFixture.getProject(), new LightVirtualFile(myFixture.getFile().getName())));
        assertFalse(CsvFileEditorProvider.acceptCsvFile(myFixture.getProject(), new DiffVirtualFileDummy(myFixture.getFile().getName())));
    }
}
