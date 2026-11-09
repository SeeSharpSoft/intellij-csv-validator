package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.ExtensionTestUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Deferred;
import kotlinx.coroutines.Dispatchers;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CsvEditorProviderAcceptanceTest extends CsvBasePlatformTestCase {

    private CsvEditorSettings.OptionSet savedSettings;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        savedSettings = settings.getState();
        settings.loadState(new CsvEditorSettings.OptionSet());
        List<FileEditorProvider> providers = FileEditorProvider.EP_FILE_EDITOR_PROVIDER.getExtensionList().stream()
                .filter(provider -> provider instanceof CsvFileEditorProviderBase)
                .toList();
        assertEquals(2, providers.size());
        ExtensionTestUtil.maskExtensions(FileEditorProvider.EP_FILE_EDITOR_PROVIDER, providers, getTestRootDisposable());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            CsvEditorSettings.getInstance().loadState(savedSettings);
        } finally {
            super.tearDown();
        }
    }

    public void testCsvProvidersResolveWhileWriteActionIsHeld() throws Exception {
        assertProvidersResolveWhileWriteActionIsHeld(
                new LightVirtualFile("data.csv", CsvFileType.INSTANCE, "name,value\nalpha,1\n"));
    }

    public void testLocalCsvFamilyProvidersResolveWhileWriteActionIsHeld() throws Exception {
        assertProvidersResolveWhileWriteActionIsHeld(
                myFixture.getTempDirFixture().createFile("data.csv", "name,value\nalpha,1\n"));
        assertProvidersResolveWhileWriteActionIsHeld(
                myFixture.getTempDirFixture().createFile("data.tsv", "name\tvalue\nalpha\t1\n"));
        assertProvidersResolveWhileWriteActionIsHeld(
                myFixture.getTempDirFixture().createFile("data.psv", "name|value\nalpha|1\n"));
    }

    private void assertProvidersResolveWhileWriteActionIsHeld(VirtualFile file) throws Exception {
        FileEditorProviderManager manager = FileEditorProviderManager.getInstance();
        CoroutineScope scope = CoroutineScopeKt.CoroutineScope(Dispatchers.getDefault());
        Deferred<List<? extends FileEditorProvider>> resolution = BuildersKt.async(scope, EmptyCoroutineContext.INSTANCE,
                CoroutineStart.LAZY, (coroutineScope, continuation) -> manager.getProvidersAsync(getProject(), file, continuation));
        CountDownLatch completed = new CountDownLatch(1);
        resolution.invokeOnCompletion(cause -> {
            completed.countDown();
            return Unit.INSTANCE;
        });
        try {
            WriteAction.run(() -> {
                resolution.start();
                boolean resolved = completed.await(5, TimeUnit.SECONDS);
                assertTrue("CSV provider selection must not wait for PSI read access", resolved);
                assertEquals(List.of(CsvFileEditorProviderBase.TEXT_EDITOR_TYPE_ID,
                                CsvFileEditorProviderBase.TABLE_EDITOR_TYPE_ID),
                        resolution.getCompleted().stream().map(FileEditorProvider::getEditorTypeId).toList());
            });
        } finally {
            CoroutineScopeKt.cancel(scope, null);
            PlatformTestUtil.waitWithEventsDispatching("Provider selection must stop", resolution::isCompleted, 10);
        }
    }
}
