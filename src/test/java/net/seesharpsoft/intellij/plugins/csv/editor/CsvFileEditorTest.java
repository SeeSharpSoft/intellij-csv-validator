package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;

public class CsvFileEditorTest extends LightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCsvFileEditorProviderIsAvailableAndHasCorrectNameAndPolicy() {
        myFixture.configureByFiles("AnyFile.csv");

        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        assertEquals(1, fileEditorProviders.length);
        assertInstanceOf(fileEditorProviders[0], CsvFileEditorProvider.class);

        FileEditorProvider fileEditorProvider = fileEditorProviders[0];
        assertEquals("csv-text-editor", fileEditorProvider.getEditorTypeId());
        assertEquals(FileEditorPolicy.HIDE_DEFAULT_EDITOR, fileEditorProvider.getPolicy());
    }

    private void disposeTextEditor(FileEditor fileEditor) {
        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        fileEditorProviders[0].disposeEditor(fileEditor);
    }

    public void testCsvEditorIsTextEditorWithInitialCsvEditorSettings() {
        myFixture.configureByFiles("AnyFile.csv");

        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        FileEditor fileEditor = fileEditorProviders[0].createEditor(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        assertInstanceOf(fileEditor, TextEditor.class);

        TextEditor textEditor = (TextEditor)fileEditor;

        CsvEditorSettingsExternalizable csvEditorSettingsExternalizable = CsvEditorSettingsExternalizable.getInstance();
        EditorSettings editorSettings = textEditor.getEditor().getSettings();
        assertEquals(csvEditorSettingsExternalizable.isCaretRowShown(), editorSettings.isCaretRowShown());
        assertEquals(csvEditorSettingsExternalizable.isUseSoftWraps(), editorSettings.isUseSoftWraps());

        disposeTextEditor(textEditor);
    }

    private TextEditor getCurrentTextEditor() {
        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        return (TextEditor)fileEditorProviders[0].createEditor(myFixture.getProject(), myFixture.getFile().getVirtualFile());
    }

    public void testCsvEditorSettingsAreApplied() {
        myFixture.configureByFiles("AnyFile.csv");

        CsvEditorSettingsExternalizable csvEditorSettingsExternalizable = CsvEditorSettingsExternalizable.getInstance();
        csvEditorSettingsExternalizable.setCaretRowShown(false);
        csvEditorSettingsExternalizable.setUseSoftWraps(true);

        TextEditor textEditor = getCurrentTextEditor();

        EditorSettings editorSettings = textEditor.getEditor().getSettings();
        assertEquals(csvEditorSettingsExternalizable.isCaretRowShown(), editorSettings.isCaretRowShown());
        assertEquals(csvEditorSettingsExternalizable.isUseSoftWraps(), editorSettings.isUseSoftWraps());

        disposeTextEditor(textEditor);
    }

}
