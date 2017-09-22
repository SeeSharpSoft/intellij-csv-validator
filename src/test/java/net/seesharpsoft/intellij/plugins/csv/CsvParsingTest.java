package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.testFramework.ParsingTestCase;

public class CsvParsingTest extends ParsingTestCase {

    public CsvParsingTest() {
        super("", "csv", new CsvParserDefinition());
    }

    public void testParsingTestData() {
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
