package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.LanguageDocumentation;
import com.intellij.lang.documentation.CompositeDocumentationProvider;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import java.util.List;

public class CsvFieldDocumentationProviderTest extends CsvBasePlatformTestCase {

    private static final String REMOVED_FIELD_INFO_SEVERITY = "CSV_COLUMN_INFO_SEVERITY";

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

    public void testHoveredFieldShowsItsValueHeaderAndColumnNumber() {
        myFixture.configureByText("test.csv", "Header 1, Header 2\nValue 1, Val<caret>ue 2\n");

        assertEquals(" Value 2\n\nHeader:  Header 2\nIndex: 2", plainText(hoverDocumentation()));
    }

    public void testFieldDocumentationEscapesValueAndHeaderMarkup() {
        myFixture.configureByText("test.csv", "<b>bold</b>\nA &<caret> B\n");

        String documentation = hoverDocumentation();

        assertEquals("A & B\n\nHeader: <b>bold</b>\nIndex: 1", plainText(documentation));
        assertFalse(documentation, documentation.contains("<b>bold</b>"));
    }

    public void testFieldDocumentationHonorsZeroBasedColumnNumbering() {
        CsvEditorSettings.getInstance().setZeroBasedColumnNumbering(true);
        myFixture.configureByText("test.csv", "first,second\n1,<caret>2\n");

        assertEquals("2\n\nHeader: second\nIndex: 1", plainText(hoverDocumentation()));
    }

    public void testTsvFieldDocumentationUsesTheTabSeparatedHeader() {
        myFixture.configureByText("test.tsv", "first\tsecond\n1\t<caret>2\n");

        assertEquals("2\n\nHeader: second\nIndex: 2", plainText(hoverDocumentation()));
    }

    public void testFieldDocumentationIsHiddenWhenInfoBalloonIsDisabled() {
        CsvEditorSettings.getInstance().setShowInfoBalloon(false);
        myFixture.configureByText("test.csv", "first,second\n1,<caret>2\n");

        assertNull(hoverDocumentation());
    }

    public void testCommentRowsHaveNoFieldDocumentation() {
        myFixture.configureByText("test.csv", "# com<caret>ment\nfirst,second\n");

        assertNull(hoverDocumentation());
    }

    public void testHighlightingCreatesNoPerFieldOrSeparatorInfos() {
        CsvEditorSettings.getInstance().setShowInfoBalloon(true);
        CsvEditorSettings.getInstance().setHighlightTabSeparator(true);
        myFixture.configureByText("test.tsv", "first\tsecond\tthird\n" + "1\t2\t3\n".repeat(200));

        List<HighlightInfo> infos = myFixture.doHighlighting();

        assertEmpty(infos.stream()
                .filter(info -> REMOVED_FIELD_INFO_SEVERITY.equals(info.getSeverity().getName()))
                .toList());
    }

    private String hoverDocumentation() {
        DocumentationProvider provider = CompositeDocumentationProvider.wrapProviders(
                LanguageDocumentation.INSTANCE.allForLanguage(CsvLanguage.INSTANCE));
        PsiFile file = myFixture.getFile();
        int offset = myFixture.getCaretOffset();
        PsiElement context = file.findElementAt(offset);
        PsiElement target = provider.getCustomDocumentationElement(myFixture.getEditor(), file, context, offset);
        return target == null ? null : provider.generateHoverDoc(target, context);
    }

    private static String plainText(String documentation) {
        assertNotNull("Expected field documentation", documentation);
        String text = documentation.replace("<br />", "\n").replaceAll("<[^>]*>", "").replace("&nbsp;", " ");
        return StringUtil.unescapeXmlEntities(text);
    }
}
