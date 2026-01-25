package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import org.jetbrains.annotations.NotNull;

public class CsvParserDefinitionIntegrationTest extends CsvBasePlatformTestCase {

    public void testPsiFileUsesProviderFileTypeForCsv() {
        PsiFile psiFile = myFixture.addFileToProject("parser/TypeCheck.csv", "a,b\nc,d");
        assertInstanceOf(psiFile, CsvFile.class);
        VirtualFile vFile = psiFile.getVirtualFile();
        assertNotNull(vFile);
        FileType vfType = vFile.getFileType();
        assertEquals("csv", vfType.getDefaultExtension());
        assertSame(vfType, psiFile.getFileType());
    }

    public void testPsiFileUsesProviderFileTypeForTsv() {
        PsiFile psiFile = myFixture.addFileToProject("parser/TypeCheck.tsv", "a\tb\n\tc");
        assertInstanceOf(psiFile, CsvFile.class);
        VirtualFile vFile = psiFile.getVirtualFile();
        assertNotNull(vFile);
        FileType vfType = vFile.getFileType();
        assertEquals("tsv", vfType.getDefaultExtension());
        assertSame(vfType, psiFile.getFileType());
    }

    public void testCreateLexerOverloads() {
        CsvParserDefinition def = (CsvParserDefinition) LanguageParserDefinitions.INSTANCE.forLanguage(CsvLanguage.INSTANCE);
        assertNotNull(def);

        PsiFile psiFile = myFixture.addFileToProject("parser/Lexer.csv", "1,2,3");
        assertInstanceOf(psiFile, CsvFile.class);

        // The PsiFile overload should work and provide a lexer
        Lexer lexer = def.createLexer(psiFile);
        assertNotNull(lexer);

        // The Project overload is intentionally unsupported and should throw
        boolean threw = false;
        try {
            def.createLexer(getProject());
        } catch (UnsupportedOperationException expected) {
            threw = true;
        }
        assertTrue("Expected UnsupportedOperationException for createLexer(Project)", threw);
    }

    private static @NotNull CsvParserDefinition getCsvParserDefinition() {
        return (CsvParserDefinition) LanguageParserDefinitions.INSTANCE.forLanguage(CsvLanguage.INSTANCE);
    }
}
