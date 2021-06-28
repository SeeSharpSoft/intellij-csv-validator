package net.seesharpsoft.intellij.plugins.csv.parser;

import com.intellij.testFramework.ParsingTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvParserDefinition;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import static net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings.COMMENT_INDICATOR_DEFAULT;

public class CsvParsingTest extends ParsingTestCase {

    public CsvParsingTest() {
        super("", "csv", new CsvParserDefinition());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings.getInstance().setAutoDetectValueSeparator(false);
    }

    @Override
    protected void tearDown() throws Exception {
        CsvEditorSettings.getInstance().setAutoDetectValueSeparator(true);
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvEditorSettings.VALUE_SEPARATOR_DEFAULT);
        CsvEditorSettings.getInstance().setCommentIndicator(COMMENT_INDICATOR_DEFAULT);
        super.tearDown();
    }

    public void testParsingTestData() {
        // without comment support, default lexer is used
        CsvEditorSettings.getInstance().setCommentIndicator("");
        doTest(true);
    }

    public void testParsingTestDataWithCustomParser() {
        setName("ParsingTestData");
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.create(","));
        doTest(true);
    }

    public void testRecordSeparator() {
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.create("\u001E"));
        doTest(true);
    }

    public void testCustomMultiSymbolSeparator() {
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.create("~§"));
        doTest(true);
    }

    public void testColonSeparator() {
        // without comment support, default lexer is used
        CsvEditorSettings.getInstance().setCommentIndicator("");
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.COLON);
        doTest(true);
    }

    public void testAllSeparators() {
        // without comment support, default lexer is used
        CsvEditorSettings.getInstance().setCommentIndicator("");
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.COMMA);
        doTest(true);
    }

    public void testCsvWithComments() {
        // comment support by default (custom lexer is used)
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.COMMA);
        doTest(true);
    }

    public void testParsingTestDataWithAutoDetect() {
        setName("ParsingTestData");
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.PIPE);
        CsvEditorSettings.getInstance().setAutoDetectValueSeparator(true);
        doTest(true);
    }

    public void testColonSeparatorWithAutoDetect() {
        setName("ColonSeparator");
        CsvEditorSettings.getInstance().setCommentIndicator("");
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.PIPE);
        CsvEditorSettings.getInstance().setAutoDetectValueSeparator(true);
        doTest(true);
    }

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/parser";
    }

    @Override
    protected boolean skipSpaces() {
        return false;
    }

    @Override
    protected boolean includeRanges() {
        return true;
    }
}
