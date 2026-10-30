package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableSearchSessionBase.Match;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class CsvTableReplacementPerformanceTest extends CsvBasePlatformTestCase {
    public void testThirtyTwoRowsAndFourThousandColumnsWithoutPsi() {
        int rows = 32;
        int columns = 4096;
        String text = ("cat,".repeat(columns - 1) + "cat\n").repeat(rows);
        List<Match> matches = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                matches.add(new Match(row, column, 0, 3));
            }
        }
        AtomicInteger reads = new AtomicInteger();
        CharSequence snapshot = new CountingSequence(text, reads);
        long start = System.nanoTime();
        CsvTableReplacement.Change change = prepare(snapshot, matches, columns);
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertNotNull(change);
        assertEquals(text.replace("cat", "\"dog,fish\""), apply(text, change));
        assertTrue("Source traversal must be linear: " + reads.get() + " character reads",
                reads.get() < text.length() * 12);
        assertTrue("Replacing 131,072 cells took " + millis + " ms", millis < 5000);
        System.out.println("32 rows x 4,096 columns, no PSI: " + millis + " ms, " + reads + " character reads");
    }

    public void testSparseChangesOnlyReplaceTheirEnclosingSpan() {
        String text = "untouched,cat,untouched\nuntouched,cat,untouched\n";
        CsvTableReplacement.Change change = prepare(text,
                List.of(new Match(0, 1, 0, 3), new Match(1, 1, 0, 3)), 3);
        assertNotNull(change);
        assertEquals(10, change.range().getStartOffset());
        assertEquals(text.lastIndexOf("cat") + 3, change.range().getEndOffset());
        assertEquals(text.replace("cat", "\"dog,fish\""), apply(text, change));
    }

    public void testCancellationDuringTraversalOfAWideUnmatchedRow() {
        String text = "untouched,".repeat(4096) + "cat\n";
        AtomicInteger reads = new AtomicInteger();
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        CharSequence snapshot = new CountingSequence(text, reads) {
            @Override
            public char charAt(int index) {
                if (reads.get() >= 1024) indicator.cancel();
                return super.charAt(index);
            }
        };
        assertThrows(ProcessCanceledException.class, () -> ProgressManager.getInstance().runProcess(() -> {
            prepare(snapshot, List.of(new Match(0, 4096, 0, 3)), 4097);
            return null;
        }, indicator));
        assertTrue("Cancellation must interrupt source traversal", reads.get() < text.length());
    }

    public void testCommentsTreatReplacementAsContent() throws Exception {
        myFixture.configureByText(CsvFileType.INSTANCE, "#cat\n");
        CsvTableReplacement.Settings settings = CsvTableReplacement.Settings.capture(myFixture.getFile());
        CsvPsiTreeUpdater writer = new CsvPsiTreeUpdater(myFixture.getFile());
        try {
            for (String replacement : List.of("dog", "#dog", " #dog", "\n#dog", "dog\nnext line", "")) {
                FindModel query = new FindModel();
                query.setStringToFind("cat");
                query.setStringToReplace(replacement);
                var comment = writer.createCommentFromContent(replacement);
                String expected = comment == null ? "#cat\n" : comment.getText() + "\n";
                CsvTableReplacement.Change change = CsvTableReplacement.prepare("#cat\n",
                        List.of(new Match(0, 0, 0, 3)), null, new int[]{0}, query, FindManager.getInstance(getProject()), settings);
                assertEquals("Replacement: " + replacement, expected, change == null ? "#cat\n" : apply("#cat\n", change));
            }
        } finally {
            writer.dispose();
        }
    }

    public void testCustomSeparatorEscapeCommentsAndMultilineValues() throws Exception {
        String text = "##cat\n\"cat\\\"cat\ncat\"||cat\n";
        FindModel query = new FindModel();
        query.setStringToFind("cat");
        query.setStringToReplace("dog\"fish");
        CsvTableReplacement.Change change = CsvTableReplacement.prepare(text, List.of(
                        new Match(0, 0, 0, 3), new Match(1, 0, 0, 3), new Match(1, 0, 4, 7),
                        new Match(1, 0, 8, 11), new Match(1, 1, 0, 3)),
                null, new int[]{0, 1}, query, FindManager.getInstance(getProject()),
                new CsvTableReplacement.Settings(CsvValueSeparator.create("||"), CsvEscapeCharacter.BACKSLASH, "##", false));
        assertNotNull(change);
        assertEquals("##dog\"fish\n\"dog\\\"fish\\\"dog\\\"fish\ndog\\\"fish\"||\"dog\\\"fish\"\n", apply(text, change));
    }

    private CsvTableReplacement.Change prepare(CharSequence text, List<Match> matches, int columns) {
        FindModel query = new FindModel();
        query.setStringToFind("cat");
        query.setStringToReplace("dog,fish");
        try {
            return CsvTableReplacement.prepare(text, matches, null, IntStream.range(0, columns).toArray(),
                    query, FindManager.getInstance(getProject()), new CsvTableReplacement.Settings(
                            CsvValueSeparator.COMMA, CsvEscapeCharacter.QUOTE, "#", false));
        } catch (FindManager.MalformedReplacementStringException exception) {
            throw new AssertionError(exception);
        }
    }

    private String apply(String text, CsvTableReplacement.Change change) {
        return text.substring(0, change.range().getStartOffset()) + change.text() + text.substring(change.range().getEndOffset());
    }

    private static class CountingSequence implements CharSequence {
        private final String text;
        private final AtomicInteger reads;

        private CountingSequence(String text, AtomicInteger reads) {
            this.text = text;
            this.reads = reads;
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            reads.incrementAndGet();
            return text.charAt(index);
        }

        @Override
        public @NotNull CharSequence subSequence(int start, int end) {
            return new CountingSequence(text.substring(start, end), reads);
        }

        @Override
        public @NotNull String toString() {
            return text;
        }
    }
}
