package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.daemon.impl.IdentifierHighlighterPassFactory;
import com.intellij.find.EditorSearchSession;
import com.intellij.find.FindModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.ComponentUtil;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvFileEditorProvider;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettingsProvider;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class CsvColumnHighlighterTest extends CsvBasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/highlighter";
    }

    private void assertHighlightedTexts(RangeHighlighter[] rangeHighlighters, String... texts) {
        assertSize(texts.length, rangeHighlighters);
        List<String> needles = new ArrayList<>(Arrays.asList(texts));
        for (RangeHighlighter rangeHighlighter : rangeHighlighters) {
            needles.removeIf(text -> text.equals(rangeHighlighter.getDocument().getText(TextRange.create(rangeHighlighter.getStartOffset(), rangeHighlighter.getEndOffset()))));
        }
        assertSize(0, needles);
    }

    private void assertFixtureColumnHighlights(String fileName, String... texts) throws ReflectiveOperationException {
        myFixture.configureByFile(fileName);
        Editor source = myFixture.getEditor();
        withTextEditor(true, editor -> {
            editor.getCaretModel().moveToOffset(source.getCaretModel().getOffset());
            editor.getSelectionModel().setSelection(
                    source.getSelectionModel().getSelectionStart(), source.getSelectionModel().getSelectionEnd());
            waitForAutomaticHighlighting(editor);
            assertHighlightedTexts(getColumnHighlights(editor), texts);
        });
    }

    public void testPartialSelectionHighlightsWholeFields() throws ReflectiveOperationException {
        assertFixtureColumnHighlights("HighlightUsagesTestData01.csv", " Header 2", " Value 2");
    }

    public void testSelectionAtEndOfHeaderHighlightsColumn() throws ReflectiveOperationException {
        assertFixtureColumnHighlights("HighlightUsagesTestData02.csv", " Header 2", " Value 2");
    }

    public void testEmptyFieldsAreNotHighlighted() throws ReflectiveOperationException {
        assertFixtureColumnHighlights("HighlightUsagesTestData03.csv", "Header 1", "Value 1");
    }

    public void testWhitespaceSelectionHighlightsFieldColumn() throws ReflectiveOperationException {
        assertFixtureColumnHighlights("HighlightUsagesTestData04.csv", " Value 3");
    }

    public void testSelectionOfSeparatorHighlightsFollowingColumn() throws ReflectiveOperationException {
        assertFixtureColumnHighlights("HighlightUsagesTestData05.csv", " Header 2", " Value 2");
    }

    public void testSelectionAtEndOfFileHighlightsColumn() throws ReflectiveOperationException {
        assertFixtureColumnHighlights("HighlightUsagesTestData06.csv", " Value 3");
    }

    public void testEmptySelectionDoesNotHighlightColumn() throws ReflectiveOperationException {
        assertFixtureColumnHighlights("HighlightUsagesTestData07.csv");
    }

    public void testCaretWithoutSelectionDoesNotHighlightColumn() throws ReflectiveOperationException {
        assertFixtureColumnHighlights("HighlightUsagesTestData08.csv");
    }

    public void testFindSelectionDoesNotHighlightColumn() throws ReflectiveOperationException {
        assertSearchSelectionDoesNotHighlightColumn("highlight.csv", "left,right\none,two", false);
    }

    public void testOpeningFindClearsExistingColumnHighlights() throws ReflectiveOperationException {
        assertOpeningSearchClearsColumnHighlights("highlight.csv", ",", false);
    }

    public void testOpeningReplaceClearsExistingColumnHighlights() throws ReflectiveOperationException {
        assertOpeningSearchClearsColumnHighlights("highlight.csv", ",", true);
    }

    public void testOpeningFindClearsExistingTsvColumnHighlights() throws ReflectiveOperationException {
        assertOpeningSearchClearsColumnHighlights("highlight.tsv", "\t", false);
    }

    public void testOpeningFindClearsExistingPsvColumnHighlights() throws ReflectiveOperationException {
        assertOpeningSearchClearsColumnHighlights("highlight.psv", "|", false);
    }

    private void assertOpeningSearchClearsColumnHighlights(String fileName, String separator, boolean replace)
            throws ReflectiveOperationException {
        myFixture.configureByText(fileName, "left,middle,right\nalpha,beta,alpha\nbeta,gamma,beta".replace(",", separator));
        withTextEditor(true, editor -> {
            assertAutomaticColumnSelection(editor, "beta");
            FindModel findModel = new FindModel();
            findModel.setGlobal(true);
            findModel.setReplaceState(replace);
            EditorSearchSession session = EditorSearchSession.start(editor, findModel, getProject());
            try {
                waitForAutomaticHighlighting(editor);
                assertEquals("beta", editor.getSelectionModel().getSelectedText());
                assertEquals("Opening Find or Replace must clear existing column highlights",
                        0, getColumnHighlights(editor).length);

                editor.getSelectionModel().removeSelection();
                waitForAutomaticHighlighting(editor);
                assertEmpty(getColumnHighlights(editor));
            } finally {
                session.close();
            }
            assertAutomaticColumnSelection(editor, "beta");
        });
    }

    public void testOpeningFindCancelsPendingColumnHighlights() throws ReflectiveOperationException {
        myFixture.configureByText("highlight.csv", "left,middle,right\nalpha,beta,alpha\nbeta,gamma,beta");
        withTextEditor(true, editor -> {
            int start = editor.getDocument().getText().indexOf("beta");
            editor.getCaretModel().moveToOffset(start + 4);
            editor.getSelectionModel().setSelection(start, start + 4);
            FindModel findModel = new FindModel();
            findModel.setGlobal(true);
            EditorSearchSession session = EditorSearchSession.start(editor, findModel, getProject());
            try {
                waitForAutomaticHighlighting(editor);
                assertEquals("beta", editor.getSelectionModel().getSelectedText());
                assertEmpty(getColumnHighlights(editor));
            } finally {
                session.close();
            }
        });
    }

    public void testAutomaticSelectionHighlightsFieldsInSameColumn() throws ReflectiveOperationException {
        assertAutomaticColumnHighlighting("automatic.csv", ",", "beta");
    }

    public void testAutomaticPartialSelectionHighlightsFieldsInSameColumn() throws ReflectiveOperationException {
        assertAutomaticColumnHighlighting("automatic.csv", ",", "be");
    }

    public void testAutomaticSelectionHighlightsTsvColumn() throws ReflectiveOperationException {
        assertAutomaticColumnHighlighting("automatic.tsv", "\t", "beta");
    }

    public void testAutomaticSelectionHighlightsPsvColumn() throws ReflectiveOperationException {
        assertAutomaticColumnHighlighting("automatic.psv", "|", "beta");
    }

    public void testColumnHighlightingRequiresReopeningEditor() throws ReflectiveOperationException {
        assertColumnHighlightingRequiresReopeningEditor("setting.csv", ",");
    }

    public void testTsvColumnHighlightingRequiresReopeningEditor() throws ReflectiveOperationException {
        assertColumnHighlightingRequiresReopeningEditor("setting.tsv", "\t");
    }

    public void testPsvColumnHighlightingRequiresReopeningEditor() throws ReflectiveOperationException {
        assertColumnHighlightingRequiresReopeningEditor("setting.psv", "|");
    }

    public void testConfiguredRowLimitSupportsUnlimitedFiniteAndZeroBudgets() throws ReflectiveOperationException {
        StringBuilder content = new StringBuilder();
        for (int row = 0; row < 1005; row++) {
            if (row > 0) content.append('\n');
            content.append("row").append(row).append(",value").append(row);
        }
        myFixture.configureByText("limit.csv", content.toString());
        withTextEditor(true, editor -> {
            CsvEditorSettings settings = CsvEditorSettings.getInstance();
            settings.setMaxHighlightingRows(-1);
            selectText(editor, "value500");
            assertEquals("Unlimited highlighting must include rows beyond the former hardcoded limit",
                    1005, getColumnHighlights(editor).length);

            settings.setMaxHighlightingRows(3);
            selectText(editor, "value500");
            assertHighlightedTexts(getColumnHighlights(editor), "value499", "value500", "value501");

            settings.setMaxHighlightingRows(1);
            selectText(editor, "value500");
            assertHighlightedTexts(getColumnHighlights(editor), "value500");

            settings.setMaxHighlightingRows(0);
            selectText(editor, "value500");
            assertEmpty(getColumnHighlights(editor));
        });
    }

    public void testFiniteRowLimitCountsCsvRecordsAcrossFileBoundaries() throws ReflectiveOperationException {
        assertFiniteRowLimitCountsRecords("limit.csv", ",");
    }

    public void testFiniteRowLimitCountsTsvRecordsAcrossFileBoundaries() throws ReflectiveOperationException {
        assertFiniteRowLimitCountsRecords("limit.tsv", "\t");
    }

    public void testFiniteRowLimitCountsPsvRecordsAcrossFileBoundaries() throws ReflectiveOperationException {
        assertFiniteRowLimitCountsRecords("limit.psv", "|");
    }

    private void assertFiniteRowLimitCountsRecords(String fileName, String separator) throws ReflectiveOperationException {
        myFixture.configureByText(fileName, ("row0,value0\nrow1,\"value\n1\"\nrow2,value2\nrow3,value3\n"
                + "row4,value4\nrow5,value5\nrow6,value6").replace(",", separator));
        withTextEditor(true, editor -> {
            CsvEditorSettings settings = CsvEditorSettings.getInstance();
            settings.setMaxHighlightingRows(5);
            selectText(editor, "value0");
            assertHighlightedTexts(getColumnHighlights(editor), "value0", "\"value\n1\"", "value2", "value3", "value4");

            selectText(editor, "value6");
            assertHighlightedTexts(getColumnHighlights(editor), "value2", "value3", "value4", "value5", "value6");

            settings.setMaxHighlightingRows(Integer.MAX_VALUE);
            selectText(editor, "value3");
            assertHighlightedTexts(getColumnHighlights(editor),
                    "value0", "\"value\n1\"", "value2", "value3", "value4", "value5", "value6");
        });
    }

    public void testChangingRowLimitDiscardsPendingColumnHighlights() throws ReflectiveOperationException {
        myFixture.configureByText("limit.csv", "left,middle,right\nalpha,beta,alpha\nbeta,gamma,beta");
        withTextEditor(true, editor -> {
            int start = editor.getDocument().getText().indexOf("beta");
            editor.getCaretModel().moveToOffset(start + 4);
            editor.getSelectionModel().setSelection(start, start + 4);
            CsvEditorSettings.getInstance().setMaxHighlightingRows(1);
            waitForAutomaticHighlighting(editor);
            assertEmpty(getColumnHighlights(editor));

            selectText(editor, "beta");
            assertHighlightedTexts(getColumnHighlights(editor), "beta");
        });
    }

    private void withTextEditor(boolean highlightColumn, Consumer<Editor> scenario) throws ReflectiveOperationException {
        withTextEditor(highlightColumn, true, scenario);
    }

    private void withTextEditor(boolean highlightColumn, boolean highlightSelectedText, Consumer<Editor> scenario)
            throws ReflectiveOperationException {
        CodeInsightSettings settings = CodeInsightSettings.getInstance();
        boolean highlightIdentifiers = settings.HIGHLIGHT_IDENTIFIER_UNDER_CARET;
        EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
        boolean highlightSelection = editorSettings.isHighlightSelectionOccurrences();
        CsvEditorSettings csvSettings = CsvEditorSettings.getInstance();
        boolean previousHighlightColumn = csvSettings.isHighlightActiveColumn();
        int previousMaxHighlightingRows = csvSettings.getMaxHighlightingRows();
        CsvFileEditorProvider provider = new CsvFileEditorProvider();
        TextEditor textEditor = null;
        try {
            csvSettings.setHighlightActiveColumn(highlightColumn);
            settings.HIGHLIGHT_IDENTIFIER_UNDER_CARET = true;
            editorSettings.setHighlightSelectionOccurrences(highlightSelectedText);
            textEditor = (TextEditor) provider.createEditor(getProject(), myFixture.getFile().getVirtualFile());
            Editor editor = textEditor.getEditor();
            ComponentUtil.markAsShowing(editor.getContentComponent(), true);
            runWithAutomaticHighlighting(() -> scenario.accept(editor));
        } finally {
            if (textEditor != null) {
                ComponentUtil.markAsShowing(textEditor.getEditor().getContentComponent(), false);
                provider.disposeEditor(textEditor);
            }
            csvSettings.setHighlightActiveColumn(previousHighlightColumn);
            csvSettings.setMaxHighlightingRows(previousMaxHighlightingRows);
            settings.HIGHLIGHT_IDENTIFIER_UNDER_CARET = highlightIdentifiers;
            editorSettings.setHighlightSelectionOccurrences(highlightSelection);
        }
    }

    private void assertColumnHighlightingRequiresReopeningEditor(String fileName, String separator) throws ReflectiveOperationException {
        myFixture.configureByText(fileName, "left,middle,right\nalpha,beta,alpha\nbeta,gamma,beta".replace(",", separator));
        withTextEditor(false, editor -> {
            int start = editor.getDocument().getText().indexOf("beta");
            editor.getCaretModel().moveToOffset(start + 4);
            editor.getSelectionModel().setSelection(start, start + 4);
            waitForAutomaticHighlighting(editor);
            assertTrue(editor.getSettings().isHighlightSelectionOccurrences());
            assertSelectedTextMatches(editor);

            applyColumnHighlighting(true);
            editor.getSelectionModel().removeSelection();
            editor.getSelectionModel().setSelection(start, start + 4);
            waitForAutomaticHighlighting(editor);
            assertTrue("An already-open editor must retain native text matching",
                    editor.getSettings().isHighlightSelectionOccurrences());
            assertSelectedTextMatches(editor);
        });

        withTextEditor(true, editor -> {
            assertAutomaticColumnSelection(editor, "beta");
            applyColumnHighlighting(false);
            editor.getSelectionModel().removeSelection();
            int start = editor.getDocument().getText().indexOf("beta");
            editor.getSelectionModel().setSelection(start, start + 4);
            waitForAutomaticHighlighting(editor);
            assertSelectedTextMatches(editor);
        });

        withTextEditor(false, false, editor -> {
            int start = editor.getDocument().getText().indexOf("beta");
            editor.getCaretModel().moveToOffset(start + 4);
            editor.getSelectionModel().setSelection(start, start + 4);
            waitForAutomaticHighlighting(editor);
            assertFalse(editor.getSettings().isHighlightSelectionOccurrences());
            assertEmpty(getNativeSelectionHighlights(editor));
        });
    }

    private void assertSelectedTextMatches(Editor editor) {
        assertHighlightedTexts(getNativeSelectionHighlights(editor), "beta", "beta");
    }

    private RangeHighlighter[] getNativeSelectionHighlights(Editor editor) {
        return Arrays.stream(editor.getMarkupModel().getAllHighlighters())
                .filter(highlighter -> EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES.equals(highlighter.getTextAttributesKey()))
                .toArray(RangeHighlighter[]::new);
    }

    private void applyColumnHighlighting(boolean enabled) {
        CsvEditorSettingsProvider provider = new CsvEditorSettingsProvider();
        try {
            provider.reset();
            JCheckBox checkbox = UIUtil.findComponentsOfType(provider.createComponent(), JCheckBox.class).stream()
                    .filter(component -> "Highlight active column".equals(component.getText()))
                    .findFirst().orElseThrow();
            checkbox.setSelected(enabled);
            provider.apply();
        } catch (ConfigurationException exception) {
            throw new AssertionError(exception);
        } finally {
            provider.disposeUIResources();
        }
    }

    private void assertAutomaticColumnHighlighting(String fileName, String separator, String selection) throws ReflectiveOperationException {
        myFixture.configureByText(fileName, "left,middle,right\nalpha,beta,alpha\nbeta,gamma,beta".replace(",", separator));
        withTextEditor(true, editor -> {
            assertSearchDoesNotAutomaticallyHighlightColumn(editor, false);
            assertSearchDoesNotAutomaticallyHighlightColumn(editor, true);
            assertAutomaticColumnSelection(editor, selection);
            assertSearchDoesNotAutomaticallyHighlightColumn(editor, false);
            assertAutomaticColumnSelection(editor, selection);
            assertSelectionClearsAndRestoresColumn(editor, selection);
            assertOnlyLatestSelectionIsHighlighted(editor);
            assertDocumentChangeUpdatesColumn(editor, separator);
            CodeInsightSettings.getInstance().HIGHLIGHT_IDENTIFIER_UNDER_CARET = false;
            editor.getSelectionModel().removeSelection();
            editor.getSelectionModel().setSelection(17, 19);
            waitForAutomaticHighlighting(editor);
            assertHighlightedTexts(getColumnHighlights(editor), "left", "alpha", "beta", "added");
        });
    }

    private void assertAutomaticColumnSelection(Editor editor, String selection) {
        selectText(editor, selection);

        assertEquals(selection, editor.getSelectionModel().getSelectedText());
        assertHighlightedTexts(getColumnHighlights(editor), "middle", "beta", "gamma");
        assertTrue(editor.getSettings().isHighlightSelectionOccurrences());
        assertHighlightedTexts(getNativeSelectionHighlights(editor), selection, selection);
    }

    private void selectText(Editor editor, String text) {
        int start = editor.getDocument().getText().indexOf(text);
        assertTrue("The selected text must exist in the fixture", start >= 0);
        editor.getSelectionModel().removeSelection();
        editor.getCaretModel().moveToOffset(start + text.length());
        editor.getSelectionModel().setSelection(start, start + text.length());
        waitForAutomaticHighlighting(editor);
    }

    private RangeHighlighter[] getColumnHighlights(Editor editor) {
        return Arrays.stream(editor.getMarkupModel().getAllHighlighters())
                .filter(highlighter -> EditorColors.WRITE_IDENTIFIER_UNDER_CARET_ATTRIBUTES.equals(highlighter.getTextAttributesKey()))
                .toArray(RangeHighlighter[]::new);
    }

    private void assertSelectionClearsAndRestoresColumn(Editor editor, String selection) {
        editor.getSelectionModel().removeSelection();
        waitForAutomaticHighlighting(editor);
        assertEmpty(getColumnHighlights(editor));

        int start = editor.getDocument().getText().indexOf("beta");
        editor.getSelectionModel().setSelection(start, start + selection.length());
        waitForAutomaticHighlighting(editor);
        assertHighlightedTexts(getColumnHighlights(editor), "middle", "beta", "gamma");
    }

    private void assertOnlyLatestSelectionIsHighlighted(Editor editor) {
        editor.getSelectionModel().removeSelection();
        editor.getCaretModel().moveToOffset(26);
        editor.getSelectionModel().setSelection(24, 26);
        editor.getSelectionModel().removeSelection();
        editor.getCaretModel().moveToOffset(19);
        editor.getSelectionModel().setSelection(17, 19);
        waitForAutomaticHighlighting(editor);
        assertHighlightedTexts(getColumnHighlights(editor), "left", "alpha", "beta");
    }

    private void assertDocumentChangeUpdatesColumn(Editor editor, String separator) {
        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                editor.getDocument().insertString(editor.getDocument().getTextLength(),
                        "\nadded" + separator + "delta" + separator + "value"));
        waitForAutomaticHighlighting(editor);
        assertHighlightedTexts(getColumnHighlights(editor), "left", "alpha", "beta", "added");
    }

    private void assertSearchDoesNotAutomaticallyHighlightColumn(Editor editor, boolean replace) {
        FindModel findModel = new FindModel();
        findModel.setGlobal(true);
        findModel.setReplaceState(replace);
        EditorSearchSession session = EditorSearchSession.start(editor, findModel, getProject());
        try {
            session.initLivePreview();
            session.setTextInField("bet");
            PlatformTestUtil.waitWithEventsDispatching("Find results", () ->
                    session.hasMatches() && !session.isSearchInProgress(), 5);
            session.searchForward();
            waitForAutomaticHighlighting(editor);

            assertEquals(3, session.getSearchResults().getMatchesCount());
            assertEquals("bet", editor.getSelectionModel().getSelectedText());
            assertTrue("Find matches must remain highlighted", editor.getMarkupModel().getAllHighlighters().length > 0);
            assertEmpty(getColumnHighlights(editor));
        } finally {
            session.close();
        }
    }

    private void runWithAutomaticHighlighting(Runnable action) throws ReflectiveOperationException {
        Class<IdentifierHighlighterPassFactory> factory = IdentifierHighlighterPassFactory.class;
        Method enable;
        Object[] arguments;
        try {
            enable = factory.getMethod("doWithIdentifierHighlightingEnabled", Project.class, Runnable.class);
            arguments = new Object[]{getProject(), action};
        } catch (NoSuchMethodException exception) {
            enable = factory.getMethod("doWithHighlightingEnabled", Project.class, Disposable.class, Runnable.class);
            arguments = new Object[]{getProject(), getTestRootDisposable(), action};
        }
        enable.invoke(null, arguments);
    }

    private void waitForAutomaticHighlighting(Editor editor) {
        Class<IdentifierHighlighterPassFactory> factory = IdentifierHighlighterPassFactory.class;
        try {
            try {
                factory.getMethod("waitForIdentifierHighlighting", Editor.class).invoke(null, editor);
            } catch (NoSuchMethodException exception) {
                factory.getMethod("waitForIdentifierHighlighting").invoke(null);
            }
            NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    public void testReplaceSelectionDoesNotHighlightColumn() throws ReflectiveOperationException {
        assertSearchSelectionDoesNotHighlightColumn("highlight.csv", "left,right\none,two", true);
    }

    public void testFindSelectionDoesNotHighlightTsvColumn() throws ReflectiveOperationException {
        assertSearchSelectionDoesNotHighlightColumn("highlight.tsv", "left\tright\none\ttwo", false);
    }

    public void testFindSelectionDoesNotHighlightPsvColumn() throws ReflectiveOperationException {
        assertSearchSelectionDoesNotHighlightColumn("highlight.psv", "left|right\none|two", false);
    }

    private void assertSearchSelectionDoesNotHighlightColumn(String fileName, String content, boolean replace) throws ReflectiveOperationException {
        myFixture.configureByText(fileName, content);
        withTextEditor(true, editor -> {
            FindModel findModel = new FindModel();
            findModel.setGlobal(true);
            findModel.setReplaceState(replace);
            EditorSearchSession session = EditorSearchSession.start(editor, findModel, getProject());
            try {
                session.initLivePreview();
                session.setTextInField("tw");
                PlatformTestUtil.waitWithEventsDispatching("Find results", () ->
                        session.hasMatches() && !session.isSearchInProgress(), 5);
                session.searchForward();
                waitForAutomaticHighlighting(editor);
                assertEquals(1, session.getSearchResults().getMatchesCount());
                assertEquals("tw", editor.getSelectionModel().getSelectedText());
                assertTrue("Find matches must remain highlighted", editor.getMarkupModel().getAllHighlighters().length > 0);
                assertEmpty(getColumnHighlights(editor));
            } finally {
                session.close();
            }

            int start = editor.getDocument().getText().indexOf("two");
            editor.getSelectionModel().removeSelection();
            editor.getCaretModel().moveToOffset(start + 2);
            editor.getSelectionModel().setSelection(start, start + 2);
            waitForAutomaticHighlighting(editor);
            assertHighlightedTexts(getColumnHighlights(editor), "right", "two");
        });
    }
}
