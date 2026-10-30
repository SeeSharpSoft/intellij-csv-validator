package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Position of a CSV field as shown by the table editor: the record index is the row, and the field index within
 * the record is the column. A comment record is a row with a single column.
 * Callers must hold read access to the committed PSI file.
 */
public record CsvFieldPosition(int row, int column) {

    /**
     * Returns the field containing the text offset. An offset at the end of a field, e.g. before a value separator
     * or line break, belongs to that field.
     */
    public static @Nullable CsvFieldPosition atOffset(@NotNull PsiFile psiFile, int offset) {
        CsvRecord record = findRecordAt(psiFile, offset);
        if (record == null) return null;
        return new CsvFieldPosition(PsiHelper.getChildIndexOfType(psiFile, record, CsvRecord.class), getColumnAt(record, offset));
    }

    /**
     * Returns the start offset of this field, the start of a comment record, or the end of a record without this
     * column. Returns -1 if the file has no such record.
     */
    public int findStartOffset(@NotNull PsiFile psiFile) {
        CsvRecord record = PsiHelper.getNthChildOfType(psiFile, row, CsvRecord.class);
        if (record == null) return -1;
        CsvField field = PsiHelper.getNthChildOfType(record, column, CsvField.class);
        if (field != null) return field.getTextRange().getStartOffset();
        return CsvHelper.isCommentElement(record.getFirstChild())
                ? record.getTextRange().getStartOffset()
                : record.getTextRange().getEndOffset();
    }

    private static @Nullable CsvRecord findRecordAt(@NotNull PsiFile psiFile, int offset) {
        PsiElement leaf = psiFile.findElementAt(offset);
        if (leaf == null && offset > 0) {
            leaf = psiFile.findElementAt(offset - 1);
        }
        if (leaf != null && leaf.getParent() == psiFile && PsiHelper.getElementType(leaf) == CsvTypes.CRLF) {
            return PsiTreeUtil.getPrevSiblingOfType(leaf, CsvRecord.class);
        }
        return PsiTreeUtil.getParentOfType(leaf, CsvRecord.class, false);
    }

    private static int getColumnAt(@NotNull CsvRecord record, int offset) {
        int column = 0;
        for (PsiElement child = record.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getTextRange().getEndOffset() > offset) break;
            if (PsiHelper.getElementType(child) == CsvTypes.COMMA) column++;
        }
        return column;
    }
}
