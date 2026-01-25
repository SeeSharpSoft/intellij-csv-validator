package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.LightVirtualFile;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;

public class CsvFileReferencesDefensiveTest extends CsvBasePlatformTestCase {

    public void testNonPhysicalCsvFileReturnsNoReferences() {
        // Use a LightVirtualFile to guarantee a non-physical provider across platform versions
        LightVirtualFile vFile = new LightVirtualFile("NonPhysical.csv", CsvFileType.INSTANCE, "a,b\nc,d");
        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(vFile);
        assertNotNull(psiFile);
        assertInstanceOf(psiFile, CsvFile.class);
        // Ensure this is a non-physical file (LightVirtualFile-backed)
        assertFalse(psiFile.getViewProvider().isPhysical());

        PsiReference[] refs = psiFile.getReferences();
        assertNotNull(refs);
        assertEquals(0, refs.length);
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
    }
}
