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

    public void testParsingTestData() {
        // without comment support, default lexer is used
        CsvEditorSettings.getInstance().setCommentIndicator("");
        doTest(true);
        CsvEditorSettings.getInstance().setCommentIndicator(COMMENT_INDICATOR_DEFAULT);
    }

    public void testParsingTestDataWithCustomParser() {
        setName("ParsingTestData");
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.create(","));
        doTest(true);
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvEditorSettings.VALUE_SEPARATOR_DEFAULT);
    }

    public void testCustomMultiSymbolSeparator() {
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.create("~§"));
        doTest(true);
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvEditorSettings.VALUE_SEPARATOR_DEFAULT);
    }

    public void testColonSeparator() {
        // without comment support, default lexer is used
        CsvEditorSettings.getInstance().setCommentIndicator("");
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.COLON);
        doTest(true);
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvEditorSettings.VALUE_SEPARATOR_DEFAULT);
        CsvEditorSettings.getInstance().setCommentIndicator(COMMENT_INDICATOR_DEFAULT);
    }

    public void testAllSeparators() {
        // without comment support, default lexer is used
        CsvEditorSettings.getInstance().setCommentIndicator("");
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.COMMA);
        doTest(true);
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvEditorSettings.VALUE_SEPARATOR_DEFAULT);
        CsvEditorSettings.getInstance().setCommentIndicator(COMMENT_INDICATOR_DEFAULT);
    }

    public void testCsvWithComments() {
        // comment support by default (custom lexer is used)
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.COMMA);
        doTest(true);
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvEditorSettings.VALUE_SEPARATOR_DEFAULT);
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
