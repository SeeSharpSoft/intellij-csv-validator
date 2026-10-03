package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorState;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CsvTableEditorSwingTest extends CsvTableEditorSwingTestBase {

    public void testBasics() {
        assertEquals(CsvTableEditor.EDITOR_NAME, fileEditor.getName());
        assertEquals(null, fileEditor.getCurrentLocation());
        assertInstanceOf(fileEditor.getState(FileEditorStateLevel.FULL), CsvTableEditorState.class);
        assertInstanceOf(fileEditor.getState(FileEditorStateLevel.NAVIGATION), CsvTableEditorState.class);
        assertInstanceOf(fileEditor.getState(FileEditorStateLevel.UNDO), CsvTableEditorState.class);

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
