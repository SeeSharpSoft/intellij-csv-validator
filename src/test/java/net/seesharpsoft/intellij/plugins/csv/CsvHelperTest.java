package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.testFramework.PlatformLiteFixture;

public class CsvHelperTest extends PlatformLiteFixture {

    public void testUnquoteCsvValue() {
        String csv = "\"Header\"\" \\\\1\\\\\"";
        assertEquals("Header\" \\\\1\\\\", CsvHelper.unquoteCsvValue(csv, CsvEscapeCharacter.QUOTE));
    }

    public void testUnquoteCsvValueWithBackslash() {
        String csv = "\"Header\\\" \\\\1\\\\\"";
        assertEquals("Header\" \\1\\", CsvHelper.unquoteCsvValue(csv, CsvEscapeCharacter.BACKSLASH));
    }

    public void testQuoteCsvValue() {
        String csv = "Header\" \\1\\";
        assertEquals( "\"Header\"\" \\1\\\"", CsvHelper.quoteCsvField(csv, CsvEscapeCharacter.QUOTE, CsvValueSeparator.COMMA, false));
    }

    public void testQuoteCsvValueWithBackslash() {
        String csv = "Header\" \\1\\";
        assertEquals("\"Header\\\" \\\\1\\\\\"",  CsvHelper.quoteCsvField(csv, CsvEscapeCharacter.BACKSLASH, CsvValueSeparator.COMMA, false));
    }
}
