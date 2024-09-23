package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Element;

import java.util.List;
import java.util.Objects;

public class CsvTableEditorProviderTest extends CsvBasePlatformTestCase {

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
        List<FileEditorProvider> fileEditorProviders = FileEditorProviderManager.getInstance().getProviderList(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        assertEquals(2, fileEditorProviders.size());
        assertInstanceOf(fileEditorProviders.get(1), CsvTableEditorProvider.class);

        FileEditorProvider fileEditorProvider = fileEditorProviders.get(1);
        assertEquals(CsvTableEditorProvider.EDITOR_TYPE_ID, fileEditorProvider.getEditorTypeId());
        assertEquals(FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR, fileEditorProvider.getPolicy());
        assertTrue(fileEditorProvider.accept(getProject(), myFixture.getFile().getVirtualFile()));

        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        csvEditorSettings.setEditorPrio(CsvEditorSettings.EditorPrio.TEXT_ONLY);
        assertEquals(FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR, fileEditorProvider.getPolicy());
        assertFalse(fileEditorProvider.accept(getProject(), myFixture.getFile().getVirtualFile()));

        csvEditorSettings.setEditorPrio(CsvEditorSettings.EditorPrio.TEXT_FIRST);
        assertEquals(FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR, fileEditorProvider.getPolicy());
        assertTrue(fileEditorProvider.accept(getProject(), myFixture.getFile().getVirtualFile()));
    }

    public void testCsvTableEditorCreatesInstanceOfCsvTableEditor() {
        List<FileEditorProvider> fileEditorProviders = FileEditorProviderManager.getInstance().getProviderList(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        FileEditorProvider fileEditorProvider = fileEditorProviders.get(1);

        FileEditor fileEditor = fileEditorProvider.createEditor(getProject(), myFixture.getFile().getVirtualFile());
        assertInstanceOf(fileEditor, CsvTableEditor.class);

        fileEditorProvider.disposeEditor(fileEditor);
    }

    public void testWriteAndReadTableEditorState() {
        List<FileEditorProvider> fileEditorProviders = FileEditorProviderManager.getInstance().getProviderList(myFixture.getProject(), myFixture.getFile().getVirtualFile());
        FileEditorProvider fileEditorProvider = fileEditorProviders.get(1);

        CsvTableEditorState editorState = new CsvTableEditorState();
        editorState.setColumnWidths(new int[]{120, 32, 9});
        editorState.setRowHeight(142);

        Element element = new Element("state");
        fileEditorProvider.writeState(editorState, getProject(), element);

        FileEditorState readState = fileEditorProvider.readState(element, getProject(), myFixture.getFile().getVirtualFile());

        assertInstanceOf(readState, CsvTableEditorState.class);

        CsvTableEditorState editorStateRead = (CsvTableEditorState) readState;
        assertTrue(Objects.deepEquals(editorState.getColumnWidths(), editorStateRead.getColumnWidths()));
        assertEquals(editorState.getRowHeight(), editorStateRead.getRowHeight());
    }
}
