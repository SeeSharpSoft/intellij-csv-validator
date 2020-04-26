package net.seesharpsoft.intellij.plugins.csv.parser;

import com.intellij.testFramework.ParsingTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvParserDefinition;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public class CsvParsingTest extends ParsingTestCase {

    public CsvParsingTest() {
        super("", "csv", new CsvParserDefinition());
    }

    public void testParsingTestData() {
        doTest(true);
    }

    public void testParsingTestDataWithCustomParser() {
        setName("ParsingTestData");
        CsvEditorSettings.getInstance().setDefaultValueSeparator(CsvValueSeparator.create(","));
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
