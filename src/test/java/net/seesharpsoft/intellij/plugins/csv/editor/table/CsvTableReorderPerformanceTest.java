package net.seesharpsoft.intellij.plugins.csv.editor.table;

import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvLexerFactory;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class CsvTableReorderPerformanceTest extends CsvBasePlatformTestCase {
    private CsvTableReorder.Change prepare(String text, int[] order, boolean columns) {
        return CsvTableReorder.prepare(text, CsvLexerFactory.getInstance().createLexer(
                        CsvValueSeparator.COMMA, CsvEscapeCharacter.QUOTE, "#"),
                order, columns, CsvValueSeparator.COMMA, CsvEscapeCharacter.QUOTE, "#");
    }

    public void testOneHundredThousandRowsAndTwelveColumnsWithoutPsi() {
        String text = "0,1,2,3,4,5,6,7,8,9,10,11\n".repeat(100_000);
        String expected = "11,0,1,2,3,4,5,6,7,8,9,10\n".repeat(100_000);
        long start = System.nanoTime();
        CsvTableReorder.Change change = prepare(text, new int[]{11, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, true);
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertNotNull(change);
        assertEquals(expected, text.substring(0, change.range().getStartOffset()) + change.text() +
                text.substring(change.range().getEndOffset()));
        assertTrue("Reordering 1.2 million cells took " + millis + " ms", millis < 5000);
        System.out.println("100,000 rows x 12 columns, no PSI: " + millis + " ms");
    }

    public void testRowMoveOnlyReplacesTheAffectedSpan() {
        String text = "a,b\n".repeat(50_000) + "c,d\ne,f\n" + "a,b\n".repeat(50_000);
        int[] order = IntStream.rangeClosed(0, 100_002).toArray();
        order[50_000] = 50_001;
        order[50_001] = 50_000;
        CsvTableReorder.Change change = prepare(text, order, false);
        assertNotNull(change);
        assertTrue("A two-row move must not replace the entire file", change.range().getLength() <= 8);
        assertEquals("a,b\n".repeat(50_000) + "e,f\nc,d\n" + "a,b\n".repeat(50_000),
                text.substring(0, change.range().getStartOffset()) + change.text() +
                        text.substring(change.range().getEndOffset()));
    }
}
