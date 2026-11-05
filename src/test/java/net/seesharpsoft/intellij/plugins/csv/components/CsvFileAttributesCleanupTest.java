package net.seesharpsoft.intellij.plugins.csv.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.highlighter.CsvEditorHighlighterProvider;
import org.jetbrains.concurrency.CancellablePromise;
import org.mockito.MockedStatic;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

public class CsvFileAttributesCleanupTest extends CsvBasePlatformTestCase {

    private CsvFileAttributes attributes;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.configureByText("cleanup.csv", "name;value\nalpha;1\n");
        attributes = CsvFileAttributes.getInstance(getProject());
        attributes.reset();
        attributes.setFileSeparator(myFixture.getFile(), CsvValueSeparator.SEMICOLON);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            attributes.reset();
        } finally {
            super.tearDown();
        }
    }

    public void testSlowCleanupDoesNotBlockHighlightingOrEdtWrites() throws Exception {
        EditorColorsScheme colors = EditorColorsManager.getInstance().getGlobalScheme();
        try (SlowCleanup cleanup = new SlowCleanup()) {
            cleanup.awaitStarted();
            CancellablePromise<EditorHighlighter> highlighting = ReadAction.nonBlocking(() -> {
                EditorHighlighter highlighter = new CsvEditorHighlighterProvider().getEditorHighlighter(
                        getProject(), myFixture.getFile().getFileType(),
                        myFixture.getFile().getVirtualFile(), colors);
                highlighter.setText("name;value\nalpha;1\n");
                return highlighter;
            }).expireWith(getTestRootDisposable()).submit(cleanup.executor);

            PlatformTestUtil.waitWithEventsDispatching(
                    "CSV highlighting must finish while cleanup filesystem I/O is stalled", highlighting::isDone, 5);
            assertNotNull(highlighting.get());

            AtomicBoolean writeCompleted = new AtomicBoolean();
            ApplicationManager.getApplication().invokeLater(() ->
                    WriteAction.run(() -> writeCompleted.set(true)));
            PlatformTestUtil.waitWithEventsDispatching(
                    "EDT writes must run while cleanup filesystem I/O is stalled", writeCompleted::get, 5);
            assertEquals("Cleanup must still be waiting for filesystem I/O", 1L, cleanup.release.getCount());
        }
    }

    public void testCleanupPreservesSeparatorChangedDuringFilesystemCheck() throws Exception {
        CsvValueSeparator separator = CsvValueSeparator.create("||");
        try (SlowCleanup cleanup = new SlowCleanup()) {
            cleanup.awaitStarted();
            attributes.setFileSeparator(myFixture.getFile(), separator);
            cleanup.finish();
        }
        assertSettings(separator, null);
    }

    public void testCleanupPreservesEscapeCharacterChangedDuringFilesystemCheck() throws Exception {
        try (SlowCleanup cleanup = new SlowCleanup()) {
            cleanup.awaitStarted();
            attributes.setEscapeCharacter(myFixture.getFile(), CsvEscapeCharacter.BACKSLASH);
            cleanup.finish();
        }
        assertSettings(CsvValueSeparator.SEMICOLON, CsvEscapeCharacter.BACKSLASH);
    }

    public void testResettingSeparatorDuringCleanupPreservesEscapeCharacter() throws Exception {
        attributes.setEscapeCharacter(myFixture.getFile(), CsvEscapeCharacter.BACKSLASH);
        try (SlowCleanup cleanup = new SlowCleanup()) {
            cleanup.awaitStarted();
            attributes.resetValueSeparator(myFixture.getFile());
            cleanup.finish();
        }
        assertSettings(null, CsvEscapeCharacter.BACKSLASH);
    }

    public void testResettingEscapeCharacterDuringCleanupPreservesSeparator() throws Exception {
        attributes.setEscapeCharacter(myFixture.getFile(), CsvEscapeCharacter.BACKSLASH);
        try (SlowCleanup cleanup = new SlowCleanup()) {
            cleanup.awaitStarted();
            attributes.resetEscapeSeparator(myFixture.getFile());
            cleanup.finish();
        }
        assertSettings(CsvValueSeparator.SEMICOLON, null);
    }

    public void testCleanupPreservesReloadedSettings() throws Exception {
        CsvFileAttributes reloaded = new CsvFileAttributes();
        reloaded.setFileSeparator(myFixture.getFile(), CsvValueSeparator.PIPE);
        reloaded.setEscapeCharacter(myFixture.getFile(), CsvEscapeCharacter.BACKSLASH);
        try (SlowCleanup cleanup = new SlowCleanup()) {
            cleanup.awaitStarted();
            attributes.loadState(reloaded);
            cleanup.finish();
        }
        assertSettings(CsvValueSeparator.PIPE, CsvEscapeCharacter.BACKSLASH);
    }

    public void testCleanupPreservesSettingsRecreatedAfterReset() throws Exception {
        try (SlowCleanup cleanup = new SlowCleanup()) {
            cleanup.awaitStarted();
            attributes.reset();
            attributes.setEscapeCharacter(myFixture.getFile(), CsvEscapeCharacter.BACKSLASH);
            cleanup.finish();
        }
        assertSettings(null, CsvEscapeCharacter.BACKSLASH);
    }

    public void testCancellationDuringFilesystemCheckDoesNotRemoveSettings() {
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        try (MockedStatic<FileUtil> files = mockStatic(FileUtil.class, CALLS_REAL_METHODS)) {
            files.when(() -> FileUtil.exists(anyString())).thenAnswer(invocation -> {
                indicator.cancel();
                return false;
            });
            assertThrows(ProcessCanceledException.class, () -> ProgressManager.getInstance().runProcess(
                    () -> attributes.cleanupAttributeMap(getProject()), indicator));
        }
        assertSettings(CsvValueSeparator.SEMICOLON, null);
    }

    private void assertSettings(CsvValueSeparator separator, CsvEscapeCharacter escapeCharacter) {
        VirtualFile file = myFixture.getFile().getVirtualFile();
        assertEquals(separator != null, attributes.hasValueSeparatorAttribute(getProject(), file));
        if (separator != null) {
            assertEquals(separator, attributes.getValueSeparator(getProject(), file));
        }
        assertEquals(escapeCharacter != null, attributes.hasEscapeCharacterAttribute(getProject(), file));
        if (escapeCharacter != null) {
            assertEquals(escapeCharacter, attributes.getEscapeCharacter(getProject(), file));
        }
    }

    private final class SlowCleanup implements AutoCloseable {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final ExecutorService executor = Executors.newFixedThreadPool(2);
        private final Future<?> task;

        private SlowCleanup() {
            task = executor.submit(() -> {
                try (MockedStatic<FileUtil> files = mockStatic(FileUtil.class, CALLS_REAL_METHODS)) {
                    files.when(() -> FileUtil.exists(anyString())).thenAnswer(invocation -> {
                        started.countDown();
                        if (!release.await(10, TimeUnit.SECONDS)) {
                            throw new AssertionError("Cleanup filesystem I/O was not released");
                        }
                        return false;
                    });
                    attributes.cleanupAttributeMap(getProject());
                }
            });
        }

        private void awaitStarted() {
            PlatformTestUtil.waitWithEventsDispatching(
                    "Cleanup must reach the filesystem", () -> started.getCount() == 0, 5);
        }

        private void finish() throws Exception {
            release.countDown();
            task.get(10, TimeUnit.SECONDS);
        }

        @Override
        public void close() throws Exception {
            try {
                finish();
            } finally {
                executor.shutdownNow();
                assertTrue("Cleanup test workers must stop", executor.awaitTermination(10, TimeUnit.SECONDS));
            }
        }
    }
}
