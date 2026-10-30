package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.psi.PsiFile;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;

public class CsvFieldPositionTest extends CsvBasePlatformTestCase {

    public void testOffsetInsideFieldMapsToRecordAndFieldIndex() {
        PsiFile file = configure("a,bb,c\nd,e,f\n");

        assertEquals(new CsvFieldPosition(0, 1), CsvFieldPosition.atOffset(file, 3));
        assertEquals(new CsvFieldPosition(1, 2), CsvFieldPosition.atOffset(file, 11));
    }

    public void testOffsetBeforeSeparatorBelongsToPrecedingField() {
        PsiFile file = configure("a,bb,c");

        assertEquals(new CsvFieldPosition(0, 1), CsvFieldPosition.atOffset(file, 4));
    }

    public void testOffsetAfterSeparatorBelongsToFollowingField() {
        PsiFile file = configure("a,bb,c");

        assertEquals(new CsvFieldPosition(0, 2), CsvFieldPosition.atOffset(file, 5));
    }

    public void testOffsetAtLineEndBelongsToLastFieldOfRecord() {
        PsiFile file = configure("a,b\nc,d\n");

        assertEquals(new CsvFieldPosition(0, 1), CsvFieldPosition.atOffset(file, 3));
    }

    public void testOffsetAtEndOfFileBelongsToLastField() {
        assertEquals(new CsvFieldPosition(1, 1), CsvFieldPosition.atOffset(configure("a,b\nc,d"), 7));
        assertEquals(new CsvFieldPosition(1, 1), CsvFieldPosition.atOffset(configure("a,b\nc,d\n"), 8));
    }

    public void testOffsetBetweenSeparatorsMapsToEmptyField() {
        PsiFile file = configure("a,,c");

        assertEquals(new CsvFieldPosition(0, 1), CsvFieldPosition.atOffset(file, 2));
    }

    public void testEmptyLineIsOwnRecord() {
        PsiFile file = configure("a\n\nb");

        assertEquals(new CsvFieldPosition(1, 0), CsvFieldPosition.atOffset(file, 2));
        assertEquals(new CsvFieldPosition(2, 0), CsvFieldPosition.atOffset(file, 3));
    }

    public void testQuotedMultilineValueIsSingleField() {
        PsiFile file = configure("\"x\ny\",z\nw");

        assertEquals(new CsvFieldPosition(0, 0), CsvFieldPosition.atOffset(file, 3));
        assertEquals(new CsvFieldPosition(0, 1), CsvFieldPosition.atOffset(file, 6));
        assertEquals(new CsvFieldPosition(1, 0), CsvFieldPosition.atOffset(file, 8));
    }

    public void testCommentRecordIsSingleColumnRow() {
        PsiFile file = configure("#note, with separator\na,b");

        assertEquals(new CsvFieldPosition(0, 0), CsvFieldPosition.atOffset(file, 10));
        assertEquals(new CsvFieldPosition(1, 1), CsvFieldPosition.atOffset(file, 24));
    }

    public void testTabSeparatedFieldsMapToColumns() {
        PsiFile file = myFixture.configureByText("test.tsv", "a\tb\tc");

        assertEquals(new CsvFieldPosition(0, 2), CsvFieldPosition.atOffset(file, 4));
    }

    public void testStartOffsetOfFieldIsItsFirstCharacter() {
        PsiFile file = configure("a,bb,c\nd,\"e\",f");

        assertEquals(2, new CsvFieldPosition(0, 1).findStartOffset(file));
        assertEquals(9, new CsvFieldPosition(1, 1).findStartOffset(file));
    }

    public void testStartOffsetOfEmptyFieldIsBetweenItsSeparators() {
        PsiFile file = configure("a,,c");

        assertEquals(2, new CsvFieldPosition(0, 1).findStartOffset(file));
    }

    public void testStartOffsetOfMissingColumnIsRecordEnd() {
        PsiFile file = configure("a,b,c\nd\n");

        assertEquals(7, new CsvFieldPosition(1, 2).findStartOffset(file));
    }

    public void testStartOffsetOfCommentRowIsLineStart() {
        PsiFile file = configure("a,b\n#note\n");

        assertEquals(4, new CsvFieldPosition(1, 1).findStartOffset(file));
    }

    public void testStartOffsetOfMissingRowIsNegative() {
        PsiFile file = configure("a,b\n");

        assertEquals(-1, new CsvFieldPosition(5, 0).findStartOffset(file));
    }

    public void testStartOffsetOfEveryFieldMapsBackToThatField() {
        PsiFile file = configure("name,\"multi\nline\",,x\n#comment\n\n\"q\"\"uote\",last\n");

        for (int offset = 0; offset <= file.getTextLength(); offset++) {
            CsvFieldPosition field = CsvFieldPosition.atOffset(file, offset);
            assertNotNull("offset " + offset, field);
            assertEquals("offset " + offset, field, CsvFieldPosition.atOffset(file, field.findStartOffset(file)));
        }
    }

    private PsiFile configure(String text) {
        return myFixture.configureByText("test.csv", text);
    }
}
