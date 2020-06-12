package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Element;

import java.util.Objects;

public class CsvTableEditorProviderTest extends BasePlatformTestCase {

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

    public void testCsvTableEditorProviderIsAvailableAndHasCorrectNameAndPolicy() {
        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(getProject(), myFixture.getFile().getVirtualFile());
        assertEquals(2, fileEditorProviders.length);
        assertInstanceOf(fileEditorProviders[1], CsvTableEditorProvider.class);

        FileEditorProvider fileEditorProvider = fileEditorProviders[1];
        assertEquals(CsvTableEditorProvider.EDITOR_TYPE_ID, fileEditorProvider.getEditorTypeId());
        assertEquals(FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR, fileEditorProvider.getPolicy());
        assertEquals(true, fileEditorProvider.accept(getProject(), myFixture.getFile().getVirtualFile()));

        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        csvEditorSettings.setEditorPrio(CsvEditorSettings.EditorPrio.TEXT_ONLY);
        assertEquals(FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR, fileEditorProvider.getPolicy());
        assertEquals(false, fileEditorProvider.accept(getProject(), myFixture.getFile().getVirtualFile()));

        csvEditorSettings.setEditorPrio(CsvEditorSettings.EditorPrio.TABLE_FIRST);
        assertEquals(FileEditorPolicy.HIDE_DEFAULT_EDITOR, fileEditorProvider.getPolicy());
        assertEquals(true, fileEditorProvider.accept(getProject(), myFixture.getFile().getVirtualFile()));
    }

    public void testCsvTableEditorCreatesInstanceOfCsvTableEditor() {
        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(getProject(), myFixture.getFile().getVirtualFile());
        FileEditorProvider fileEditorProvider = fileEditorProviders[1];

        FileEditor fileEditor = fileEditorProvider.createEditor(getProject(), myFixture.getFile().getVirtualFile());
        assertInstanceOf(fileEditor, CsvTableEditor.class);

        fileEditorProvider.disposeEditor(fileEditor);
    }

    public void testWriteAndReadTableEditorState() {
        FileEditorProvider[] fileEditorProviders = FileEditorProviderManager.getInstance().getProviders(getProject(), myFixture.getFile().getVirtualFile());
        FileEditorProvider fileEditorProvider = fileEditorProviders[1];

        CsvTableEditorState editorState = new CsvTableEditorState();
        editorState.setColumnWidths(new int[]{ 120, 32, 9});
        editorState.setRowLines(5);
        editorState.setShowInfoPanel(false);

        Element element = new Element("state");
        fileEditorProvider.writeState(editorState, getProject(), element);

        FileEditorState readState = fileEditorProvider.readState(element, getProject(), myFixture.getFile().getVirtualFile());

        assertInstanceOf(readState, CsvTableEditorState.class);

        CsvTableEditorState editorStateRead = (CsvTableEditorState)readState;
        assertTrue(Objects.deepEquals(editorState.getColumnWidths(), editorStateRead.getColumnWidths()));
        assertEquals(editorState.getRowLines(), editorStateRead.getRowLines());
        assertEquals(editorState.showInfoPanel(), editorStateRead.showInfoPanel());
    }
}
