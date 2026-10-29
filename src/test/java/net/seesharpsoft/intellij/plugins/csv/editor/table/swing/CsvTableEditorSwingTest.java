package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorState;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

public class CsvTableEditorSwingTest extends CsvTableEditorSwingTestBase {

    public void testTableAndHeaderPaintWithoutReadActions() {
        assertTrue("This regression exercises painting with existing read access",
                ApplicationManager.getApplication().isReadAccessAllowed());
        assertPaintsWithoutReadActions(() -> {
            assertEquals("Header1", fileEditor.getTable().getValueAt(0, 0));
            assertEquals("Header1 (1)", fileEditor.getTable().getModel().getColumnName(0));
        });

        WriteCommandAction.runWriteCommandAction(getProject(), () -> myFixture.getEditor().getDocument()
                .setText("# comment\n\"New \"\"header\"\"\",second\n\"multi\nline\"\n"));
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertPaintsWithoutReadActions(() -> {
            CsvTableModelSwing model = fileEditor.getTable().getModel();
            assertEquals(4, model.getRowCount());
            assertEquals(2, model.getColumnCount());
            assertTrue(model.isCommentRow(0));
            assertFalse(model.hasErrors());
            assertEquals(" comment", model.getValueAt(0, 1));
            assertEquals("New \"header\" (1)", model.getColumnName(0));
            assertEquals("multi\nline", model.getValueAt(2, 0));
            assertEquals("", model.getValueAt(2, 1));
            assertEquals("", model.getValueAt(3, 0));
        });
    }

    private void assertPaintsWithoutReadActions(Runnable assertions) {
        JTable table = fileEditor.getTable();
        table.setSize(800, 400);
        table.getTableHeader().setSize(800, 40);
        BufferedImage image = new BufferedImage(800, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        // The baseline SDK predates the platform's paint-time lock prohibition.
        Application paintApplication = prohibitReadActions(ApplicationManager.getApplication());
        try (MockedStatic<ApplicationManager> manager = mockStatic(ApplicationManager.class, CALLS_REAL_METHODS)) {
            manager.when(ApplicationManager::getApplication).thenReturn(paintApplication);
            table.paint(graphics);
            table.getTableHeader().paint(graphics);
            assertions.run();
        } finally {
            graphics.dispose();
        }
    }

    private Application prohibitReadActions(Application application) {
        Application paintApplication = mock(Application.class, delegatesTo(application));
        AssertionError prohibited = new AssertionError("The Read/Write lock is disallowed during paint");
        doThrow(prohibited).when(paintApplication).runReadAction(any(Runnable.class));
        doThrow(prohibited).when(paintApplication).runReadAction(ArgumentMatchers.<Computable<Object>>any());
        doThrow(prohibited).when(paintApplication).runReadAction(ArgumentMatchers.<ThrowableComputable<Object, RuntimeException>>any());
        return paintApplication;
    }

    public void testUnlockedTableAndHeaderPaintsAreDeferred() {
        WriteCommandAction.runWriteCommandAction(getProject(), () -> myFixture.getEditor().getDocument()
                .setText("# comment\n\"New \"\"header\"\"\",second\n\"multi\nline\",value\n"));
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        fileEditor.setFrozenColumnCount(1);

        try (DeferredPaintProbe probe = new DeferredPaintProbe()) {
            probe.paint(false, 1);
            assertEquals("Ordinary painting must not queue a callback", 0, probe.queuedPaints);
            assertFalse(probe.cellValues.isEmpty());
            assertFalse(probe.headerValues.isEmpty());
            probe.cellValues.clear();
            probe.headerValues.clear();

            probe.paint(true, 32);
            assertEquals("Table and header paints must share one pending callback", 1, probe.queuedPaints);
            assertTrue("Unlocked painting must not read or render cells", probe.cellValues.isEmpty());
            assertTrue("Unlocked painting must not render headers", probe.headerValues.isEmpty());
            assertEquals(0, probe.immediatePaints);

            UIUtil.dispatchAllInvocationEvents();

            assertEquals("The callback must paint immediately, not queue another repaint", 1, probe.immediatePaints);
            assertTrue(probe.cellValues.contains("New \"header\""));
            assertTrue(probe.cellValues.contains("multi\nline"));
            assertTrue(probe.headerValues.contains("New \"header\" (1)"));
            assertEquals("Deferred painting must not start a callback loop", 1, probe.queuedPaints);
        }
    }

    public void testDeferredPaintSkipsHiddenAndDisposedEditors() {
        try (DeferredPaintProbe probe = new DeferredPaintProbe()) {
            probe.paint(true, 1);
            assertEquals(1, probe.queuedPaints);
            probe.surface.setVisible(false);

            UIUtil.dispatchAllInvocationEvents();

            assertEquals(0, probe.immediatePaints);
            assertTrue(probe.cellValues.isEmpty());
            assertTrue(probe.headerValues.isEmpty());

            probe.surface.setVisible(true);
            probe.paint(true, 1);
            assertEquals("Skipping a hidden editor must clear the pending paint", 2, probe.queuedPaints);

            UIUtil.dispatchAllInvocationEvents();

            assertEquals(1, probe.immediatePaints);
            probe.cellValues.clear();
            probe.headerValues.clear();
            probe.paint(true, 1);
            assertEquals(3, probe.queuedPaints);
            fileEditor.dispose();

            UIUtil.dispatchAllInvocationEvents();

            assertEquals("A disposed editor must not be painted by a queued callback", 1, probe.immediatePaints);
            assertTrue(probe.cellValues.isEmpty());
            assertTrue(probe.headerValues.isEmpty());
        }
    }

    public void testHeaderOnlyDeferredPaintUsesCurrentValues() {
        try (DeferredPaintProbe probe = new DeferredPaintProbe()) {
            probe.paint(true, 1, fileEditor.getTable().getTableHeader());
            assertEquals(1, probe.queuedPaints);
            assertTrue(probe.headerValues.isEmpty());

            WriteCommandAction.runWriteCommandAction(getProject(), () -> myFixture.getEditor().getDocument()
                    .setText("Updated,second\none,two\n"));
            PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

            UIUtil.dispatchAllInvocationEvents();

            assertEquals(1, probe.immediatePaints);
            assertTrue(probe.headerValues.contains("Updated (1)"));
            assertTrue(probe.cellValues.contains("Updated"));
            assertTrue(probe.cellValues.contains("two"));
            assertEquals(1, probe.queuedPaints);
        }
    }

    private final class DeferredPaintProbe implements AutoCloseable {
        private final ApplicationImpl application = (ApplicationImpl) ApplicationManager.getApplication();
        private final Application paintApplication = prohibitReadActions(application);
        private final CsvTable table = fileEditor.getTable();
        private final JScrollPane originalScrollPane = fileEditor.getTableScrollPane();
        private final TableCellRenderer cellRenderer = table.getDefaultRenderer(Object.class);
        private final TableCellRenderer commentRenderer = table.getDefaultRenderer(CsvTable.CommentColumn.class);
        private final TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        private final List<Object> cellValues = new ArrayList<>();
        private final List<Object> headerValues = new ArrayList<>();
        private final BufferedImage image = new BufferedImage(800, 400, BufferedImage.TYPE_INT_ARGB);
        private int queuedPaints;
        private int immediatePaints;

        // Adapt the native display seam so the real table/renderers can also run headlessly.
        private final JScrollPane surface = new JScrollPane(table) {
            @Override
            public boolean isShowing() {
                return isVisible();
            }

            @Override
            public void paintImmediately(Rectangle rectangle) {
                assertTrue("The IDE callback must establish read access before painting",
                        application.isReadAccessAllowed());
                immediatePaints++;
                Graphics2D graphics = image.createGraphics();
                try {
                    graphics.setClip(rectangle);
                    paint(graphics);
                } finally {
                    graphics.dispose();
                }
            }
        };

        private DeferredPaintProbe() {
            surface.setColumnHeaderView(table.getTableHeader());
            surface.setSize(800, 400);
            layout(surface);
            table.setDefaultRenderer(Object.class, recordValues(cellRenderer, cellValues));
            table.setDefaultRenderer(CsvTable.CommentColumn.class, recordValues(commentRenderer, cellValues));
            table.getTableHeader().setDefaultRenderer(recordValues(headerRenderer, headerValues));

            doAnswer(invocation -> {
                queuedPaints++;
                Runnable callback = invocation.getArgument(0);
                ModalityState modality = invocation.getArgument(1);
                Condition<?> expired = invocation.getArgument(2);
                application.invokeLater(() -> withPaintApplication(callback), modality, expired);
                return null;
            }).when(paintApplication).invokeLater(any(Runnable.class), any(ModalityState.class), any(Condition.class));
        }

        private TableCellRenderer recordValues(TableCellRenderer renderer, List<Object> values) {
            return (table, value, selected, focused, row, column) -> {
                values.add(value);
                return renderer.getTableCellRendererComponent(table, value, selected, focused, row, column);
            };
        }

        private void layout(Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                if (child instanceof Container nested) layout(nested);
            }
        }

        private void paint(boolean unlocked, int repetitions) {
            paint(unlocked, repetitions, table, table.getTableHeader());
        }

        @SuppressWarnings("UnstableApiUsage")
        private void paint(boolean unlocked, int repetitions, JComponent... components) {
            Runnable painting = () -> withPaintApplication(() -> {
                assertEquals(!unlocked, application.isReadAccessAllowed());
                Graphics2D graphics = image.createGraphics();
                try {
                    for (int i = 0; i < repetitions; i++) {
                        for (JComponent component : components) component.paint(graphics);
                    }
                } finally {
                    graphics.dispose();
                }
            });
            if (unlocked) {
                // runUnlockingIntendedWrite is a no-op with the baseline SDK's nested locking.
                application.getThreadingSupport().releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack(() -> {
                    painting.run();
                    return null;
                });
            } else {
                painting.run();
            }
        }

        private void withPaintApplication(Runnable painting) {
            try (MockedStatic<ApplicationManager> manager = mockStatic(ApplicationManager.class, CALLS_REAL_METHODS)) {
                manager.when(ApplicationManager::getApplication).thenReturn(paintApplication);
                painting.run();
            }
        }

        @Override
        public void close() {
            surface.setVisible(false);
            table.setDefaultRenderer(Object.class, cellRenderer);
            table.setDefaultRenderer(CsvTable.CommentColumn.class, commentRenderer);
            table.getTableHeader().setDefaultRenderer(headerRenderer);
            originalScrollPane.setViewportView(table);
            originalScrollPane.setColumnHeaderView(table.getTableHeader());
            UIUtil.dispatchAllInvocationEvents();
        }
    }

    public void testSelectedCellColorsRemainVisibleOutsideEditMode() {
        JTable table = fileEditor.getTable();
        CsvMultiLineCellRenderer renderer = (CsvMultiLineCellRenderer) table.getCellRenderer(0, 0);
        JTextArea area = (JTextArea) renderer.getViewport().getView();
        for (boolean focused : new boolean[]{false, true}) {
            renderer.getTableCellRendererComponent(table, "Selected", true, focused, 0, 0);
            assertEquals(table.getSelectionBackground(), area.getBackground());
            assertEquals(table.getSelectionForeground(), area.getForeground());
            assertEquals(area.getBackground(), renderer.getViewport().getBackground());
            assertNull("Normal cell selection must not select text inside the cell", area.getSelectedText());
        }
        renderer.getTableCellEditorComponent(table, "Editing", true, 0, 0);
        assertEquals(table.getBackground(), area.getBackground());
        assertEquals(table.getForeground(), area.getForeground());
    }

    public void testBasics() {
        assertEquals(CsvTableEditor.EDITOR_NAME, fileEditor.getName());
        assertEquals(null, fileEditor.getCurrentLocation());
        assertInstanceOf(fileEditor.getState(FileEditorStateLevel.FULL), CsvTableEditorState.class);
        assertInstanceOf(fileEditor.getState(FileEditorStateLevel.NAVIGATION), CsvTableEditorState.class);
        assertSame(FileEditorState.INSTANCE, fileEditor.getState(FileEditorStateLevel.UNDO));

        CsvTableEditorState myState = new CsvTableEditorState();
        fileEditor.setState(myState);
        assertEquals(myState, fileEditor.getState(FileEditorStateLevel.FULL));

        assertEquals(false, fileEditor.isModified());
        assertEquals(true, fileEditor.isValid());
        assertEquals(null, fileEditor.getBackgroundHighlighter());

        assertEquals(myFixture.getFile().getVirtualFile(), fileEditor.getFile());
        assertEquals(this.getProject(), fileEditor.getProject());
        assertNotNull(fileEditor.getComponent());
        assertEquals(fileEditor.getTable(), fileEditor.getPreferredFocusedComponent());
    }

    public void testPixelRowHeightDoesNotChangeWithFontSize() {
        CsvTableEditorState state = new CsvTableEditorState();
        state.setRowHeight(24);
        fileEditor.setState(state);
        fileEditor.changeFontSize(4);
        assertEquals(24, fileEditor.getTable().getRowHeight());
        assertEquals(24, ((JTable) fileEditor.getTableScrollPane().getRowHeader().getView()).getRowHeight());
    }

    public void testAddRemovePropertyChangeListener() throws Throwable {
        assertThrows(IllegalArgumentException.class, () -> fileEditor.addPropertyChangeListener(null));
        assertThrows(IllegalArgumentException.class, () -> fileEditor.removePropertyChangeListener(null));

        PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
            }
        };
        fileEditor.removePropertyChangeListener(listener);
        fileEditor.addPropertyChangeListener(listener);
        fileEditor.removePropertyChangeListener(listener);
        assertTrue(true);
    }

    public void testEditorStatesAreIndependentSnapshots() {
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 200});
        fileEditor.getTableEditorState().setRowHeight(42);
        fileEditor.getTableEditorState().setFrozenColumnCount(1);
        for (FileEditorStateLevel level : new FileEditorStateLevel[]{FileEditorStateLevel.FULL, FileEditorStateLevel.NAVIGATION}) {
            CsvTableEditorState snapshot = (CsvTableEditorState) fileEditor.getState(level);
            assertNotSame(snapshot, fileEditor.getTableEditorState());
            fileEditor.getTableEditorState().getColumnWidths()[0] = 300;
            assertOrderedEquals(snapshot.getColumnWidths(), new int[]{80, 200});
            fileEditor.setState(snapshot);
            assertNotSame(snapshot, fileEditor.getTableEditorState());
            snapshot.getColumnWidths()[1] = 400;
            snapshot.setRowHeight(50);
            snapshot.setFrozenColumnCount(0);
            assertOrderedEquals(fileEditor.getTableEditorState().getColumnWidths(), new int[]{80, 200});
            assertEquals(42, fileEditor.getTableEditorState().getRowHeight());
            assertEquals(1, fileEditor.getTableEditorState().getFrozenColumnCount());
        }
    }

    public void testUndoStateDoesNotRestoreTableLayout() {
        FileEditorState undoState = fileEditor.getState(FileEditorStateLevel.UNDO);
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 200});
        fileEditor.getTableEditorState().setRowHeight(42);
        fileEditor.getTableEditorState().setFrozenColumnCount(1);

        assertSame(undoState, fileEditor.getState(FileEditorStateLevel.UNDO));
        fileEditor.setState(undoState);

        assertOrderedEquals(fileEditor.getTableEditorState().getColumnWidths(), new int[]{80, 200});
        assertEquals(42, fileEditor.getTableEditorState().getRowHeight());
        assertEquals(1, fileEditor.getTableEditorState().getFrozenColumnCount());
    }

    public void testEditable() {
        assertEquals(true, fileEditor.isEditable());
        fileEditor.setEditable(false);
        assertEquals(false, fileEditor.isEditable());
        fileEditor.setEditable(true);
        assertEquals(true, fileEditor.isEditable());
    }

    public void testUserDataHolder() {
        Key<String> testKey = new Key<>("myKey");
        String value = "This is just a test";
        assertNull(fileEditor.getUserData(testKey));
        fileEditor.putUserData(testKey, value);
        assertEquals(value, fileEditor.getUserData(testKey));
    }

    public void testTableContent() {
        CsvTableModel tableModel = fileEditor.getTableModel();
        assertEquals(2, tableModel.getColumnCount());
        assertEquals(4, tableModel.getRowCount());

        assertEquals("Header1", tableModel.getValue(0, 0));
        assertEquals(" header 2", tableModel.getValue(0, 1));
        assertEquals("this is column \"Header1\"", tableModel.getValue(1, 0));
        assertEquals("this is column header 2", tableModel.getValue(1, 1));
        assertEquals(" just another line with leading and trailing whitespaces  ", tableModel.getValue(2, 0));
        assertEquals("  and one more value  ", tableModel.getValue(2, 1));
        assertEquals("", tableModel.getValue(3, 0));
        assertEquals("", tableModel.getValue(3, 1));
    }

    public void testDisposedEditorDoesNotExposePsiFile() {
        assertNotNull(fileEditor.getPsiFile());

        fileEditor.dispose();

        assertNull(fileEditor.getPsiFile());
    }

    public void testDisposedEditorIgnoresPendingPsiResolution() throws Exception {
        BlockingCsvTableEditor editor = new BlockingCsvTableEditor(getProject(), myFixture.getFile().getVirtualFile());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            editor.invalidateCachedPsiFile();
            editor.blockNextResolution();
            Future<PsiFile> resolution = executor.submit(editor::getPsiFile);

            assertTrue(editor.resolutionStarted.await(5, TimeUnit.SECONDS));
            editor.dispose();
            editor.releaseResolution.countDown();

            assertNull(resolution.get(5, TimeUnit.SECONDS));
        } finally {
            editor.releaseResolution.countDown();
            if (!editor.isDisposed()) {
                editor.dispose();
            }
            executor.shutdownNow();
        }
    }

    public void testInvalidatedFileDoesNotReuseCachedPsiFile() throws Exception {
        assertNotNull(fileEditor.getPsiFile());
        VirtualFile virtualFile = fileEditor.getFile();

        WriteAction.run(() -> {
            try {
                virtualFile.delete(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertFalse(fileEditor.isValid());
        assertNull(fileEditor.getPsiFile());
    }

    private static final class BlockingCsvTableEditor extends CsvTableEditorSwing {
        private final CountDownLatch resolutionStarted = new CountDownLatch(1);
        private final CountDownLatch releaseResolution = new CountDownLatch(1);
        private volatile boolean blockResolution;

        private BlockingCsvTableEditor(@NotNull com.intellij.openapi.project.Project project,
                                       @NotNull VirtualFile file) {
            super(project, file);
        }

        private void blockNextResolution() {
            blockResolution = true;
        }

        private void invalidateCachedPsiFile() {
            psiFile = null;
        }

        @Override
        protected PsiFile resolvePsiFile() {
            if (blockResolution) {
                resolutionStarted.countDown();
                try {
                    releaseResolution.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                blockResolution = false;
            }
            return super.resolvePsiFile();
        }
    }
}
