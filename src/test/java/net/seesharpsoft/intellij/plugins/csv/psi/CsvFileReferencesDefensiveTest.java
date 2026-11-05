package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistryImpl;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ProcessingContext;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;

public class CsvFileReferencesDefensiveTest extends CsvBasePlatformTestCase {

    private int referenceProviderCalls;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ReferenceProvidersRegistryImpl registry = (ReferenceProvidersRegistryImpl) ReferenceProvidersRegistry.getInstance();
        registry.getRegistrar(CsvLanguage.INSTANCE).registerReferenceProvider(
                PlatformPatterns.psiElement(CsvFile.class), new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                          @NotNull ProcessingContext context) {
                        referenceProviderCalls++;
                        return new PsiReference[]{new PsiReferenceBase.Immediate<>(element, TextRange.create(0, 1), element)};
                    }
                }, PsiReferenceRegistrar.DEFAULT_PRIORITY, getTestRootDisposable());
    }

    public void testPhysicalCsvFileReturnsContributedReferences() {
        PsiFile psiFile = myFixture.addFileToProject("refs/physical.csv", "x,y\n1,2");

        PsiReference[] references = psiFile.getReferences();

        assertEquals(1, references.length);
        assertSame(psiFile, references[0].resolve());
        assertTrue(referenceProviderCalls > 0);
    }

    public void testNonPhysicalCsvFileReturnsNoReferences() {
        LightVirtualFile vFile = new LightVirtualFile("NonPhysical.csv", CsvFileType.INSTANCE, "a,b\nc,d");
        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(vFile);
        assertNotNull(psiFile);
        assertInstanceOf(psiFile, CsvFile.class);
        assertSame(vFile, psiFile.getVirtualFile());
        assertTrue("Light files can participate in PSI events without representing real files", psiFile.isPhysical());

        PsiReference[] refs = psiFile.getReferences();
        assertNotNull(refs);
        assertEquals(0, refs.length);
        assertEquals(0, referenceProviderCalls);
    }

    public void testNonPhysicalFileSystemReturnsNoReferences() throws Exception {
        VirtualFile vFile = WriteAction.compute(() -> {
            VirtualFile root = DummyFileSystem.getInstance().createRoot(getTestName(true));
            VirtualFile file = root.createChildData(this, "nonPhysical.csv");
            VfsUtil.saveText(file, "a,b\nc,d");
            return file;
        });
        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(vFile);
        assertNotNull(psiFile);
        assertInstanceOf(psiFile, CsvFile.class);
        assertSame(vFile, psiFile.getVirtualFile());
        assertFalse(vFile instanceof LightVirtualFile);
        assertTrue(psiFile.isPhysical());

        assertEmpty(psiFile.getReferences());
        assertEquals(0, referenceProviderCalls);
    }

    public void testCsvFileWithoutPsiEventsReturnsNoReferences() {
        VirtualFile vFile = myFixture.addFileToProject("refs/noEvents.csv", "x,y\n1,2").getVirtualFile();
        SingleRootFileViewProvider provider = new SingleRootFileViewProvider(PsiManager.getInstance(getProject()), vFile, false);
        PsiFile psiFile = provider.getPsi(CsvLanguage.INSTANCE);
        assertNotNull(psiFile);
        assertNull(psiFile.getVirtualFile());

        assertEmpty(psiFile.getReferences());
        assertEquals(0, referenceProviderCalls);
    }

    public void testInvalidatedCsvFileReturnsNoReferences() throws Exception {
        PsiFile psiFile = myFixture.addFileToProject("refs/physical.csv", "x,y\n1,2");
        assertInstanceOf(psiFile, CsvFile.class);
        VirtualFile vFile = psiFile.getVirtualFile();
        assertNotNull(vFile);

        // Invalidate file by deleting the underlying VirtualFile
        WriteAction.run(() -> {
            try {
                vFile.delete(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertFalse("PsiFile should be invalid after deleting VirtualFile", psiFile.isValid());

        // Must not throw, and must return empty references
        PsiReference[] refs = psiFile.getReferences();
        assertNotNull(refs);
        assertEquals(0, refs.length);
        assertEquals(0, referenceProviderCalls);
    }
}
