package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.ComponentManagerEx;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.AsyncFileEditorProvider;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.xmlb.XmlSerializer;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Deferred;
import kotlinx.coroutines.Dispatchers;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvStorageHelper;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorProvider;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableEditorSwing;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableEditorSwingProvider;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CsvEditorInitializationTest extends CsvBasePlatformTestCase {

    private static final Key<StateLoading> STATE_LOADING = Key.create("csv.test.stateLoading");
    private static final String CUSTOM_CSV = "name||value\n\"alpha||beta\"||\"quoted \\\"value\\\"\"\n";

    private final LightProjectDescriptor projectDescriptor = new LightProjectDescriptor();

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return projectDescriptor;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        } finally {
            super.tearDown();
        }
    }

    public void testTableEditorLoadsColdStateBeforeBuildingUi() throws Exception {
        checkColdEditorCreation(tableProvider());
    }

    public void testTextEditorLoadsColdStateBeforeBuildingUi() throws Exception {
        checkColdEditorCreation(textProvider());
    }

    public void testSlowTableStateLoadingKeepsEdtAndWriteActionsResponsive() throws Exception {
        checkSlowStateLoading(tableProvider());
    }

    public void testSlowTextStateLoadingKeepsEdtAndWriteActionsResponsive() throws Exception {
        checkSlowStateLoading(textProvider());
    }

    public void testCancellingTableEditorDuringStateLoadingDoesNotBuildUi() throws Exception {
        checkCancellation(tableProvider());
    }

    public void testCancellingTextEditorDuringStateLoadingDoesNotBuildUi() throws Exception {
        checkCancellation(textProvider());
    }

    public void testConcurrentTextAndTableEditorsWaitForTheSameStateLoad() throws Exception {
        VirtualFile file = myFixture.getTempDirFixture().createFile("shared.csv", CUSTOM_CSV);
        StateLoading loading = installDelayedState(file);
        try (EditorCreation text = startEditorCreation(textProvider(), file);
             EditorCreation table = startEditorCreation(tableProvider(), file)) {
            try {
                awaitStateLoading(loading);
                assertFalse(text.result().isCompleted());
                assertFalse(table.result().isCompleted());
                assertFalse(loading.uiBuilt.get());
            } finally {
                loading.release.countDown();
            }
            assertCustomFileSettings(text.awaitEditor());
            assertCustomFileSettings(table.awaitEditor());
        }
    }

    private static CsvTableEditorProvider tableProvider() {
        return new CsvTableEditorSwingProvider() {
            @Override
            public @NotNull Builder createEditorAsync(@NotNull Project project, @NotNull VirtualFile file) {
                return requireLoadedState(project, super.createEditorAsync(project, file));
            }
        };
    }

    private static CsvFileEditorProvider textProvider() {
        return new CsvFileEditorProvider() {
            @Override
            public @NotNull Builder createEditorAsync(@NotNull Project project, @NotNull VirtualFile file) {
                return requireLoadedState(project, super.createEditorAsync(project, file));
            }
        };
    }

    private static AsyncFileEditorProvider.Builder requireLoadedState(
            Project project, AsyncFileEditorProvider.Builder delegate) {
        return new AsyncFileEditorProvider.Builder() {
            @Override
            public @NotNull FileEditor build() {
                assertTrue(ApplicationManager.getApplication().isDispatchThread());
                assertNotNull("CSV state must finish loading before constructing the UI",
                        project.getServiceIfCreated(CsvFileAttributes.class));
                StateLoading loading = project.getUserData(STATE_LOADING);
                if (loading != null) loading.uiBuilt.set(true);
                return delegate.build();
            }
        };
    }

    private void checkColdEditorCreation(AsyncFileEditorProvider provider) throws Exception {
        VirtualFile file = myFixture.getTempDirFixture().createFile("cold.csv", "name,value\nalpha,1\n");
        try (EditorCreation creation = startEditorCreation(provider, file)) {
            assertNotNull(creation.awaitEditor());
        }
    }

    private void checkSlowStateLoading(AsyncFileEditorProvider provider) throws Exception {
        VirtualFile file = myFixture.getTempDirFixture().createFile("custom.csv", CUSTOM_CSV);
        StateLoading loading = installDelayedState(file);
        try (EditorCreation creation = startEditorCreation(provider, file)) {
            try {
                awaitStateLoading(loading);
                assertFalse("UI construction must wait for the persisted state", creation.result().isCompleted());
                assertFalse(loading.uiBuilt.get());
                AtomicBoolean uiCallbackRan = new AtomicBoolean();
                ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> uiCallbackRan.set(true)));
                PlatformTestUtil.waitWithEventsDispatching("EDT writes must run while CSV state is loading",
                        uiCallbackRan::get, 5);
            } finally {
                loading.release.countDown();
            }
            assertCustomFileSettings(creation.awaitEditor());
        }
    }

    private void checkCancellation(AsyncFileEditorProvider provider) throws Exception {
        VirtualFile file = myFixture.getTempDirFixture().createFile("cancelled.csv", CUSTOM_CSV);
        StateLoading loading = installDelayedState(file);
        try (EditorCreation creation = startEditorCreation(provider, file)) {
            try {
                awaitStateLoading(loading);
                creation.result().cancel(null);
            } finally {
                loading.release.countDown();
            }
            PlatformTestUtil.waitWithEventsDispatching("Cancelled editor creation must finish",
                    creation.result()::isCompleted, 10);
            assertTrue(creation.result().isCancelled());
            assertFalse("Cancelled editors must not construct Swing components", loading.uiBuilt.get());
        }
    }

    private StateLoading installDelayedState(VirtualFile file) {
        CsvFileAttributes savedAttributes = new CsvFileAttributes();
        CsvFileAttributes.Attribute attribute = new CsvFileAttributes.Attribute();
        attribute.separator = CsvValueSeparator.create("||");
        attribute.escapeCharacter = CsvEscapeCharacter.BACKSLASH;
        savedAttributes.attributeMap.put(CsvStorageHelper.getRelativeFilePath(getProject(), file), attribute);
        StateLoading loading = new StateLoading(XmlSerializer.serialize(savedAttributes));
        getProject().putUserData(STATE_LOADING, loading);
        ComponentManagerEx container = (ComponentManagerEx) getProject();
        var plugin = Objects.requireNonNull(PluginManagerCore.getPlugin(
                PluginId.getId("net.seesharpsoft.intellij.plugins.csv")));
        // Replace the implementation, not an instance: the service must remain cold.
        container.registerService(CsvFileAttributes.class, DelayedFileAttributes.class, plugin, true, null);
        Disposer.register(getTestRootDisposable(), () -> {
            loading.release.countDown();
            getProject().putUserData(STATE_LOADING, null);
            container.registerService(CsvFileAttributes.class, CsvFileAttributes.class, plugin, true, null);
        });
        return loading;
    }

    private static void awaitStateLoading(StateLoading loading) {
        PlatformTestUtil.waitWithEventsDispatching("CSV state loading must start",
                () -> loading.started.getCount() == 0, 10);
        assertFalse("Persistent CSV state must not load on EDT", loading.onEdt);
        assertFalse("Slow state storage must not hold a PSI read lock", loading.withReadAccess);
    }

    private static void assertCustomFileSettings(FileEditor editor) {
        if (editor instanceof CsvTableEditorSwing tableEditor) {
            assertEquals(2, tableEditor.getTableModel().getColumnCount());
            assertEquals("alpha||beta", tableEditor.getTableModel().getValue(1, 0));
            assertEquals("quoted \"value\"", tableEditor.getTableModel().getValue(1, 1));
        } else {
            CsvTextEditor textEditor = assertInstanceOf(editor, CsvTextEditor.class);
            textEditor.selectField(new CsvFieldPosition(1, 1));
            assertEquals(CUSTOM_CSV.indexOf("\"quoted"), textEditor.getEditor().getCaretModel().getOffset());
        }
    }

    private EditorCreation startEditorCreation(AsyncFileEditorProvider provider, VirtualFile file) {
        assertTrue(ApplicationManager.getApplication().isDispatchThread());
        Document document = ReadAction.compute(() -> {
            Document result = FileDocumentManager.getInstance().getDocument(file);
            assertNotNull(result);
            assertNotNull(PsiDocumentManager.getInstance(getProject()).getPsiFile(result));
            return result;
        });
        assertNull("The regression must start without a warmed CSV service",
                getProject().getServiceIfCreated(CsvFileAttributes.class));
        CoroutineScope scope = CoroutineScopeKt.CoroutineScope(Dispatchers.getDefault());
        Deferred<FileEditor> creation = BuildersKt.async(scope, EmptyCoroutineContext.INSTANCE, CoroutineStart.UNDISPATCHED,
                (editorScope, continuation) -> provider.createFileEditor(getProject(), file, document, editorScope, continuation));
        return new EditorCreation(provider, scope, creation);
    }

    private record EditorCreation(AsyncFileEditorProvider provider, CoroutineScope scope,
                                  Deferred<FileEditor> result) implements AutoCloseable {
        private FileEditor awaitEditor() {
            PlatformTestUtil.waitWithEventsDispatching("CSV editor creation must finish", result::isCompleted, 10);
            return result.getCompleted();
        }

        @Override
        public void close() {
            CoroutineScopeKt.cancel(scope, null);
            PlatformTestUtil.waitWithEventsDispatching("CSV editor creation must stop", result::isCompleted, 10);
            if (!result.isCancelled()) provider.disposeEditor(result.getCompleted());
        }
    }

    private static final class StateLoading {
        private final Element savedState;
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicBoolean uiBuilt = new AtomicBoolean();
        private boolean onEdt;
        private boolean withReadAccess;

        private StateLoading(Element savedState) {
            this.savedState = savedState;
        }

        private void load(CsvFileAttributes attributes) {
            onEdt = ApplicationManager.getApplication().isDispatchThread();
            withReadAccess = ApplicationManager.getApplication().isReadAccessAllowed();
            started.countDown();
            // Report unsafe loading threads without deadlocking the regression itself.
            if (!onEdt && !withReadAccess) {
                try {
                    if (!release.await(10, TimeUnit.SECONDS))
                        throw new AssertionError("CSV state loading was not released");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
            }
            attributes.loadState(XmlSerializer.deserialize(savedState, CsvFileAttributes.class));
        }
    }

    @State(name = "CsvFileAttributes", storages = @Storage(CsvStorageHelper.CSV_STATE_STORAGE_FILE))
    public static final class DelayedFileAttributes extends CsvFileAttributes {
        public DelayedFileAttributes(Project project) {
            Objects.requireNonNull(project.getUserData(STATE_LOADING)).load(this);
        }
    }
}
