package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CsvTableReorderProgressTest extends CsvTableEditorSwingTestBase {
    private static final String ORIGINAL = "a,b,c,d\n1,2,3,4\n5,6,7,8\n9,10,11,12\n";
    private static final String PADDING = "#" + "padding".repeat(20_000) + "\n";
    protected Runnable beforeOperation = () -> {
    };
    protected Runnable afterOperation = () -> {
    };
    private final AtomicInteger progressRuns = new AtomicInteger();

    @Override
    protected CsvTableEditorSwing createEditor() {
        return new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile()) {
            @Override
            protected boolean runReorderWithProgress(Runnable operation) {
                return super.runReorderWithProgress(() -> controlledOperation(operation));
            }
        };
    }

    protected void controlledOperation(Runnable operation) {
        assertFalse("Large-file work must run off the EDT", ApplicationManager.getApplication().isDispatchThread());
        progressRuns.incrementAndGet();
        beforeOperation.run();
        operation.run();
        afterOperation.run();
    }

    private void onUi(Runnable operation) {
        ApplicationManager.getApplication().invokeAndWait(operation, ModalityState.defaultModalityState());
    }

    private void configure(String text) {
        fileEditor.dispose();
        myFixture.configureByText(CsvFileType.INSTANCE, text);
        fileEditor = createEditor();
        fileEditor.selectNotify();
        fileEditor.getTable().changeSelection(1, 1, false, false);
    }

    private void assertReady() {
        assertFalse(fileEditor.isReordering());
        assertFalse(fileEditor.getTableModel().isSuspended());
        assertTrue(fileEditor.isEditable());
        assertTrue(fileEditor.getTable().isEnabled());
    }

    public void testSmallMoveDoesNotNeedProgress() {
        configure(ORIGINAL);
        fileEditor.moveSelection(true, true);
        assertEquals(0, progressRuns.get());
        assertEquals("b,a,c,d\n2,1,3,4\n6,5,7,8\n10,9,11,12\n", myFixture.getFile().getText());
        assertReady();
    }

    public void testLargeMoveIsOneDocumentEventAndUndoStep() {
        configure(ORIGINAL + PADDING);
        AtomicBoolean uiRan = new AtomicBoolean();
        beforeOperation = () -> onUi(() -> {
            assertTrue(fileEditor.isReordering());
            assertFalse(fileEditor.isEditable());
            assertEquals(ORIGINAL + PADDING, myFixture.getEditor().getDocument().getText());
            uiRan.set(true);
        });
        AtomicInteger changes = new AtomicInteger();
        myFixture.getEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                changes.incrementAndGet();
            }
        }, getTestRootDisposable());
        fileEditor.moveSelection(true, true);
        assertTrue(uiRan.get());
        assertEquals(1, progressRuns.get());
        assertEquals(1, changes.get());
        assertEquals("b,a,c,d\n2,1,3,4\n6,5,7,8\n10,9,11,12\n" + PADDING, myFixture.getFile().getText());
        assertEquals("b (1)", fileEditor.getTable().getColumnModel().getColumn(0).getHeaderValue());
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
        assertReady();
        UndoManager undo = UndoManager.getInstance(getProject());
        undo.undo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(ORIGINAL + PADDING, myFixture.getFile().getText());
        undo.redo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals("b", fileEditor.getTableModel().getValue(0, 0));
    }

    public void testCancelDuringPreparationLeavesFileUnchanged() {
        configure(ORIGINAL + PADDING);
        beforeOperation = () -> ProgressManager.getInstance().getProgressIndicator().cancel();
        fileEditor.moveSelection(true, true);
        assertEquals(ORIGINAL + PADDING, myFixture.getFile().getText());
        assertReady();
    }

    public void testCancelAfterApplyingFinishesConsistentUpdate() {
        configure(ORIGINAL + PADDING);
        AtomicReference<ProgressIndicator> progress = new AtomicReference<>();
        beforeOperation = () -> progress.set(ProgressManager.getInstance().getProgressIndicator());
        myFixture.getEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                progress.get().cancel();
            }
        }, getTestRootDisposable());
        fileEditor.moveSelection(true, true);
        assertEquals("b", fileEditor.getTableModel().getValue(0, 0));
        assertReady();
        UndoManager.getInstance(getProject()).undo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(ORIGINAL + PADDING, myFixture.getFile().getText());
    }

    public void testExternalEditRejectsStalePreparedMove() {
        configure(ORIGINAL + PADDING);
        beforeOperation = () -> onUi(() -> WriteCommandAction.runWriteCommandAction(getProject(), () ->
                myFixture.getEditor().getDocument().insertString(0, "# external edit\n")));
        fileEditor.moveSelection(true, true);
        assertEquals("# external edit\n" + ORIGINAL + PADDING, myFixture.getEditor().getDocument().getText());
        assertReady();
    }

    public void testFileBecomingReadOnlyRejectsPreparedMove() throws Exception {
        configure(ORIGINAL + PADDING);
        beforeOperation = () -> onUi(() -> {
            try {
                WriteAction.run(() -> fileEditor.getFile().setWritable(false));
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
        });
        try {
            fileEditor.moveSelection(true, true);
            assertEquals(ORIGINAL + PADDING, myFixture.getFile().getText());
            assertFalse(fileEditor.isReordering());
            assertFalse(fileEditor.getTableModel().isSuspended());
        } finally {
            WriteAction.run(() -> fileEditor.getFile().setWritable(true));
        }
    }

    public void testSettingsChangeRejectsPreparedMove() {
        configure(ORIGINAL + PADDING);
        beforeOperation = () -> onUi(() -> CsvEditorSettings.getInstance().setCommentIndicator("##"));
        try {
            fileEditor.moveSelection(true, true);
            assertEquals(ORIGINAL + PADDING, myFixture.getFile().getText());
            assertReady();
        } finally {
            CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        }
    }

    public void testDisposedEditorRejectsPreparedMove() {
        configure(ORIGINAL + PADDING);
        beforeOperation = () -> onUi(fileEditor::dispose);
        fileEditor.moveSelection(true, true);
        assertTrue(fileEditor.isDisposed());
        assertFalse(fileEditor.isReordering());
        assertFalse(fileEditor.getTableModel().isSuspended());
        assertEquals(ORIGINAL + PADDING, myFixture.getEditor().getDocument().getText());
    }

    public void testFailureRestoresEditorState() {
        configure(ORIGINAL + PADDING);
        beforeOperation = () -> {
            throw new IllegalStateException("Test preparation failure");
        };
        assertThrows(IllegalStateException.class, () -> fileEditor.moveSelection(true, true));
        assertEquals(ORIGINAL + PADDING, myFixture.getFile().getText());
        assertReady();
    }

    public void testManyShortRowsAlsoUseProgress() {
        configure("a,b\n" + "1,2\n".repeat(12_000));
        fileEditor.moveSelection(true, true);
        assertEquals(1, progressRuns.get());
        assertEquals("b", fileEditor.getTableModel().getValue(0, 0));
        assertEquals("2", fileEditor.getTableModel().getValue(12_000, 0));
        assertReady();
    }

    public void testNativeColumnDropUsesProgressWithoutCustomRendering() {
        configure(ORIGINAL + PADDING);
        fileEditor.getTable().setSize(560, 240);
        var header = fileEditor.getTable().getTableHeader();
        header.setSize(560, 32);
        Rectangle from = header.getHeaderRect(0);
        Rectangle to = header.getHeaderRect(1);
        int x = from.x + from.width / 2;
        int target = to.x + to.width / 2;
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                InputEvent.BUTTON1_DOWN_MASK, x, 12, 1, false, MouseEvent.BUTTON1));
        for (int position = x + 5; position < target; position += 5) {
            header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                    InputEvent.BUTTON1_DOWN_MASK, position, 12, 1, false, MouseEvent.NOBUTTON));
        }
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                InputEvent.BUTTON1_DOWN_MASK, target, 12, 1, false, MouseEvent.NOBUTTON));
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                0, target, 12, 1, false, MouseEvent.BUTTON1));
        assertEquals(1, progressRuns.get());
        assertEquals("b", fileEditor.getTableModel().getValue(0, 0));
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
        assertNull(UIUtil.findComponentOfType(fileEditor.getComponent(), JLayer.class));
        assertReady();
    }

    public void testRowMoveUpdatesHeaderAndLeavesNoCustomRendering() {
        configure(ORIGINAL + PADDING);
        afterOperation = () -> onUi(() -> {
            assertEquals("1", fileEditor.getTable().getValueAt(0, 0));
            fileEditor.getTable().setSize(560, 200);
            BufferedImage image = new BufferedImage(560, 200, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                fileEditor.getTable().paint(graphics);
            } finally {
                graphics.dispose();
            }
        });
        fileEditor.moveRow(0, 3);
        assertEquals("1,2,3,4\n5,6,7,8\n9,10,11,12\na,b,c,d\n" + PADDING, myFixture.getFile().getText());
        assertEquals("1 (1)", fileEditor.getTable().getColumnModel().getColumn(0).getHeaderValue());
        assertEquals(3, fileEditor.getTable().getSelectedRow());
        assertReady();
    }

    public void testLargeMoveKeepsSelectionFrozenColumnsAndWidthsWithoutEventStorm() {
        configure("a,b,c,d\n" + "1,2,3,4\n".repeat(20_000));
        fileEditor.setFrozenColumnCount(2);
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 120, 160, 200});
        fileEditor.updateEditorLayout();
        fileEditor.selectColumn(1, false);
        AtomicInteger selectionEvents = new AtomicInteger();
        fileEditor.getTable().getSelectionModel().addListSelectionListener(event -> selectionEvents.incrementAndGet());
        AtomicLong previous = new AtomicLong(System.nanoTime());
        AtomicLong longest = new AtomicLong();
        Timer heartbeat = new Timer(10, event -> {
            long now = System.nanoTime();
            longest.accumulateAndGet(now - previous.getAndSet(now), Math::max);
        });
        heartbeat.start();
        try {
            fileEditor.moveSelection(true, true);
        } finally {
            heartbeat.stop();
        }
        long gap = TimeUnit.NANOSECONDS.toMillis(longest.get());
        assertTrue("UI heartbeat stalled for " + gap + " ms", gap < 2500);
        assertTrue("Selection event storm: " + selectionEvents, selectionEvents.get() < 30);
        assertEquals(20_002, fileEditor.getTable().getSelectedRowCount());
        assertEquals(2, fileEditor.getFrozenColumnCount());
        assertEquals(120, fileEditor.getTableEditorState().getColumnWidths()[0]);
        assertEquals("2", fileEditor.getTableModel().getValue(20_000, 0));
        assertReady();
        System.out.println("20,001-row modal move: " + gap + " ms UI heartbeat gap, " + selectionEvents + " selection events");
    }
}
