package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffContext;
import com.intellij.diff.DiffTool;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.actions.BaseShowDiffAction;
import com.intellij.diff.actions.impl.MutableDiffRequestChain;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.SimpleDiffRequestChain;
import com.intellij.diff.impl.CacheDiffRequestChainProcessor;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.tools.util.base.TextDiffSettingsHolder;
import com.intellij.diff.tools.util.base.HighlightPolicy;
import com.intellij.diff.tools.util.base.IgnorePolicy;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.tools.simple.SimpleDiffTool;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.diff.util.DiffUserDataKeysEx;
import com.intellij.diff.util.TextDiffType;
import com.intellij.ide.DataManager;
import com.intellij.ide.impl.HeadlessDataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.DocumentsEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.diff.DiffColors;
import net.seesharpsoft.intellij.plugins.csv.highlighter.CsvTextAttributeKeys;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableEditorSwing;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.ui.dsl.builder.components.SegmentedButtonComponent;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.tsv.TsvFileType;
import net.seesharpsoft.intellij.plugins.psv.PsvFileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;

public class CsvTableDiffTest extends CsvBasePlatformTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        HeadlessDataManager.fallbackToProductionDataManager(getTestRootDisposable());
        ((HeadlessDataManager) DataManager.getInstance()).setTestDataProvider(null, getTestRootDisposable());
    }

    private final DiffContext context = new DiffContext() {
        @Override
        public Project getProject() {
            return CsvTableDiffTest.this.getProject();
        }

        @Override
        public boolean isWindowFocused() {
            return false;
        }

        @Override
        public boolean isFocusedInWindow() {
            return false;
        }

        @Override
        public void requestFocusInWindow() {
        }
    };

    public void testRegisteredTableDiffShowsBothRevisionsAsCells() {
        DocumentContent left = content("id,name\n1,Alice\n");
        DocumentContent right = content("id,name\n1,Bob\n");
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", left, right, "Before", "After");
        FrameDiffTool tool = tableTool();
        assertTrue(tool.canShow(context, request));
        FrameDiffTool.DiffViewer viewer = tool.createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();

        List<JTable> tables = tables(viewer.getComponent());
        assertEquals(2, tables.size());
        PlatformTestUtil.waitWithEventsDispatching("CSV tables populated",
                () -> tables.getFirst().getRowCount() == 2, 10);
        assertEquals("Alice", tables.getFirst().getValueAt(1, 1));
        assertEquals("Bob", tables.getLast().getValueAt(1, 1));
        assertEquals("Before", tables.getFirst().getAccessibleContext().getAccessibleName());
        assertEquals("After", tables.getLast().getAccessibleContext().getAccessibleName());
    }

    public void testSourceTitlesHaveScaledLeftPadding() {
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content("a,b"), content("a,c"), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        show(viewer);
        List<JLabel> titles = UIUtil.findComponentsOfType(viewer.getComponent(), JLabel.class).stream()
                .filter(label -> "Before".equals(label.getText()) || "After".equals(label.getText())).toList();
        assertEquals(2, titles.size());
        List<JTable> tables = tables(viewer.getComponent());
        for (int side = 0; side < tables.size(); side++) {
            JLabel title = titles.get(side);
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, tables.get(side));
            Container pane = scrollPane.getParent();
            JComponent header = (JComponent) ((BorderLayout) pane.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            assertEquals("Source headers need an outer margin from the left edge", JBUI.scale(8), header.getInsets().left);
            Point textPosition = SwingUtilities.convertPoint(title, title.getInsets().left, 0, pane);
            assertTrue("Source text must retain the outer margin and any native padding", textPosition.x >= JBUI.scale(8));
        }
    }

    public void testFileComparisonShowsFileNamesSuppliedByNativeTitleCustomizers() throws Exception {
        TempDirTestFixture files = IdeaTestFixtureFactory.getFixtureFactory().createTempDirTestFixture();
        files.setUp();
        try {
            VirtualFile selected = files.createFile("selected.csv", "id,name\n1,Alice\n");
            VirtualFile current = files.createFile("current.csv", "id,name\n1,Bob\n");
            MutableDiffRequestChain chain = BaseShowDiffAction.createMutableChainFromFiles(null, selected, current);
            ContentDiffRequest request = (ContentDiffRequest) chain.getRequests().getFirst()
                    .process(new UserDataHolderBase(), new EmptyProgressIndicator());
            assertEquals(Arrays.asList(null, null), request.getContentTitles());
            assertNotNull(request.getUserData(DiffUserDataKeysEx.EDITORS_TITLE_CUSTOMIZER));
            FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
            try {
                show(viewer, 1000, 300);
                List<String> titles = UIUtil.findComponentsOfType(viewer.getComponent(), JLabel.class).stream()
                        .map(JLabel::getText).filter(text -> text != null).toList();
                assertTrue("The selected file's name must appear above its table: " + titles,
                        titles.stream().anyMatch(text -> text.contains("selected.csv")));
                assertTrue("The editor file's name must appear above its table: " + titles,
                        titles.stream().anyMatch(text -> text.contains("current.csv")));
            } finally {
                Disposer.dispose(viewer);
            }
        } finally {
            files.tearDown();
        }
    }

    private DocumentContent content(String text) {
        return DiffContentFactory.getInstance().create(getProject(), text, CsvFileType.INSTANCE);
    }

    public void testTableDiffShowsItsOwnIconInTheDiffViewSwitcher() throws Exception {
        FrameDiffTool tool = tableTool();
        Method getIcon;
        try {
            // IDEs before 2026.3 do not show icons for diff tools.
            getIcon = FrameDiffTool.class.getMethod("getIcon");
        } catch (NoSuchMethodException exception) {
            getIcon = tool.getClass().getMethod("getIcon");
        }
        Icon icon = (Icon) getIcon.invoke(tool);
        assertSame(CsvTableDiffTool.ICON, icon);
        assertEquals(16, icon.getIconWidth());
        assertEquals(16, icon.getIconHeight());
        assertNotNull(CsvTableDiffTool.class.getResource("/media/icons/actions/show-table-diff_dark.svg"));
    }

    public void testInsertedRowsStayAlignedAndOnlyChangedCellsAreHighlighted() {
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id,name\n1,Alice\n2,Bob\n"),
                content("id,name\n0,Zoe\n1,Alice\n2,Robert\n"), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("Inserted CSV row aligned",
                () -> tables.getLast().getRowCount() == 4, 10);
        JTable left = tables.getFirst();
        JTable right = tables.getLast();
        assertEquals(4, left.getRowCount());
        assertEquals("", left.getValueAt(1, 0));
        assertEquals("0", right.getValueAt(1, 0));
        assertEquals("Alice", left.getValueAt(2, 1));
        assertEquals("Alice", right.getValueAt(2, 1));
        Color unchanged = right.prepareRenderer(right.getCellRenderer(2, 1), 2, 1).getBackground();
        Color changed = right.prepareRenderer(right.getCellRenderer(3, 1), 3, 1).getBackground();
        assertFalse(unchanged.equals(changed));
        assertEquals(unchanged, right.prepareRenderer(right.getCellRenderer(3, 0), 3, 0).getBackground());
    }

    public void testRowNumbersOfBothSidesAreShownNextToEachOtherBetweenTheApplyArrows() {
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                editable("id,name\n1,Alice\n2,\"Bob\nSmith\"\n"),
                editable("id,name\n0,Zoe\n1,Alice\n2,\"Bob\nSmith\"\n"), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV rows displayed", () -> tables.getLast().getRowCount() == 4, 10);
        show(viewer, 600, 480);
        CsvTableDiffDivider divider = divider(viewer);
        Rectangle applyToRight = divider.applyArrowBounds(1, 1);
        Rectangle applyToLeft = divider.applyArrowBounds(1, 0);
        List<List<String>> expectedRowNumbers = List.of(List.of("1", "", "2", "3"), List.of("1", "2", "3", "4"));
        for (int side = 0; side < 2; side++) {
            JTable table = tables.get(side);
            List<String> labels = new ArrayList<>();
            for (int row = 0; row < table.getRowCount(); row++) {
                CsvTableDiffDivider.RowNumber number = divider.rowNumber(side, row);
                labels.add(number.text());
                Rectangle cell = SwingUtilities.convertRectangle(table, table.getCellRect(row, 0, true), divider);
                assertEquals("Row numbers must align with their rows", cell.y, number.bounds().y);
                assertEquals("Row numbers must align with their rows", cell.height, number.bounds().height);
                Rectangle other = divider.rowNumber(1 - side, row).bounds();
                if (side == 0) {
                    assertTrue("Left numbers must follow the apply-to-right arrow", applyToRight.getMaxX() <= number.bounds().x);
                    assertTrue("Left numbers must precede the right numbers", number.bounds().getMaxX() <= other.x);
                } else {
                    assertTrue("Right numbers must precede the apply-to-left arrow", number.bounds().getMaxX() <= applyToLeft.x);
                }
            }
            assertEquals(expectedRowNumbers.get(side), labels);
        }
    }

    public void testDraggingTheMiddleOfTheDividerChangesTheWidthOfBothTables() {
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id,v\n1,a\n"), content("id,v\n1,b\n"), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV rows displayed", () -> tables.getLast().getRowCount() == 2, 10);
        show(viewer, 800, 240);
        CsvTableDiffDivider divider = divider(viewer);
        int handleStart = (int) divider.rowNumber(0, 1).bounds().getMaxX();
        int handleEnd = divider.rowNumber(1, 1).bounds().x;
        assertTrue("The middle of the divider must be an empty area of about 20px, was " + (handleEnd - handleStart),
                Math.abs(handleEnd - handleStart - JBUI.scale(20)) <= JBUI.scale(2));
        int middle = (handleStart + handleEnd) / 2;
        int y = divider.getHeight() / 2;
        divider.dispatchEvent(new MouseEvent(divider, MouseEvent.MOUSE_MOVED, 0, 0, middle, y, 0, false, MouseEvent.NOBUTTON));
        assertEquals(Cursor.E_RESIZE_CURSOR, divider.getCursor().getType());

        int leftWidth = paneWidth(tables.getFirst());
        int rightWidth = paneWidth(tables.getLast());
        int dividerX = divider.getX();
        dragDivider(divider, middle, y, 100);
        layout(viewer.getComponent());
        assertEquals(dividerX + 100, divider.getX());
        assertEquals(leftWidth + 100, paneWidth(tables.getFirst()));
        assertEquals(rightWidth - 100, paneWidth(tables.getLast()));

        dragDivider(divider, middle, y, 2000);
        layout(viewer.getComponent());
        assertTrue("Neither table may be dragged out of view", paneWidth(tables.getLast()) >= JBUI.scale(50));
    }

    private static void dragDivider(CsvTableDiffDivider divider, int x, int y, int distance) {
        divider.dispatchEvent(new MouseEvent(divider, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
        divider.dispatchEvent(new MouseEvent(divider, MouseEvent.MOUSE_DRAGGED, 0, InputEvent.BUTTON1_DOWN_MASK, x + distance, y, 1, false, MouseEvent.BUTTON1));
        divider.dispatchEvent(new MouseEvent(divider, MouseEvent.MOUSE_RELEASED, 0, 0, x + distance, y, 1, false, MouseEvent.BUTTON1));
    }

    private static int paneWidth(JTable table) {
        return SwingUtilities.getAncestorOfClass(JScrollPane.class, table).getParent().getWidth();
    }

    public void testThinRowHeaderMarksChangedRowsAndSelectsWholeRowsWithoutNumbers() {
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id,name\n1,Alice\n2,\"Bob\nSmith\"\n"),
                content("id,name\n0,Zoe\n1,Alice\n2,\"Bob\nSmith\"\n"), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV rows displayed", () -> tables.getLast().getRowCount() == 4, 10);
        viewer.getComponent().setBounds(0, 0, 400, 240);
        for (JTable table : tables) {
            for (int column = 0; column < table.getColumnCount(); column++) {
                table.getColumnModel().getColumn(column).setPreferredWidth(300);
                table.getColumnModel().getColumn(column).setWidth(300);
            }
        }
        layout(viewer.getComponent());
        for (int side = 0; side < 2; side++) {
            JTable table = tables.get(side);
            assertEquals("Row markers must not be a scrollable data column", 2, table.getColumnCount());
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
            assertNotNull("Row markers must be shown in the fixed row header", scrollPane.getRowHeader());
            assertTrue("The row header must be thin", scrollPane.getRowHeader().getWidth() > 0
                    && scrollPane.getRowHeader().getWidth() <= JBUI.scale(6));
            JTable rowMarkers = (JTable) scrollPane.getRowHeader().getView();
            int markerWidth = scrollPane.getRowHeader().getWidth();
            assertEquals("The marker column must exactly fill its thin header",
                    markerWidth, rowMarkers.getColumnModel().getColumn(0).getWidth());
            JComponent corner = (JComponent) scrollPane.getCorner(JScrollPane.UPPER_LEFT_CORNER);
            int edge = corner.getWidth() - 1;
            corner.dispatchEvent(new MouseEvent(corner, MouseEvent.MOUSE_MOVED, 0, 0, edge, 5, 0, false, MouseEvent.NOBUTTON));
            assertFalse("The marker column must not offer resizing", corner.getCursor().getType() == Cursor.E_RESIZE_CURSOR);
            corner.dispatchEvent(new MouseEvent(corner, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, edge, 5, 1, false, MouseEvent.BUTTON1));
            corner.dispatchEvent(new MouseEvent(corner, MouseEvent.MOUSE_DRAGGED, 0, InputEvent.BUTTON1_DOWN_MASK, edge + 40, 5, 1, false, MouseEvent.BUTTON1));
            corner.dispatchEvent(new MouseEvent(corner, MouseEvent.MOUSE_RELEASED, 0, 0, edge + 40, 5, 1, false, MouseEvent.BUTTON1));
            layout(viewer.getComponent());
            assertEquals("Dragging the marker header must not resize it", markerWidth, scrollPane.getRowHeader().getWidth());
            assertEquals(markerWidth, rowMarkers.getColumnModel().getColumn(0).getWidth());
            for (int row = 0; row < rowMarkers.getRowCount(); row++) {
                assertEquals("Row numbers are shown in the divider instead", "", String.valueOf(rowMarkers.getValueAt(row, 0)));
                assertEquals("Row markers must align with their rows", table.getRowHeight(row), rowMarkers.getRowHeight(row));
            }
            assertEquals(TextDiffType.INSERTED.getMarkerColor(null), rowMarker(rowMarkers, 1));
            assertEquals("Unchanged rows must not be marked", rowMarkers.getBackground(), rowMarker(rowMarkers, 2));

            scrollPane.getViewport().setViewPosition(new Point(200, 0));
            assertEquals(new Point(0, 0), scrollPane.getRowHeader().getViewPosition());

            rowMarkers.changeSelection(3, 0, false, false);
            assertEquals(3, table.getSelectedRow());
            assertArrayEquals(new int[]{0, 1}, table.getSelectedColumns());
            rowMarkers.clearSelection();
        }
    }

    private static Color rowMarker(JTable rowMarkers, int row) {
        return rowMarkers.prepareRenderer(rowMarkers.getCellRenderer(row, 0), row, 0).getBackground();
    }

    public void testRowNumbersInChangeBandsAreReadable() {
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id,v\n1,a\n2,b\n3,c\n4,d\n"), content("id,v\n0,new\n1,a\n2,changed\n3,c\n"), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV differences displayed", () -> tables.getLast().getRowCount() == 6, 10);
        show(viewer, 600, 480);
        CsvTableDiffDivider divider = divider(viewer);
        Map<Integer, TextDiffType> changedRows = Map.of(1, TextDiffType.INSERTED, 3, TextDiffType.MODIFIED, 5, TextDiffType.DELETED);
        changedRows.forEach((row, type) -> {
            for (int side = 0; side < 2; side++) {
                CsvTableDiffDivider.RowNumber number = divider.rowNumber(side, row);
                if (number.text().isEmpty()) continue;
                double contrast = contrast(number.color(), type.getColor(null));
                assertTrue("Row number " + number.text() + " must be readable on its " + type + " band, contrast " + contrast,
                        contrast >= 4.5);
            }
        });
    }

    /**
     * WCAG contrast ratio between two colors.
     */
    private static double contrast(Color first, Color second) {
        double lighter = Math.max(luminance(first), luminance(second));
        double darker = Math.min(luminance(first), luminance(second));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double luminance(Color color) {
        return 0.2126 * channel(color.getRed()) + 0.7152 * channel(color.getGreen()) + 0.0722 * channel(color.getBlue());
    }

    private static double channel(int value) {
        double srgb = value / 255.0;
        return srgb <= 0.03928 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
    }

    private DocumentContent editable(String text) {
        return DiffContentFactory.getInstance().createEditable(getProject(), text, CsvFileType.INSTANCE);
    }

    public void testCellEditsUpdateOriginalDocumentAndCanBeUndone() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(), "id,name\n1,Bob\n", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id,name\n1,Alice\n"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("Editable CSV cells populated", () -> table.getRowCount() == 2, 10);
        assertTrue(table.isCellEditable(1, 1));
        table.setValueAt("Carol, \"C\"\nSmith", 1, 1);
        assertEquals("id,name\n1,\"Carol, \"\"C\"\"\nSmith\"\n", right.getDocument().getText());
        FileEditor editor = DataManager.getInstance().getDataContext(table).getData(PlatformDataKeys.FILE_EDITOR);
        assertNotNull(editor);
        UndoManager.getInstance(getProject()).undo(editor);
        assertEquals("id,name\n1,Bob\n", right.getDocument().getText());
        UndoManager.getInstance(getProject()).redo(editor);
        assertEquals("id,name\n1,\"Carol, \"\"C\"\"\nSmith\"\n", right.getDocument().getText());
    }

    public void testDiffFileEditorsExposeOriginalFilesEvenForReadOnlyRevisions() {
        DocumentContent left = content("id,name\n1,Alice\n");
        var file = myFixture.addFileToProject("current.csv", "id,name\n1,Bob\n");
        DocumentContent right = DiffContentFactory.getInstance().createDocument(getProject(), file.getVirtualFile());
        assertNotNull(right);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", left, right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        List<JTable> tables = tables(viewer.getComponent());
        FileEditor leftEditor = DataManager.getInstance().getDataContext(tables.getFirst()).getData(PlatformDataKeys.FILE_EDITOR);
        FileEditor rightEditor = DataManager.getInstance().getDataContext(tables.getLast()).getData(PlatformDataKeys.FILE_EDITOR);
        assertNotNull(leftEditor);
        assertNotNull(rightEditor);
        assertEquals(0, ((DocumentsEditor) leftEditor).getDocuments().length);
        var revisionFile = FileDocumentManager.getInstance().getFile(left.getDocument());
        assertNotNull(revisionFile);
        assertSame(revisionFile, leftEditor.getFile());
        assertSame(file.getVirtualFile(), rightEditor.getFile());
    }

    public void testJumpToSourceOpensTheSelectedCellWhenComparingWithClipboard() {
        PsiFile file = myFixture.addFileToProject("source.csv", "id,value\n1,\"two\nlines\"\n2,target\n");
        DocumentContent source = DiffContentFactory.getInstance().createDocument(getProject(), file.getVirtualFile());
        assertNotNull(source);
        SimpleDiffRequest request = new SimpleDiffRequest("Clipboard comparison",
                content("id,value\n1,\"two\nlines\"\n2,clipboard\n"), source, "Clipboard", "Source");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV source cells displayed", () -> table.getRowCount() == 3, 10);
        table.changeSelection(2, 1, false, false);
        assertJumpToSource(table, source, source.getDocument().getText().indexOf("target"));
    }

    public void testJumpToSourceFromClipboardUsesAlignedRecordsBehindCollapsedRows() {
        textDiffSettings().setExpandByDefault(false);
        PsiFile file = myFixture.addFileToProject("source.csv", records(Map.of()));
        DocumentContent source = DiffContentFactory.getInstance().createDocument(getProject(), file.getVirtualFile());
        assertNotNull(source);
        String clipboard = records(Map.of(17, "clipboard")).replace("15,r15\n", "15,r15\n99,inserted\n");
        SimpleDiffRequest request = new SimpleDiffRequest("Clipboard comparison",
                content(clipboard), source, "Clipboard", "Source");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getFirst();
        PlatformTestUtil.waitWithEventsDispatching("Collapsed CSV source displayed", () -> table.getRowCount() > 0, 10);
        int selected = -1;
        int inserted = -1;
        for (int row = 0; row < table.getRowCount(); row++) {
            if ("clipboard".equals(table.getValueAt(row, 1))) selected = row;
            if ("inserted".equals(table.getValueAt(row, 1))) inserted = row;
        }
        assertTrue("The selected record must be displayed after a collapsed range", selected > 0 && selected < 17);
        table.changeSelection(selected, 1, false, false);
        assertJumpToSource(table, source, source.getDocument().getText().indexOf("r17"));
        assertTrue(inserted >= 0);
        table.changeSelection(inserted, 1, false, false);
        assertJumpToSource(table, source, source.getDocument().getText().indexOf("r16"));
    }

    private void assertJumpToSource(JComponent component, DocumentContent source, int expectedOffset) {
        assertJumpToSource(ActionManager.getInstance().getAction("Diff.OpenInEditor"), component, source, expectedOffset);
    }

    private void assertJumpToSource(AnAction jump, JComponent component, DocumentContent source, int expectedOffset) {
        AnActionEvent event = actionEvent(component);
        jump.update(event);
        assertTrue("Jump to Source must be enabled for a file-backed CSV comparison", event.getPresentation().isEnabled());
        assertNotNull(event.getData(DiffDataKeys.NAVIGATABLE_ARRAY));
        jump.actionPerformed(event);
        FileEditorManager manager = FileEditorManager.getInstance(getProject());
        PlatformTestUtil.waitWithEventsDispatching("CSV source opened",
                () -> manager.getSelectedTextEditor() != null
                        && manager.getSelectedTextEditor().getDocument() == source.getDocument(), 10);
        Editor editor = manager.getSelectedTextEditor();
        assertNotNull(editor);
        assertEquals(expectedOffset, editor.getCaretModel().getOffset());
    }

    public void testTableDifferenceButtonsLeadTheCommonToolbarInBothLayouts() throws Exception {
        for (boolean newToolbar : List.of(false, true)) {
            PsiFile file = myFixture.addFileToProject("source-" + newToolbar + ".csv", "id,value\n1,b\n2,same\n3,d\n");
            DocumentContent source = DiffContentFactory.getInstance().createDocument(getProject(), file.getVirtualFile());
            assertNotNull(source);
            MutableDiffRequestChain chain = new MutableDiffRequestChain(
                    content("id,value\n1,a\n2,same\n3,c\n"), source);
            chain.putUserData(DiffUserDataKeysEx.DIFF_NEW_TOOLBAR, newToolbar);
            CacheDiffRequestChainProcessor processor = tableDiffProcessor(chain);
            PlatformTestUtil.waitWithEventsDispatching("CSV differences displayed", () -> {
                List<JTable> tables = tables(processor.getComponent());
                return tables.size() == 2 && tables.getLast().getRowCount() == 4;
            }, 10);
            List<AnAction> actions = toolbarButtons(processor);
            assertEquals("CSV navigation must lead the common toolbar in layout " + newToolbar,
                    List.of("Previous Difference", "Next Difference", "Jump to Source"),
                    actions.stream().limit(3).map(action -> action.getTemplatePresentation().getText()).toList());
            assertEquals("Jump to Source must not be duplicated across the toolbars", 1,
                    UIUtil.findComponentsOfType(processor.getComponent(), ActionButton.class).stream()
                            .filter(Component::isVisible)
                            .filter(button -> "Jump to Source".equals(button.getAction().getTemplatePresentation().getText()))
                            .count());
            JTable table = tables(processor.getComponent()).getLast();
            actions.get(1).actionPerformed(actionEvent(table));
            assertEquals(1, table.getSelectedRow());
            actions.get(1).actionPerformed(actionEvent(table));
            assertEquals(3, table.getSelectedRow());
            actions.getFirst().actionPerformed(actionEvent(table));
            assertEquals(1, table.getSelectedRow());
            assertJumpToSource(actions.get(2), table, source, source.getDocument().getText().indexOf("b\n"));
        }
    }

    public void testTextDiffStillHasOneWorkingNavigationPairInBothLayouts() throws Exception {
        for (boolean newToolbar : List.of(false, true)) {
            DocumentContent right = content("id,value\n1,b\n2,same\n3,d\n");
            MutableDiffRequestChain chain = new MutableDiffRequestChain(
                    content("id,value\n1,a\n2,same\n3,c\n"), right);
            chain.putUserData(DiffUserDataKeysEx.DIFF_NEW_TOOLBAR, newToolbar);
            CacheDiffRequestChainProcessor processor = diffProcessor(chain, SimpleDiffTool.INSTANCE);
            PlatformTestUtil.waitWithEventsDispatching("Text diff displayed",
                    () -> EditorFactory.getInstance().getEditors(right.getDocument(), getProject()).length > 0, 10);
            Editor editor = EditorFactory.getInstance().getEditors(right.getDocument(), getProject())[0];
            List<AnAction> actions = toolbarButtons(processor);
            assertEquals(List.of("Previous Difference", "Next Difference", "Jump to Source"),
                    actions.stream().limit(3).map(action -> action.getTemplatePresentation().getText()).toList());
            for (String name : List.of("Previous Difference", "Next Difference", "Jump to Source")) {
                assertEquals("Text diff must not acquire duplicate " + name + " buttons", 1,
                        actions.stream().filter(action -> name.equals(action.getTemplatePresentation().getText())).count());
            }
            editor.getCaretModel().moveToOffset(0);
            PlatformTestUtil.waitWithEventsDispatching("Text differences ready", () -> {
                AnActionEvent event = actionEvent(editor.getContentComponent());
                actions.get(1).update(event);
                return event.getPresentation().isEnabled();
            }, 10);
            actions.get(1).actionPerformed(actionEvent(editor.getContentComponent()));
            assertEquals(1, editor.getCaretModel().getLogicalPosition().line);
            actions.get(1).actionPerformed(actionEvent(editor.getContentComponent()));
            assertEquals(3, editor.getCaretModel().getLogicalPosition().line);
            actions.getFirst().actionPerformed(actionEvent(editor.getContentComponent()));
            assertEquals(1, editor.getCaretModel().getLogicalPosition().line);
        }
    }

    public void testTableNavigationTargetsTheCurrentFileAfterNativeFileNavigation() throws Exception {
        SimpleDiffRequest first = new SimpleDiffRequest("First file",
                content("first,v\n1,a\n2,same\n3,c\n"), content("first,v\n1,b\n2,same\n3,d\n"), "Before", "After");
        SimpleDiffRequest second = new SimpleDiffRequest("Second file",
                content("second,v\n1,e\n2,same\n3,g\n"), content("second,v\n1,f\n2,same\n3,h\n"), "Before", "After");
        CacheDiffRequestChainProcessor processor = tableDiffProcessor(new SimpleDiffRequestChain(new ArrayList<>(List.of(first, second))));
        PlatformTestUtil.waitWithEventsDispatching("First CSV diff displayed", () -> {
            List<JTable> tables = tables(processor.getComponent());
            return tables.size() == 2 && tables.getLast().getRowCount() == 4;
        }, 10);
        List<AnAction> actions = toolbarButtons(processor);
        assertEquals(List.of("Previous Difference", "Next Difference"),
                actions.stream().limit(2).map(action -> action.getTemplatePresentation().getText()).toList());
        JTable table = tables(processor.getComponent()).getLast();
        actions.get(1).actionPerformed(actionEvent(table));
        assertEquals(1, table.getSelectedRow());
        AnAction nextFile = ActionManager.getInstance().getAction("Diff.NextChange");
        AnActionEvent event = actionEvent(table);
        nextFile.update(event);
        assertTrue("Native next-file navigation must remain available", event.getPresentation().isEnabled());
        nextFile.actionPerformed(event);
        PlatformTestUtil.waitWithEventsDispatching("Second CSV diff displayed", () -> {
            List<JTable> tables = tables(processor.getComponent());
            return tables.size() == 2 && tables.getLast().getRowCount() == 4
                    && "second".equals(tables.getLast().getValueAt(0, 0));
        }, 10);
        table = tables(processor.getComponent()).getLast();
        actions = toolbarButtons(processor);
        assertEquals("CSV navigation must still lead the toolbar after switching files",
                List.of("Previous Difference", "Next Difference"),
                actions.stream().limit(2).map(action -> action.getTemplatePresentation().getText()).toList());
        actions.get(1).actionPerformed(actionEvent(table));
        assertEquals(1, table.getSelectedRow());
        actions.get(1).actionPerformed(actionEvent(table));
        assertEquals(3, table.getSelectedRow());
        actions.getFirst().actionPerformed(actionEvent(table));
        assertEquals(1, table.getSelectedRow());
    }

    public void testNativeSwapSidesSwapsContentsTitlesAndSourceNavigation() {
        PsiFile file = myFixture.addFileToProject("source.csv", "id,name\n1,Bob\n");
        DocumentContent source = DiffContentFactory.getInstance().createDocument(getProject(), file.getVirtualFile());
        assertNotNull(source);
        DocumentContent clipboard = content("id,name\n1,Alice\n");
        MutableDiffRequestChain chain = new MutableDiffRequestChain(clipboard, source);
        chain.setTitle1("Clipboard");
        chain.setTitle2("Source");
        chain.putUserData(DiffUserDataKeysEx.DIFF_NEW_TOOLBAR, false);
        CacheDiffRequestChainProcessor processor = tableDiffProcessor(chain);
        PlatformTestUtil.waitWithEventsDispatching("CSV diff populated", () -> {
            List<JTable> tables = tables(processor.getComponent());
            return tables.size() == 2 && tables.getLast().getRowCount() == 2;
        }, 10);
        List<AnAction> actions = new ArrayList<>();
        collectToolbarActions(processor.getToolbar().getActionGroup(), actions);
        AnAction swap = swapSidesAction(actions);
        AnActionEvent event = actionEvent(tables(processor.getComponent()).getLast());
        swap.update(event);
        assertTrue(event.getPresentation().isEnabledAndVisible());
        swap.actionPerformed(event);
        PlatformTestUtil.waitWithEventsDispatching("CSV sides swapped", () -> {
            List<JTable> tables = tables(processor.getComponent());
            return tables.size() == 2 && tables.getFirst().getRowCount() == 2
                    && "Bob".equals(tables.getFirst().getValueAt(1, 1));
        }, 10);
        List<JTable> tables = tables(processor.getComponent());
        assertEquals("Source", tables.getFirst().getAccessibleContext().getAccessibleName());
        assertEquals("Clipboard", tables.getLast().getAccessibleContext().getAccessibleName());
        assertEquals("Alice", tables.getLast().getValueAt(1, 1));
        assertSame(source, chain.getContent1());
        assertSame(clipboard, chain.getContent2());
        assertTrue(tables.getFirst().isCellEditable(1, 1));
        assertFalse(tables.getLast().isCellEditable(1, 1));
        AnActionEvent jump = actionEvent(tables.getFirst());
        ActionManager.getInstance().getAction("Diff.OpenInEditor").update(jump);
        assertTrue("Source navigation must follow the swapped file", jump.getPresentation().isEnabled());
    }

    public void testSwappingSidesCommitsTheActiveCell() {
        DocumentContent source = editable("id,name\n1,Bob\n");
        MutableDiffRequestChain chain = new MutableDiffRequestChain(content("id,name\n1,Alice\n"), source);
        chain.putUserData(DiffUserDataKeysEx.DIFF_NEW_TOOLBAR, false);
        CacheDiffRequestChainProcessor processor = tableDiffProcessor(chain);
        PlatformTestUtil.waitWithEventsDispatching("Editable CSV diff displayed", () -> {
            List<JTable> tables = tables(processor.getComponent());
            return tables.size() == 2 && tables.getLast().getRowCount() == 2;
        }, 10);
        JTable table = tables(processor.getComponent()).getLast();
        assertTrue(table.editCellAt(1, 1));
        textComponent(table.getEditorComponent()).setText("Carol");
        List<AnAction> actions = new ArrayList<>();
        collectToolbarActions(processor.getToolbar().getActionGroup(), actions);
        swapSidesAction(actions).actionPerformed(actionEvent(table));
        assertEquals("Swapping must not discard the active cell's value", "id,name\n1,Carol\n", source.getDocument().getText());
    }

    private static AnAction swapSidesAction(List<AnAction> actions) {
        String name = ActionManager.getInstance().getAction("SwapSidesInDiffWindow").getTemplatePresentation().getText();
        return actions.stream().filter(action -> name.equals(action.getTemplatePresentation().getText()))
                .findFirst().orElseThrow(() -> new AssertionError("The table diff must offer the native Swap Sides action"));
    }

    private CacheDiffRequestChainProcessor tableDiffProcessor(DiffRequestChain chain) {
        return diffProcessor(chain, tableTool());
    }

    private CacheDiffRequestChainProcessor diffProcessor(DiffRequestChain chain, DiffTool tool) {
        CacheDiffRequestChainProcessor processor = new CacheDiffRequestChainProcessor(getProject(), chain) {
            @Override
            protected @NotNull List<DiffTool> getToolOrderFromSettings(@NotNull List<? extends DiffTool> availableTools) {
                return List.of(tool);
            }
        };
        Disposer.register(getTestRootDisposable(), processor);
        processor.updateRequest();
        return processor;
    }

    private List<AnAction> toolbarButtons(CacheDiffRequestChainProcessor processor) throws Exception {
        JPanel root = new JPanel(new BorderLayout());
        root.add(processor.getComponent());
        root.setSize(1200, 400);
        root.addNotify();
        Disposer.register(getTestRootDisposable(), root::removeNotify);
        for (JComponent child : UIUtil.findComponentsOfType(root, JComponent.class)) {
            if (child instanceof ActionToolbar toolbar) {
                var update = toolbar.updateActionsAsync();
                PlatformTestUtil.waitWithEventsDispatching("Diff toolbar updated", update::isDone, 10);
                update.get();
            }
        }
        layout(root);
        return UIUtil.findComponentsOfType(processor.getToolbar().getComponent(), ActionButton.class)
                .stream().filter(Component::isVisible).map(ActionButton::getAction).toList();
    }

    private static void collectToolbarActions(Container component, List<AnAction> actions) {
        for (Component child : component.getComponents()) {
            if (child instanceof ActionToolbar toolbar) collectToolbarActions(toolbar.getActionGroup(), actions);
            else if (child instanceof Container container) collectToolbarActions(container, actions);
        }
    }

    private static void collectToolbarActions(ActionGroup group, List<AnAction> actions) {
        AnAction[] children = group instanceof DefaultActionGroup standard
                ? standard.getChildren(ActionManager.getInstance()) : group.getChildren(null);
        for (AnAction action : children) {
            if (action instanceof ActionGroup nested && !nested.isPopup()) collectToolbarActions(nested, actions);
            else actions.add(action);
        }
    }

    public void testFilelessDiffDocumentsHaveStableTypedFilesAndSupportUndo() {
        var factory = DiffContentFactory.getInstance();
        DocumentContent left = factory.create(null, "id\tname\n1\tAlice\n", TsvFileType.INSTANCE);
        DocumentContent right = factory.createEditable(null, "id\tname\n1\tBob\n", TsvFileType.INSTANCE);
        assertNull(FileDocumentManager.getInstance().getFile(left.getDocument()));
        assertNull(FileDocumentManager.getInstance().getFile(right.getDocument()));
        SimpleDiffRequest request = new SimpleDiffRequest("TSV comparison", left, right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        FileEditor leftEditor = DataManager.getInstance().getDataContext(tables.getFirst()).getData(PlatformDataKeys.FILE_EDITOR);
        FileEditor rightEditor = DataManager.getInstance().getDataContext(tables.getLast()).getData(PlatformDataKeys.FILE_EDITOR);
        assertNotNull(leftEditor);
        assertNotNull(rightEditor);
        var leftFile = leftEditor.getFile();
        var rightFile = rightEditor.getFile();
        assertNotNull(leftFile);
        assertNotNull(rightFile);
        assertSame(TsvFileType.INSTANCE, leftFile.getFileType());
        assertSame(TsvFileType.INSTANCE, rightFile.getFileType());
        assertSame(leftFile, leftEditor.getFile());
        assertSame(rightFile, rightEditor.getFile());
        assertNotSame(leftFile, rightFile);
        assertSame(right.getDocument(), ((DocumentsEditor) rightEditor).getDocuments()[0]);
        PlatformTestUtil.waitWithEventsDispatching("Fileless TSV displayed", () -> tables.getLast().getRowCount() == 2, 10);
        tables.getLast().setValueAt("Carol", 1, 1);
        assertEquals("id\tname\n1\tCarol\n", right.getDocument().getText());
        UndoManager.getInstance(getProject()).undo(rightEditor);
        assertEquals("id\tname\n1\tBob\n", right.getDocument().getText());
        assertSame(rightFile, rightEditor.getFile());
    }

    public void testEmptyDiffSidesExposeStableFilesForEditorState() {
        for (int emptySide = 0; emptySide < 2; emptySide++) {
            var empty = DiffContentFactory.getInstance().createEmpty();
            DocumentContent document = content("id,name\n1,Alice\n");
            SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                    emptySide == 0 ? empty : document, emptySide == 0 ? document : empty, "Before", "After");
            FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
            Disposer.register(getTestRootDisposable(), viewer);
            JTable table = tables(viewer.getComponent()).get(emptySide);
            FileEditor editor = DataManager.getInstance().getDataContext(table).getData(PlatformDataKeys.FILE_EDITOR);
            assertNotNull(editor);
            var file = editor.getFile();
            assertNotNull(file);
            assertTrue(file.isValid());
            assertSame(file, editor.getFile());
            assertSame(file, editor.getFilesToRefresh().getFirst());
            assertEquals(0, ((DocumentsEditor) editor).getDocuments().length);
            Disposer.dispose(viewer);
            assertSame(file, editor.getFile());
        }
    }

    public void testApplyingInsertedRowsUpdatesWritableSideWithoutAddingFinalLineBreak() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(), "id,name\n1,Alice", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id,name\n1,Alice\n2,Bob"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("Inserted row displayed", () -> table.getRowCount() == 3, 10);
        table.changeSelection(2, 0, false, false);
        AnAction apply = shortcutAction(viewer, "Apply Difference to Right");
        AnActionEvent event = actionEvent(table);
        apply.update(event);
        assertTrue(event.getPresentation().isEnabled());
        apply.actionPerformed(event);
        PlatformTestUtil.waitWithEventsDispatching("Inserted row applied",
                () -> "id,name\n1,Alice\n2,Bob".equals(right.getDocument().getText()), 10);
        FileEditor editor = DataManager.getInstance().getDataContext(table).getData(PlatformDataKeys.FILE_EDITOR);
        UndoManager.getInstance(getProject()).undo(editor);
        assertEquals("id,name\n1,Alice", right.getDocument().getText());
    }

    public void testDividerConnectsEachChangedRowRangeWithItsDiffColor() {
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id,v\n1,a\n2,same\n3,gone\n"), content("id,v\n1,b\n2,same\n"), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV differences displayed", () -> tables.getLast().getRowCount() == 4, 10);
        show(viewer);
        CsvTableDiffDivider divider = divider(viewer);
        Rectangle dividerBounds = SwingUtilities.convertRectangle(divider, new Rectangle(divider.getSize()), viewer.getComponent());
        Rectangle leftBounds = SwingUtilities.convertRectangle(tables.getFirst().getParent(),
                new Rectangle(tables.getFirst().getParent().getSize()), viewer.getComponent());
        Rectangle rightBounds = SwingUtilities.convertRectangle(tables.getLast().getParent(),
                new Rectangle(tables.getLast().getParent().getSize()), viewer.getComponent());
        assertTrue("The divider must lie between both tables",
                leftBounds.getMaxX() <= dividerBounds.x && dividerBounds.getMaxX() <= rightBounds.x);

        BufferedImage image = new BufferedImage(divider.getWidth(), divider.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            divider.paint(graphics);
        } finally {
            graphics.dispose();
        }
        assertEquals(TextDiffType.MODIFIED.getColor(null).getRGB(), dividerPixel(image, divider, tables.getFirst(), 1));
        assertEquals("Unchanged rows must not be connected", 0, dividerPixel(image, divider, tables.getFirst(), 2) >>> 24);
        assertEquals(TextDiffType.DELETED.getColor(null).getRGB(), dividerPixel(image, divider, tables.getFirst(), 3));
    }

    private static int dividerPixel(BufferedImage image, CsvTableDiffDivider divider, JTable table, int row) {
        Rectangle cell = SwingUtilities.convertRectangle(table, table.getCellRect(row, 0, true), divider);
        return image.getRGB(1, (int) cell.getCenterY());
    }

    public void testDividerArrowsApplyTheChangeOfTheirRowToTheWritableSide() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(),
                "id,v\n1,b\n2,same\n3,d\n", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id,v\n1,a\n2,same\n3,c\n"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV differences displayed", () -> tables.getLast().getRowCount() == 4, 10);
        show(viewer);
        CsvTableDiffDivider divider = divider(viewer);
        assertNull("Unchanged rows must not offer apply arrows", divider.applyArrowBounds(2, 1));
        assertNull("The read-only left side must not offer an apply arrow", divider.applyArrowBounds(3, 0));

        Rectangle arrow = divider.applyArrowBounds(3, 1);
        assertNotNull(arrow);
        Rectangle row = SwingUtilities.convertRectangle(tables.getFirst(), tables.getFirst().getCellRect(3, 0, true), divider);
        assertTrue("The arrow " + arrow + " must be at its changed row " + row,
                row.y <= arrow.y && arrow.getMaxY() <= row.getMaxY());
        assertTrue("The apply-to-right arrow must be next to the left table", arrow.getCenterX() < divider.getWidth() / 2.0);
        assertEquals("Apply Difference to Right", divider.getToolTipText(mouseEvent(divider, MouseEvent.MOUSE_MOVED, arrow)));

        clickApplyArrow(divider, 3, 1);
        PlatformTestUtil.waitWithEventsDispatching("Only the clicked difference applied",
                () -> "id,v\n1,b\n2,same\n3,c\n".equals(right.getDocument().getText()), 10);
    }

    public void testMalformedCsvShowsAnExplicitErrorInsteadOfAnIncompleteDiff() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(), "id,name\n1,Bob\n", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content("id,name\n1,Alice\n"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        FrameDiffTool.ToolbarComponents toolbar = viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("Valid CSV displayed", () -> table.getRowCount() == 2, 10);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> right.getDocument().setText("id,name\n1,\"unfinished"));
        JLabel status = (JLabel) toolbar.statusPanel;
        PlatformTestUtil.waitWithEventsDispatching("CSV parsing error displayed",
                () -> status.getText() != null && status.getText().contains("Cannot compare"), 10);
        assertEquals(0, table.getRowCount());
        assertEquals("id,name\n1,\"unfinished", right.getDocument().getText());
    }

    public void testCsvRevisionWithoutAHighlightFileAutoDetectsItsSeparator() {
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id;note\n1;\"A,B\"\n"), content("id;note\n1;\"C,D\"\n"), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("Semicolon CSV displayed", () -> table.getRowCount() == 2, 10);
        assertEquals(2, table.getColumnCount());
        assertEquals("C,D", table.getValueAt(1, 1));
    }

    public void testFinalLineBreakDifferenceIsVisibleAndCanBeApplied() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(), "value", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content("value\n"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("Final line break difference displayed", () -> tables.getLast().getRowCount() == 2, 10);
        assertEquals("Final line break", tables.getFirst().getValueAt(1, 0));
        assertEquals("No final line break", tables.getLast().getValueAt(1, 0));
        show(viewer);
        clickApplyArrow(divider(viewer), 1, 1);
        PlatformTestUtil.waitWithEventsDispatching("Final line break applied", () -> "value\n".equals(right.getDocument().getText()), 10);
    }

    public void testDifferenceToolbarNavigationSelectsAndRevealsChangedRows() {
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id,value\n1,a\n2,same\n3,c\n"), content("id,value\n1,b\n2,same\n3,d\n"), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV differences displayed", () -> tables.getLast().getRowCount() == 4, 10);
        AnAction next = ActionManager.getInstance().getAction("CsvTableDiff.NextDifference");
        AnAction previous = ActionManager.getInstance().getAction("CsvTableDiff.PreviousDifference");
        next.actionPerformed(actionEvent(tables.getLast()));
        assertEquals(1, tables.getFirst().getSelectedRow());
        assertEquals(1, tables.getLast().getSelectedRow());
        next.actionPerformed(actionEvent(tables.getLast()));
        assertEquals(3, tables.getLast().getSelectedRow());
        AnActionEvent event = actionEvent(tables.getLast());
        next.update(event);
        assertFalse(event.getPresentation().isEnabled());
        previous.actionPerformed(actionEvent(tables.getLast()));
        assertEquals(1, tables.getLast().getSelectedRow());
    }

    public void testDifferenceNavigationScrollsHorizontallyToTheChangedCell() {
        String header = "c1,c2,c3,c4,c5,c6,c7,c8,c9,c10\n";
        String unchanged = "a,b,c,d,e,f,g,h,i,j\n";
        String before = header + unchanged + unchanged.repeat(40) + unchanged;
        String after = header + "a,b,c,d,e,f,g,h,X,j\n" + unchanged.repeat(40) + "a,Y,c,d,e,f,g,h,i,j\n";
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content(before), content(after), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV differences displayed", () -> tables.getLast().getRowCount() == 43, 10);
        for (JTable table : tables) {
            for (int column = 0; column < table.getColumnCount(); column++) {
                table.getColumnModel().getColumn(column).setPreferredWidth(200);
                table.getColumnModel().getColumn(column).setWidth(200);
            }
        }
        viewer.getComponent().setBounds(0, 0, 600, 240);
        layout(viewer.getComponent());
        AnAction next = ActionManager.getInstance().getAction("CsvTableDiff.NextDifference");

        next.actionPerformed(actionEvent(tables.getLast()));
        assertChangedCellRevealed(tables, 1, 8);

        next.actionPerformed(actionEvent(tables.getLast()));
        assertChangedCellRevealed(tables, 42, 1);
    }

    public void testDifferenceNavigationCentersTheChangedRowVertically() {
        String unchanged = "a,b\n";
        String before = "c1,c2\n" + unchanged.repeat(80);
        String after = "c1,c2\n" + unchanged.repeat(39) + "a,X\n" + unchanged.repeat(40);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content(before), content(after), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV differences displayed", () -> tables.getLast().getRowCount() == 81, 10);
        viewer.getComponent().setBounds(0, 0, 600, 240);
        layout(viewer.getComponent());

        ActionManager.getInstance().getAction("CsvTableDiff.NextDifference").actionPerformed(actionEvent(tables.getLast()));

        for (JTable table : tables) {
            assertEquals(40, table.getSelectedRow());
            Rectangle cell = table.getCellRect(40, 1, true);
            Rectangle visible = table.getVisibleRect();
            assertTrue("Changed cell " + cell + " must be inside " + visible, visible.contains(cell));
            assertEquals("Changed row must be vertically centered in " + visible,
                    visible.getCenterY(), cell.getCenterY(), 1.0);
        }
    }

    public void testCollapsingUnchangedFragmentsShowsOneWavyRowPerHiddenRangeAndSkipsItsRowNumbers() {
        TextDiffSettingsHolder.TextDiffSettings settings = textDiffSettings();
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content(records(Map.of())), editable(records(Map.of(10, "changed"))), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        FrameDiffTool.ToolbarComponents toolbar = viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV rows displayed", () -> tables.getLast().getRowCount() == 21, 10);
        ToggleAction collapse = (ToggleAction) toolbarAction(toolbar, "Collapse Unchanged Fragments");
        AnActionEvent event = actionEvent(tables.getLast());
        collapse.update(event);
        assertTrue(event.getPresentation().isVisible());
        assertFalse("Unchanged fragments are expanded by default", Toggleable.isSelected(event.getPresentation()));

        collapse.setSelected(event, true);
        assertFalse("The setting is shared with the text diff", settings.isExpandByDefault());
        assertEquals("Records 0-5 and 15-20 are farther than 4 rows from the change", 11, tables.getLast().getRowCount());
        show(viewer, 600, 600);
        CsvTableDiffDivider divider = divider(viewer);
        List<String> expectedNumbers = List.of("", "7", "8", "9", "10", "11", "12", "13", "14", "15", "");
        for (int side = 0; side < 2; side++) {
            JTable table = tables.get(side);
            assertEquals(expectedNumbers, rowNumbers(divider, side, table.getRowCount()));
            assertEquals("6", table.getValueAt(1, 0));
            assertEquals(side == 0 ? "r10" : "changed", table.getValueAt(5, 1));
            for (int fold : new int[]{0, 10}) {
                assertEquals("Fold rows span all columns", table.getCellRect(fold, 0, true), table.getCellRect(fold, 1, true));
                assertFalse(table.isCellEditable(fold, 0));
                assertWaveSpansRow(table, table.getCellRect(fold, 0, true), table.getBackground());
                Rectangle row = SwingUtilities.convertRectangle(table, table.getCellRect(fold, 0, true), divider);
                assertWaveSpansRow(divider, new Rectangle(0, row.y, divider.getWidth(), row.height), null);
            }
        }
        assertNotNull("Changed rows still offer apply arrows", divider.applyArrowBounds(5, 1));
        assertNull("Fold rows offer no apply arrows", divider.applyArrowBounds(10, 1));

        collapse.setSelected(event, false);
        assertEquals(21, tables.getLast().getRowCount());
        assertEquals("21", divider.rowNumber(0, 20).text());

        collapse.setSelected(event, true);
        settings.setContextRange(-1);
        assertEquals("Disabled collapsing shows every row", 21, tables.getLast().getRowCount());
        collapse.update(event);
        assertFalse(event.getPresentation().isVisible());
    }

    public void testClickingAWavyRowExpandsItsRowsAndKeepsThemExpandedAfterRefresh() {
        textDiffSettings().setExpandByDefault(false);
        DocumentContent right = editable(records(Map.of(10, "changed")));
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content(records(Map.of())), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("Collapsed CSV rows displayed", () -> tables.getLast().getRowCount() == 11, 10);
        show(viewer, 600, 600);
        JTable left = tables.getFirst();
        Rectangle fold = left.getCellRect(0, 0, true);
        left.dispatchEvent(mouseEvent(left, MouseEvent.MOUSE_MOVED, fold));
        assertEquals(Cursor.HAND_CURSOR, left.getCursor().getType());
        left.dispatchEvent(mouseEvent(left, MouseEvent.MOUSE_CLICKED, fold));
        for (JTable table : tables) {
            assertEquals("Only the clicked fold expands", 16, table.getRowCount());
            assertEquals("id", table.getValueAt(0, 0));
            assertEquals("5", table.getValueAt(5, 0));
        }
        assertEquals("1", divider(viewer).rowNumber(1, 0).text());

        WriteCommandAction.runWriteCommandAction(getProject(),
                () -> right.getDocument().setText(records(Map.of(10, "changed again"))));
        PlatformTestUtil.waitWithEventsDispatching("Refreshed CSV values displayed",
                () -> "changed again".equals(tables.getLast().getValueAt(10, 1)), 10);
        assertEquals("Expanded rows stay expanded", 16, tables.getLast().getRowCount());
    }

    public void testNavigationApplyingAndEditingUseTheRecordsBehindCollapsedRows() {
        textDiffSettings().setExpandByDefault(false);
        DocumentContent right = editable(records(Map.of(3, "x", 17, "y")));
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content(records(Map.of())), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("Collapsed CSV rows displayed", () -> tables.getLast().getRowCount() == 17, 10);
        show(viewer, 600, 600);
        AnAction next = ActionManager.getInstance().getAction("CsvTableDiff.NextDifference");
        next.actionPerformed(actionEvent(tables.getLast()));
        assertEquals(3, tables.getLast().getSelectedRow());
        next.actionPerformed(actionEvent(tables.getLast()));
        assertEquals("Records 8-12 are folded into one row", 13, tables.getLast().getSelectedRow());
        assertEquals("y", tables.getLast().getValueAt(13, 1));

        clickApplyArrow(divider(viewer), 13, 1);
        PlatformTestUtil.waitWithEventsDispatching("Folded row difference applied",
                () -> records(Map.of(3, "x")).equals(right.getDocument().getText()), 10);
        PlatformTestUtil.waitWithEventsDispatching("Collapsed CSV rows refreshed", () -> tables.getLast().getRowCount() == 9, 10);
        tables.getLast().setValueAt("edited", 5, 1);
        assertEquals(records(Map.of(3, "x", 5, "edited")), right.getDocument().getText());
    }

    private TextDiffSettingsHolder.TextDiffSettings textDiffSettings() {
        TextDiffSettingsHolder.TextDiffSettings settings = new TextDiffSettingsHolder.TextDiffSettings();
        context.putUserData(TextDiffSettingsHolder.TextDiffSettings.KEY, settings);
        return settings;
    }

    private static String records(Map<Integer, String> changes) {
        StringBuilder text = new StringBuilder("id,v\n");
        for (int record = 1; record <= 20; record++) {
            text.append(record).append(',').append(changes.getOrDefault(record, "r" + record)).append('\n');
        }
        return text.toString();
    }

    private static List<String> rowNumbers(CsvTableDiffDivider divider, int side, int rows) {
        List<String> numbers = new ArrayList<>();
        for (int row = 0; row < rows; row++) numbers.add(divider.rowNumber(side, row).text());
        return numbers;
    }

    /**
     * Asserts that a line is drawn through (nearly) every pixel column of the row, excluding its bottom grid line.
     */
    private static void assertWaveSpansRow(JComponent component, Rectangle row, Color background) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            component.paint(graphics);
        } finally {
            graphics.dispose();
        }
        int left = Math.max(0, row.x);
        int right = Math.min(component.getWidth(), (int) row.getMaxX()) - 1;
        int painted = 0;
        for (int x = left; x < right; x++) {
            for (int y = row.y; y < row.getMaxY() - 1; y++) {
                int pixel = image.getRGB(x, y);
                if (background == null ? pixel >>> 24 != 0 : pixel != background.getRGB()) {
                    painted++;
                    break;
                }
            }
        }
        assertTrue("A wave must cross the row, painted " + painted + " of " + (right - left) + " columns",
                painted >= (right - left) * 9 / 10);
    }

    private static void assertChangedCellRevealed(List<JTable> tables, int row, int column) {
        for (JTable table : tables) {
            assertEquals(row, table.getSelectedRow());
            assertEquals(column, table.getSelectedColumn());
            Rectangle cell = table.getCellRect(row, column, true);
            assertTrue("Changed cell " + cell + " must be inside " + table.getVisibleRect(),
                    table.getVisibleRect().contains(cell));
        }
    }

    public void testForcedReadOnlyDiffProhibitsCellEditingApplyingAndUndo() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(), "value", CsvFileType.INSTANCE);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> right.getDocument().setText("changed"));
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content("value"), right, "Before", "After");
        request.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("Read-only CSV displayed", () -> table.getRowCount() == 1, 10);
        assertFalse(table.isCellEditable(0, 0));
        table.setValueAt("forbidden", 0, 0);
        assertEquals("changed", right.getDocument().getText());
        table.changeSelection(0, 0, false, false);
        for (String name : List.of("Apply Difference to Left", "Apply Difference to Right")) {
            AnActionEvent event = actionEvent(table);
            shortcutAction(viewer, name).update(event);
            assertFalse(event.getPresentation().isEnabled());
        }
        show(viewer);
        for (int side = 0; side < 2; side++) {
            assertNull("Read-only sides must not offer apply arrows", divider(viewer).applyArrowBounds(0, side));
        }
        FileEditor editor = DataManager.getInstance().getDataContext(table).getData(PlatformDataKeys.FILE_EDITOR);
        assertFalse("Native undo must not mutate a forced read-only diff", UndoManager.getInstance(getProject()).isUndoAvailable(editor));
    }

    public void testMultilineCellValuesAreVisibleAndPreservedWhenEdited() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(), "id,name\n1,\"Bob\nSmith\"\n", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id,name\n1,\"Alice\nSmith\"\n"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("Multiline CSV displayed", () -> table.getRowCount() == 2, 10);
        Component renderer = table.prepareRenderer(table.getCellRenderer(1, 1), 1, 1);
        assertTrue("Both lines must be rendered", renderer.getPreferredSize().height >= table.getFontMetrics(table.getFont()).getHeight() * 2);
        assertTrue("Row must fit multiline contents", table.getRowHeight(1) >= renderer.getPreferredSize().height);
        assertTrue(table.editCellAt(1, 1));
        JTextComponent text = textComponent(table.getEditorComponent());
        assertNotNull(text);
        text.setText("Carol\nJones");
        assertTrue(table.getCellEditor().stopCellEditing());
        assertEquals("id,name\n1,\"Carol\nJones\"\n", right.getDocument().getText());
    }

    private static JTextComponent textComponent(Component component) {
        if (component instanceof JTextComponent text) return text;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTextComponent text = textComponent(child);
                if (text != null) return text;
            }
        }
        return null;
    }

    public void testOnlyTwoWayCsvFamilyRequestsOfferTheTableTool() {
        FrameDiffTool tool = tableTool();
        assertFalse(tool.canShow(context, new SimpleDiffRequest("Plain text",
                DiffContentFactory.getInstance().create(getProject(), "before", PlainTextFileType.INSTANCE),
                DiffContentFactory.getInstance().create(getProject(), "after", PlainTextFileType.INSTANCE), "Before", "After")));
        assertFalse(tool.canShow(context, new SimpleDiffRequest("Three-way", List.of(content("a"), content("b"), content("c")),
                List.of("Left", "Base", "Right"))));
        assertTrue(tool.canShow(context, new SimpleDiffRequest("Added CSV",
                DiffContentFactory.getInstance().createEmpty(), content("id,name\n1,Alice\n"), "Before", "After")));
        assertTrue(tool.canShow(context, new SimpleDiffRequest("Deleted CSV",
                content("id,name\n1,Alice\n"), DiffContentFactory.getInstance().createEmpty(), "Before", "After")));
    }

    public void testApplyingCsvRowsToPsvUsesTheTargetSeparator() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(), "id|name\n1|Bob\n", PsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV/PSV comparison",
                content("id,name\n1,\"Alice; A\"\n"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("PSV displayed", () -> table.getRowCount() == 2, 10);
        show(viewer);
        clickApplyArrow(divider(viewer), 1, 1);
        PlatformTestUtil.waitWithEventsDispatching("CSV rows converted to PSV",
                () -> "id|name\n1|\"Alice; A\"\n".equals(right.getDocument().getText()), 10);
    }

    public void testTsvUsesItsFixedSeparator() {
        SimpleDiffRequest request = new SimpleDiffRequest("TSV comparison",
                DiffContentFactory.getInstance().create(getProject(), "id\tname\n1\tAlice\n", TsvFileType.INSTANCE),
                DiffContentFactory.getInstance().create(getProject(), "id\tname\n1\tBob\n", TsvFileType.INSTANCE), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("TSV displayed", () -> table.getRowCount() == 2, 10);
        assertEquals("Bob", table.getValueAt(1, 1));
    }

    public void testHistoricalRevisionUsesFileSpecificSeparatorAndEscapeWithoutReadingCurrentValues() {
        var file = myFixture.addFileToProject("attributes.csv", "id||name\n1||Current\n");
        CsvFileAttributes attributes = CsvFileAttributes.getInstance(getProject());
        attributes.setFileSeparator(file, new CsvValueSeparator("||"));
        attributes.setEscapeCharacter(file, CsvEscapeCharacter.BACKSLASH);
        var factory = DiffContentFactory.getInstance();
        SimpleDiffRequest request = new SimpleDiffRequest("History comparison",
                factory.create(getProject(), "id||name\n1||\"Old\\\"Name\"\n", file.getVirtualFile()),
                factory.create(getProject(), "id||name\n1||\"New\\\"Name\"\n", file.getVirtualFile()), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("Historical CSV displayed", () -> tables.getLast().getRowCount() == 2, 10);
        assertEquals("Old\"Name", tables.getFirst().getValueAt(1, 1));
        assertEquals("New\"Name", tables.getLast().getValueAt(1, 1));
        assertEquals("id||name\n1||Current\n", file.getText());
    }

    public void testApplyingToLeftDeletesTrailingRowsWithoutAddingALineBreak() {
        DocumentContent left = DiffContentFactory.getInstance().createEditable(getProject(), "id,name\n1,Alice\n2,Bob", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", left, content("id,name\n1,Alice"), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getFirst();
        PlatformTestUtil.waitWithEventsDispatching("Deleted row displayed", () -> table.getRowCount() == 3, 10);
        show(viewer);
        clickApplyArrow(divider(viewer), 2, 0);
        PlatformTestUtil.waitWithEventsDispatching("Trailing row deleted",
                () -> "id,name\n1,Alice".equals(left.getDocument().getText()), 10);
    }

    public void testCellEditsKeepColumnWidthsAndTheSelectedCellAfterRefresh() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(), "id,name\n1,Bob\n", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content("id,name\n1,Alice\n"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV displayed", () -> table.getRowCount() == 2, 10);
        table.getColumnModel().getColumn(1).setPreferredWidth(233);
        table.getColumnModel().getColumn(1).setWidth(233);
        table.changeSelection(1, 1, false, false);
        table.setValueAt("Bobby", 1, 1);
        PlatformTestUtil.waitWithEventsDispatching("Edited CSV refreshed", () -> "Bobby".equals(table.getValueAt(1, 1)), 10);
        assertEquals(233, table.getColumnModel().getColumn(1).getWidth());
        assertEquals(1, table.getSelectedRow());
        assertEquals(1, table.getSelectedColumn());
    }

    public void testScrollingBothTablesKeepsTheSameVisibleRowsAndColumns() {
        StringBuilder text = new StringBuilder("id,name\n");
        for (int row = 0; row < 60; row++) text.append(row).append(",value\n");
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content(text.toString()), content(text.toString()), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV rows displayed", () -> tables.getLast().getRowCount() == 61, 10);
        for (JTable table : tables) {
            for (int column = 0; column < table.getColumnCount(); column++) {
                table.getColumnModel().getColumn(column).setPreferredWidth(200);
                table.getColumnModel().getColumn(column).setWidth(200);
            }
        }
        viewer.getComponent().setBounds(0, 0, 600, 240);
        layout(viewer.getComponent());
        JScrollPane left = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, tables.getFirst());
        JScrollPane right = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, tables.getLast());
        left.getViewport().setViewPosition(new Point(20, 84));
        assertEquals(new Point(20, 84), right.getViewport().getViewPosition());
        right.getViewport().setViewPosition(new Point(30, 140));
        assertEquals(new Point(30, 140), left.getViewport().getViewPosition());
    }

    private static List<AnAction> viewerToolbarActions(FrameDiffTool.ToolbarComponents toolbar) throws IllegalAccessException {
        assertNotNull(toolbar.toolbarActions);
        List<AnAction> actions = new ArrayList<>(toolbar.toolbarActions);
        for (var field : FrameDiffTool.ToolbarComponents.class.getFields()) {
            if (!field.getName().equals("rightToolbarActions")) continue;
            for (Object action : assertInstanceOf(field.get(toolbar), List.class)) {
                actions.add(assertInstanceOf(action, AnAction.class));
            }
        }
        return actions;
    }

    public void testOptionsMenuUsesNativeSectionsAndOmitsTextOnlyOptions() throws IllegalAccessException {
        textDiffSettings();
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context,
                new SimpleDiffRequest("CSV comparison", content("a,b"), content("a,c"), "Before", "After"));
        Disposer.register(getTestRootDisposable(), viewer);
        FrameDiffTool.ToolbarComponents toolbar = viewer.init();
        assertNotNull("Diff options are also available in the viewer popup", toolbar.popupActions);
        AnAction options = viewerToolbarActions(toolbar).getLast();
        assertTrue("The rightmost action must be a popup cogwheel", options instanceof ActionGroup);
        assertTrue(((ActionGroup) options).isPopup());
        assertSame(com.intellij.icons.AllIcons.General.GearPlain, options.getTemplatePresentation().getIcon());
        assertEquals(toolbar.popupActions, Arrays.asList(((ActionGroup) options).getChildren(actionEvent(viewer.getComponent()))));
        assertEquals(Arrays.asList("Synchronize Scrolling", "Ignore differences", "None", "Trim Whitespaces",
                        "Ignore Whitespaces", "Ignore Formatting", "Highlight Differences", "Lines", "Words",
                        "Characters", "None", null, "Help"),
                toolbar.popupActions.stream().map(action -> action instanceof Separator separator
                        ? separator.getText() : action.getTemplatePresentation().getText()).toList());
        assertTrue(toolbar.popupActions.get(1) instanceof Separator);
        assertTrue(toolbar.popupActions.get(6) instanceof Separator);
    }

    public void testOptionsCanDisableScrollingSyncAndResynchronizeWhenEnabled() {
        TextDiffSettingsHolder.TextDiffSettings settings = textDiffSettings();
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context,
                new SimpleDiffRequest("CSV comparison", content(records(Map.of())), content(records(Map.of())), "Before", "After"));
        Disposer.register(getTestRootDisposable(), viewer);
        FrameDiffTool.ToolbarComponents toolbar = viewer.init();
        List<JTable> tables = tables(viewer.getComponent());
        PlatformTestUtil.waitWithEventsDispatching("CSV rows displayed", () -> tables.getLast().getRowCount() == 21, 10);
        show(viewer);
        JViewport left = ((JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, tables.getFirst())).getViewport();
        JViewport right = ((JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, tables.getLast())).getViewport();
        ToggleAction sync = (ToggleAction) toolbar.popupActions.getFirst();
        AnActionEvent event = actionEvent(tables.getLast());
        sync.setSelected(event, false);
        assertFalse(settings.isEnableSyncScroll());
        right.setViewPosition(new Point(0, 100));
        assertEquals(0, left.getViewPosition().y);
        sync.setSelected(event, true);
        assertEquals(right.getViewPosition(), left.getViewPosition());
        left.setViewPosition(new Point(0, 150));
        assertEquals(left.getViewPosition(), right.getViewPosition());
    }

    public void testOptionsCogwheelAppearsAfterSwapSidesInBothNativeToolbarLayouts() {
        for (boolean newToolbar : List.of(false, true)) {
            MutableDiffRequestChain chain = new MutableDiffRequestChain(content("id,value\n1,a\n"), content("id,value\n1,b\n"));
            chain.putUserData(DiffUserDataKeysEx.DIFF_NEW_TOOLBAR, newToolbar);
            CacheDiffRequestChainProcessor processor = tableDiffProcessor(chain);
            PlatformTestUtil.waitWithEventsDispatching("CSV diff toolbar displayed",
                    () -> tables(processor.getComponent()).size() == 2, 10);
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            List<AnAction> actions = new ArrayList<>();
            collectToolbarActions(processor.getComponent(), actions);
            AnAction options = actions.stream().filter(action -> "Options".equals(action.getTemplatePresentation().getText()))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing cogwheel in toolbar layout " + newToolbar));
            int index = actions.indexOf(options);
            assertTrue("Options must follow Swap Sides, without requiring the same toolbar",
                    actions.subList(0, index).stream()
                            .anyMatch(action -> "Swap Sides".equals(action.getTemplatePresentation().getText())));
            assertEquals("The toolbar must contain exactly one Options cogwheel", 1L,
                    actions.stream().filter(action -> "Options".equals(action.getTemplatePresentation().getText())).count());
            assertSame(com.intellij.icons.AllIcons.General.GearPlain, options.getTemplatePresentation().getIcon());
        }
    }

    public void testOptionsCogwheelIsRightOfTheDiffViewSelectorInBothNativeToolbarLayouts() {
        for (boolean newToolbar : List.of(false, true)) {
            MutableDiffRequestChain chain = new MutableDiffRequestChain(content("a"), content("b"));
            chain.putUserData(DiffUserDataKeysEx.DIFF_NEW_TOOLBAR, newToolbar);
            CacheDiffRequestChainProcessor processor = tableDiffProcessor(chain);
            PlatformTestUtil.waitWithEventsDispatching("CSV diff displayed",
                    () -> tables(processor.getComponent()).size() == 2, 10);
            JPanel root = new JPanel(new BorderLayout());
            root.add(processor.getComponent());
            root.setSize(1200, 400);
            root.addNotify();
            Disposer.register(getTestRootDisposable(), root::removeNotify);
            List<ActionToolbarImpl> toolbars = UIUtil.findComponentsOfType(root, ActionToolbarImpl.class);
            for (ActionToolbarImpl toolbar : toolbars) toolbar.updateActionsImmediately();
            layout(root);
            ActionButton options = UIUtil.findComponentsOfType(root, ActionButton.class).stream()
                    .filter(button -> "Options".equals(button.getAction().getTemplatePresentation().getText()))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing rendered cogwheel"));
            int optionsX = SwingUtilities.convertPoint(options, 0, 0, root).x;
            int viewSelectors = 0;
            for (ActionToolbarImpl toolbar : toolbars) {
                for (Component button : toolbar.getComponents()) {
                    if (!button.isVisible() || button.getWidth() == 0 || button == options) continue;
                    AnAction action;
                    if (button instanceof ActionButton actionButton) action = actionButton.getAction();
                    else if (button instanceof JComponent component
                            && component.getClientProperty(CustomComponentAction.ACTION_KEY) instanceof AnAction custom) action = custom;
                    else continue;
                    if (action instanceof CustomComponentAction) viewSelectors++;
                    int buttonX = SwingUtilities.convertPoint(button, 0, 0, root).x;
                    assertTrue("The cogwheel must be rightmost in toolbar layout " + newToolbar
                                    + ", but " + action.getClass().getSimpleName() + " is at " + buttonX
                                    + " and the cogwheel is at " + optionsX,
                            optionsX >= buttonX + button.getWidth());
                }
            }
            for (Component selector : UIUtil.findComponentsOfType(root, SegmentedButtonComponent.class)) {
                if (!selector.isVisible() || selector.getWidth() == 0) continue;
                viewSelectors++;
                int selectorRight = SwingUtilities.convertPoint(selector, selector.getWidth(), 0, root).x;
                assertTrue("The cogwheel must be right of the native view selector in toolbar layout " + newToolbar
                                + ", but the selector ends at " + selectorRight + " and the cogwheel is at " + optionsX,
                        optionsX >= selectorRight);
            }
            assertTrue("The native view selector must be included in the geometry check", viewSelectors > 0);
        }
    }

    public void testIgnoreOptionsRefreshCountsHighlightsAndApplyWithoutChangingIgnoredRecords() {
        textDiffSettings();
        DocumentContent right = editable("id,value\n1, same \n2,after\n");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context,
                new SimpleDiffRequest("CSV comparison", content("id,value\n1,same\n2,before\n"), right, "Before", "After"));
        Disposer.register(getTestRootDisposable(), viewer);
        FrameDiffTool.ToolbarComponents toolbar = viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        JLabel status = (JLabel) toolbar.statusPanel;
        PlatformTestUtil.waitWithEventsDispatching("CSV differences displayed", () -> table.getRowCount() == 3, 10);
        selectOption(toolbar, "Trim Whitespaces", table);
        PlatformTestUtil.waitWithEventsDispatching("Whitespace difference ignored",
                () -> "1 differences".equals(status.getText())
                        && renderedText(table, 0, 1).getBackground().equals(renderedText(table, 1, 1).getBackground()), 10);
        assertEquals(renderedText(table, 0, 1).getBackground(), renderedText(table, 1, 1).getBackground());
        ActionManager.getInstance().getAction("CsvTableDiff.NextDifference").actionPerformed(actionEvent(table));
        assertEquals(2, table.getSelectedRow());
        shortcutAction(viewer, "Apply Difference to Right").actionPerformed(actionEvent(table));
        PlatformTestUtil.waitWithEventsDispatching("Only meaningful change applied",
                () -> "id,value\n1, same \n2,before\n".equals(right.getDocument().getText()), 10);
        PlatformTestUtil.waitWithEventsDispatching("Comparison refreshed", () -> "0 differences".equals(status.getText()), 10);
        selectOption(toolbar, "None", table);
        PlatformTestUtil.waitWithEventsDispatching("Exact comparison restored", () -> "1 differences".equals(status.getText()), 10);
    }

    public void testIgnoreFormattingComparesDecodedFieldsButPreservesWhitespaceAndStructure() {
        TextDiffSettingsHolder.TextDiffSettings settings = textDiffSettings();
        settings.setIgnorePolicy(IgnorePolicy.FORMATTING);
        DocumentContent right = DiffContentFactory.getInstance().create(getProject(),
                "id|value\n1|\"same\"\n2|a b\n3|\"x,y\"\n#comment\n\n", PsvFileType.INSTANCE);
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context,
                new SimpleDiffRequest("CSV comparison",
                        content("id,value\n1,same\n2,ab\n3,x,y\n\"#comment\"\n"), right, "Before", "After"));
        Disposer.register(getTestRootDisposable(), viewer);
        FrameDiffTool.ToolbarComponents toolbar = viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV values displayed", () -> table.getRowCount() >= 6, 10);
        assertEquals(renderedText(table, 0, 1).getBackground(), renderedText(table, 1, 1).getBackground());
        assertFalse(renderedText(table, 0, 1).getBackground().equals(renderedText(table, 2, 1).getBackground()));
        assertFalse("Different field boundaries must not compare equal",
                renderedText(table, 0, 1).getBackground().equals(renderedText(table, 3, 1).getBackground()));
        assertFalse("Blank records and comments are data", "0 differences".equals(((JLabel) toolbar.statusPanel).getText()));
    }

    public void testWhitespaceIgnoringKeepsFieldBoundariesAndBlankRecords() {
        TextDiffSettingsHolder.TextDiffSettings settings = textDiffSettings();
        settings.setIgnorePolicy(IgnorePolicy.IGNORE_WHITESPACES);
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context,
                new SimpleDiffRequest("CSV comparison",
                        content("id,value\n1,a b\n2,a,b\n"),
                        content("id,value\n1,ab\n2,ab\n\n"), "Before", "After"));
        Disposer.register(getTestRootDisposable(), viewer);
        FrameDiffTool.ToolbarComponents toolbar = viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV rows displayed", () -> table.getRowCount() == 4, 10);
        assertEquals(renderedText(table, 0, 1).getBackground(), renderedText(table, 1, 1).getBackground());
        assertFalse(renderedText(table, 0, 1).getBackground().equals(renderedText(table, 2, 1).getBackground()));
        assertFalse("Blank records must not disappear", "0 differences".equals(((JLabel) toolbar.statusPanel).getText()));
    }

    public void testHighlightOptionsChangeInlineRangesAndClearReusedRenderer() {
        TextDiffSettingsHolder.TextDiffSettings settings = textDiffSettings();
        settings.setHighlightPolicy(HighlightPolicy.BY_WORD);
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context,
                new SimpleDiffRequest("CSV comparison", content("id,value\n1,hello cat\n2,same\n"),
                        editable("id,value\n1,hello cut\n2,same\n"), "Before", "After"));
        Disposer.register(getTestRootDisposable(), viewer);
        FrameDiffTool.ToolbarComponents toolbar = viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV rows displayed", () -> table.getRowCount() == 3, 10);
        show(viewer);
        assertInlineRange(table, 1, 6, 9);
        assertEquals(0, renderedText(table, 2, 1).getHighlighter().getHighlights().length);
        selectOption(toolbar, "Characters", table);
        PlatformTestUtil.waitWithEventsDispatching("Character highlights displayed",
                () -> inlineRangeMatches(table, 1, 7, 8), 10);
        selectOption(toolbar, "Lines", table);
        PlatformTestUtil.waitWithEventsDispatching("Cell highlights displayed",
                () -> renderedText(table, 1, 1).getHighlighter().getHighlights().length == 0, 10);
        assertFalse(renderedText(table, 0, 1).getBackground().equals(renderedText(table, 1, 1).getBackground()));
        AnAction none = toolbar.popupActions.get(toolbar.popupActions.size() - 3);
        none.actionPerformed(actionEvent(table));
        PlatformTestUtil.waitWithEventsDispatching("Highlights disabled",
                () -> renderedText(table, 0, 1).getBackground().equals(renderedText(table, 1, 1).getBackground()), 10);
        assertEquals("Navigation and apply remain available", "1 differences", ((JLabel) toolbar.statusPanel).getText());
        assertNotNull(divider(viewer).applyArrowBounds(1, 1));
    }

    private static void selectOption(FrameDiffTool.ToolbarComponents toolbar, String name, JTable table) {
        assertNotNull("The settings menu is required", toolbar.popupActions);
        toolbar.popupActions.stream().filter(action -> name.equals(action.getTemplatePresentation().getText()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing diff option: " + name))
                .actionPerformed(actionEvent(table));
    }

    private static boolean inlineRangeMatches(JTable table, int row, int start, int end) {
        var highlights = renderedText(table, row, 1).getHighlighter().getHighlights();
        return highlights.length == 1 && highlights[0].getStartOffset() == start && highlights[0].getEndOffset() == end;
    }

    private static void assertInlineRange(JTable table, int row, int start, int end) {
        assertTrue("Expected highlighted range " + start + "-" + end, inlineRangeMatches(table, row, start, end));
    }

    public void testChangingDiffOptionsCommitsTheActiveCellAndUsesTheLatestSettings() {
        TextDiffSettingsHolder.TextDiffSettings settings = textDiffSettings();
        DocumentContent right = editable("id,value\n1,after\n");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context,
                new SimpleDiffRequest("CSV comparison", content("id,value\n1,before\n"), right, "Before", "After"));
        Disposer.register(getTestRootDisposable(), viewer);
        FrameDiffTool.ToolbarComponents toolbar = viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV cells displayed", () -> table.getRowCount() == 2, 10);
        assertTrue(table.editCellAt(1, 1));
        textComponent(table.getEditorComponent()).setText("before");
        selectOption(toolbar, "Ignore Whitespaces", table);
        settings.setHighlightPolicy(HighlightPolicy.BY_CHAR);
        settings.setIgnorePolicy(IgnorePolicy.FORMATTING);
        assertFalse(table.isEditing());
        assertEquals("id,value\n1,before\n", right.getDocument().getText());
        PlatformTestUtil.waitWithEventsDispatching("Latest diff options applied",
                () -> "0 differences".equals(((JLabel) toolbar.statusPanel).getText()), 10);
        assertEquals(0, renderedText(table, 1, 1).getHighlighter().getHighlights().length);
    }

    public void testTextOnlyPoliciesUseMeaningfulTableAlternativesWithoutChangingTextPreferences() {
        TextDiffSettingsHolder.TextDiffSettings settings = textDiffSettings();
        settings.setIgnorePolicy(IgnorePolicy.IGNORE_WHITESPACES_CHUNKS);
        settings.setHighlightPolicy(HighlightPolicy.BY_WORD_SPLIT);
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context,
                new SimpleDiffRequest("CSV comparison", content("id,value\n1,hello cat\n"),
                        content("id,value\n1,hello cut\n\n"), "Before", "After"));
        Disposer.register(getTestRootDisposable(), viewer);
        FrameDiffTool.ToolbarComponents toolbar = viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV rows displayed", () -> table.getRowCount() == 3, 10);
        assertEquals(IgnorePolicy.IGNORE_WHITESPACES_CHUNKS, settings.getIgnorePolicy());
        assertEquals(HighlightPolicy.BY_WORD_SPLIT, settings.getHighlightPolicy());
        for (String name : List.of("Ignore Whitespaces", "Words")) {
            AnAction option = toolbar.popupActions.stream()
                    .filter(action -> name.equals(action.getTemplatePresentation().getText())).findFirst().orElseThrow();
            AnActionEvent event = actionEvent(table);
            option.update(event);
            assertTrue(name + " is the effective table policy", Toggleable.isSelected(event.getPresentation()));
        }
        assertInlineRange(table, 1, 6, 9);
    }

    public void testExternalDocumentChangesRefreshOnlyTheLatestValues() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(), "id,name\n1,Bob\n", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content("id,name\n1,Alice\n"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV displayed", () -> table.getRowCount() == 2, 10);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            right.getDocument().setText("id,name\n1,First\n");
            right.getDocument().setText("id,name\n1,Second\n");
            right.getDocument().setText("id,name\n1,Latest\n");
        });
        PlatformTestUtil.waitWithEventsDispatching("Latest CSV values displayed", () -> "Latest".equals(table.getValueAt(1, 1)), 10);
        assertEquals("id,name\n1,Latest\n", right.getDocument().getText());
    }

    public void testDisposingTheViewerCancelsQueuedRefreshesAndReleasesDisplayedValues() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(), "id,name\n1,Bob\n", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content("id,name\n1,Alice\n"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV displayed", () -> table.getRowCount() == 2, 10);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> right.getDocument().setText("id,name\n1,Changed\n"));
        Disposer.dispose(viewer);
        assertEquals(0, table.getRowCount());
        WriteCommandAction.runWriteCommandAction(getProject(), () -> right.getDocument().setText("id,name\n1,After disposal\n"));
        UIUtil.dispatchAllInvocationEvents();
        assertEquals(0, table.getRowCount());
    }

    public void testApplyActionsDoNotDiscardAnUncommittedCellEdit() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(), "id,name\n1,Bob\n", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content("id,name\n1,Alice\n"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV displayed", () -> table.getRowCount() == 2, 10);
        show(viewer);
        table.changeSelection(1, 1, false, false);
        assertTrue(table.editCellAt(1, 1));
        textComponent(table.getEditorComponent()).setText("Uncommitted");
        AnAction action = shortcutAction(viewer, "Apply Difference to Right");
        AnActionEvent event = actionEvent(table);
        action.update(event);
        assertFalse("Applying a difference must not cancel typed cell contents", event.getPresentation().isEnabled());
        action.actionPerformed(event);
        clickApplyArrow(divider(viewer), 1, 1);
        UIUtil.dispatchAllInvocationEvents();
        assertTrue(table.isEditing());
        assertEquals("Uncommitted", textComponent(table.getEditorComponent()).getText());
        assertEquals("id,name\n1,Bob\n", right.getDocument().getText());
        table.getCellEditor().cancelCellEditing();
    }

    public void testDiffCommentsSpanAllDataColumns() {
        DocumentContent right = DiffContentFactory.getInstance().createEditable(getProject(),
                "id,name\n# new note\n1,Alice\n", CsvFileType.INSTANCE);
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                content("id,name\n# old note\n1,Alice\n"), right, "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable table = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV comments displayed", () -> table.getRowCount() == 3, 10);
        viewer.getComponent().setBounds(0, 0, 700, 240);
        layout(viewer.getComponent());
        Rectangle comment = table.getCellRect(1, 0, false);
        assertEquals(comment, table.getCellRect(1, 1, false));
        assertTrue(comment.width > table.getColumnModel().getColumn(0).getWidth());
        assertEquals(0, comment.x);
        table.changeSelection(1, 1, false, false);
        assertTrue(table.isCellSelected(1, 0));
        assertTrue(table.editCellAt(1, 0));
        assertEquals(comment, table.getEditorComponent().getBounds());
        textComponent(table.getEditorComponent()).setText("updated note");
        assertTrue(table.getCellEditor().stopCellEditing());
        String expectedText = "id,name\n#updated note\n1,Alice\n";
        PlatformTestUtil.waitWithEventsDispatching("CSV comment edit applied",
                () -> expectedText.equals(right.getDocument().getText()), 10);
        assertEquals(expectedText, right.getDocument().getText());
    }

    public void testDiffColorsOverlayTheSharedColumnAndCommentStyles() {
        textDiffSettings().setHighlightPolicy(HighlightPolicy.BY_WORD);
        EditorColorsManager manager = EditorColorsManager.getInstance();
        var originalScheme = manager.getGlobalScheme();
        var originalColoring = CsvEditorSettings.getInstance().getValueColoring();
        EditorColorsScheme scheme = (EditorColorsScheme) originalScheme.clone();
        scheme.setAttributes(CsvTextAttributeKeys.getTextAttributesKeys(0),
                new TextAttributes(Color.RED, Color.PINK, null, null, Font.PLAIN));
        scheme.setAttributes(CsvTextAttributeKeys.getTextAttributesKeys(1),
                new TextAttributes(Color.BLUE, Color.CYAN, null, null, Font.PLAIN));
        scheme.setAttributes(CsvTextAttributeKeys.COMMENT,
                new TextAttributes(Color.GRAY, Color.LIGHT_GRAY, null, null, Font.PLAIN));
        scheme.setAttributes(DiffColors.DIFF_MODIFIED,
                new TextAttributes(null, Color.ORANGE, null, null, Font.PLAIN));
        try {
            CsvEditorSettings.getInstance().setValueColoring(CsvEditorSettings.ValueColoring.RAINBOW);
            manager.setGlobalScheme(scheme);
            SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison",
                    content("id,name\n# note\n1,Alice\n"), content("id,name\n# note\n1,Bob\n"), "Before", "After");
            FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
            Disposer.register(getTestRootDisposable(), viewer);
            viewer.init();
            JTable table = tables(viewer.getComponent()).getLast();
            PlatformTestUtil.waitWithEventsDispatching("CSV colors displayed", () -> table.getRowCount() == 3, 10);
            JTextComponent unchanged = renderedText(table, 2, 0);
            assertEquals(Color.RED, unchanged.getForeground());
            assertEquals(Color.PINK, unchanged.getBackground());
            JTextComponent changed = renderedText(table, 2, 1);
            assertEquals(Color.BLUE, changed.getForeground());
            assertEquals(TextDiffType.MODIFIED.getIgnoredColor(null), changed.getBackground());
            var highlights = changed.getHighlighter().getHighlights();
            assertEquals(1, highlights.length);
            assertEquals(Color.ORANGE, ((DefaultHighlighter.DefaultHighlightPainter) highlights[0].getPainter()).getColor());
            JTextComponent comment = renderedText(table, 1, 0);
            assertEquals(Color.GRAY, comment.getForeground());
            assertEquals(Color.LIGHT_GRAY, comment.getBackground());
            assertEquals("id (1)", table.getColumnName(0));
            assertEquals("name (2)", table.getColumnName(1));

            EditorColorsScheme updated = (EditorColorsScheme) scheme.clone();
            updated.setAttributes(CsvTextAttributeKeys.getTextAttributesKeys(0),
                    new TextAttributes(Color.GREEN, Color.YELLOW, null, null, Font.PLAIN));
            manager.setGlobalScheme(updated);
            PlatformTestUtil.waitWithEventsDispatching("Updated CSV colors displayed",
                    () -> Color.GREEN.equals(renderedText(table, 2, 0).getForeground()), 10);
            assertEquals(Color.YELLOW, renderedText(table, 2, 0).getBackground());
        } finally {
            manager.setGlobalScheme(originalScheme);
            CsvEditorSettings.getInstance().setValueColoring(originalColoring);
        }
    }

    public void testUnchangedCellsPaintLikePlainTableEditor() {
        assertCellsPaintLikePlainTableEditor("a,b\nc,d\n", 1, false);
    }

    public void testMultilineCellsPaintLikePlainTableEditor() {
        assertCellsPaintLikePlainTableEditor("a,b\n\"one\ntwo\nthree\",d\n", 1, false);
    }

    public void testSelectedCellsPaintLikePlainTableEditor() {
        assertCellsPaintLikePlainTableEditor("a,b\nc,d\n", 1, true);
    }

    public void testCommentCellsPaintLikePlainTableEditor() {
        assertCellsPaintLikePlainTableEditor("a,b\n# a comment wider than one column\nc,d\n", 1, false);
    }

    public void testSelectedCommentCellsPaintLikePlainTableEditor() {
        assertCellsPaintLikePlainTableEditor("a,b\n# a comment wider than one column\nc,d\n", 1, true);
    }

    private void assertCellsPaintLikePlainTableEditor(String text, int row, boolean selected) {
        myFixture.configureByText(CsvFileType.INSTANCE, text);
        CsvTableEditorSwing editor = new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile());
        Disposer.register(getTestRootDisposable(), editor);
        editor.selectNotify();
        editor.getTableEditorState().setColumnWidths(new int[]{100, 100});
        editor.updateEditorLayout();
        JTable plain = tables(editor.getComponent()).stream()
                .filter(table -> table.getColumnCount() == 2).findFirst().orElseThrow();
        SimpleDiffRequest request = new SimpleDiffRequest("CSV comparison", content(text), content(text), "Before", "After");
        FrameDiffTool.DiffViewer viewer = tableTool().createComponent(context, request);
        Disposer.register(getTestRootDisposable(), viewer);
        viewer.init();
        JTable diff = tables(viewer.getComponent()).getLast();
        PlatformTestUtil.waitWithEventsDispatching("CSV cells displayed",
                () -> diff.getRowCount() > row, 10);
        assertEquals("Both fixtures must show the same cell value", plain.getValueAt(row, 0), diff.getValueAt(row, 0));
        for (int column = 0; column < diff.getColumnCount(); column++) {
            diff.getColumnModel().getColumn(column).setPreferredWidth(100);
            diff.getColumnModel().getColumn(column).setWidth(100);
        }
        plain.clearSelection();
        diff.clearSelection();
        if (selected) {
            plain.changeSelection(row, 0, false, false);
            diff.changeSelection(row, 0, false, false);
        }
        // Diff rows expand to show hard line breaks; compare the plain renderer at that same height.
        plain.setRowHeight(row, diff.getRowHeight(row));
        plain.setFont(diff.getFont());
        JPanel plainRoot = displayedTable((JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, plain));
        JPanel diffRoot = displayedTable((JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, diff));
        try {
            assertCellPaintMatches(plain, diff, row);
            plain.updateUI();
            diff.updateUI();
            plain.setFont(diff.getFont());
            assertCellPaintMatches(plain, diff, row);
        } finally {
            plainRoot.removeNotify();
            diffRoot.removeNotify();
        }
    }

    private static void assertCellPaintMatches(JTable plain, JTable diff, int row) {
        Rectangle plainCell = plain.getCellRect(row, 0, true);
        Rectangle diffCell = diff.getCellRect(row, 0, true);
        assertEquals("Cells must have the same size", plainCell.getSize(), diffCell.getSize());
        assertEquals("The plain renderer must use its painted cell bounds",
                plain.getCellRect(row, 0, false).getSize(), plain.prepareRenderer(plain.getCellRenderer(row, 0), row, 0).getSize());
        assertEquals("The diff renderer must use its painted cell bounds",
                diff.getCellRect(row, 0, false).getSize(), diff.prepareRenderer(diff.getCellRenderer(row, 0), row, 0).getSize());
        assertArrayEquals("Cells must fill and paint the same cell rectangle",
                cellPixels(plain, plainCell), cellPixels(diff, diffCell));
        JScrollPane plainRenderer = (JScrollPane) plain.getCellRenderer(row, 0);
        JScrollPane diffRenderer = (JScrollPane) diff.getCellRenderer(row, 0);
        assertEquals("Renderer insets must not shrink diff cells", plainRenderer.getInsets(), diffRenderer.getInsets());
        assertEquals("Text viewports must occupy the same area",
                plainRenderer.getViewport().getBounds(), diffRenderer.getViewport().getBounds());
    }

    private static JPanel displayedTable(JScrollPane scrollPane) {
        JPanel root = new JPanel(new BorderLayout());
        root.add(scrollPane);
        root.setSize(400, 240);
        root.addNotify();
        layout(root);
        root.validate();
        return root;
    }

    private static int[] cellPixels(JTable table, Rectangle cell) {
        BufferedImage image = new BufferedImage(table.getWidth(), table.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setClip(cell);
            table.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image.getRGB(cell.x, cell.y, cell.width, cell.height, null, 0, cell.width);
    }

    private static JTextComponent renderedText(JTable table, int row, int column) {
        JTextComponent text = textComponent(table.prepareRenderer(table.getCellRenderer(row, column), row, column));
        assertNotNull("CSV renderer must expose selectable text", text);
        return text;
    }

    private static AnActionEvent actionEvent(JComponent component) {
        return AnActionEvent.createEvent(DataManager.getInstance().getDataContext(component), null,
                "test", ActionUiKind.TOOLBAR, null);
    }

    private static AnAction toolbarAction(FrameDiffTool.ToolbarComponents toolbar, String name) {
        assertNotNull("Table diff toolbar is required", toolbar.toolbarActions);
        return toolbar.toolbarActions.stream()
                .filter(action -> name.equals(action.getTemplatePresentation().getText()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing toolbar action: " + name));
    }

    private static AnAction shortcutAction(FrameDiffTool.DiffViewer viewer, String name) {
        return ActionUtil.getActions(viewer.getComponent()).stream()
                .filter(action -> name.equals(action.getTemplatePresentation().getText()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing shortcut action: " + name));
    }

    private static void show(FrameDiffTool.DiffViewer viewer) {
        show(viewer, 600, 240);
    }

    private static void show(FrameDiffTool.DiffViewer viewer, int width, int height) {
        viewer.getComponent().setBounds(0, 0, width, height);
        layout(viewer.getComponent());
    }

    private static CsvTableDiffDivider divider(FrameDiffTool.DiffViewer viewer) {
        return UIUtil.findComponentOfType(viewer.getComponent(), CsvTableDiffDivider.class);
    }

    private static void clickApplyArrow(CsvTableDiffDivider divider, int row, int targetSide) {
        Rectangle arrow = divider.applyArrowBounds(row, targetSide);
        assertNotNull("Row " + row + " must offer an apply arrow", arrow);
        divider.dispatchEvent(mouseEvent(divider, MouseEvent.MOUSE_CLICKED, arrow));
    }

    private static MouseEvent mouseEvent(Component component, int id, Rectangle target) {
        return new MouseEvent(component, id, System.currentTimeMillis(), 0,
                (int) target.getCenterX(), (int) target.getCenterY(), 1, false, MouseEvent.BUTTON1);
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layout(nested);
        }
    }

    private FrameDiffTool tableTool() {
        for (DiffTool tool : DiffTool.EP_NAME.getExtensionList()) {
            if (tool.getClass().getName().equals("net.seesharpsoft.intellij.plugins.csv.diff.CsvTableDiffTool")) {
                return (FrameDiffTool) tool;
            }
        }
        fail("CSV table diff must be registered with IntelliJ's diff tool extension point");
        throw new AssertionError();
    }

    private static List<JTable> tables(Container root) {
        List<JTable> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof JTable table) {
                if (!isRowHeader(table)) result.add(table);
            } else if (child instanceof Container container) {
                result.addAll(tables(container));
            }
        }
        return result;
    }

    private static boolean isRowHeader(JTable table) {
        return table.getParent() instanceof JViewport viewport
                && viewport.getParent() instanceof JScrollPane scrollPane && scrollPane.getRowHeader() == viewport;
    }
}
