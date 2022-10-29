package net.seesharpsoft.intellij.plugins.csv.editor.table.api;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvVisitor;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import net.seesharpsoft.intellij.psi.PsiHelper;
import net.seesharpsoft.intellij.util.Suspendable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public interface CsvTableModel extends PsiFileHolder, Suspendable {

    void notifyUpdate();

    default CsvEscapeCharacter getEscapeCharacter() {
        return CsvHelper.getEscapeCharacter(getPsiFile());
    }

    default CsvValueSeparator getValueSeparator() {
        return CsvHelper.getValueSeparator(getPsiFile());
    }

    default boolean isCommentRow(int rowIndex) {
        PsiElement field = getFieldAt(rowIndex, 0);
        return CsvHelper.isCommentElement(field);
    }

    default boolean hasErrors() {
        PsiFile psiFile = getPsiFile();
        return psiFile != null && PsiTreeUtil.hasErrorElements(psiFile);
    }

    default int getRowCount() {
        PsiFile psiFile = getPsiFile();
        return psiFile != null ? PsiTreeUtil.countChildrenOfType(getPsiFile(), CsvRecord.class) : 0;
    }

    default int getColumnCount() {
        final AtomicInteger maxColumnCount = new AtomicInteger(0);
        PsiFile psiFile = getPsiFile();
        if (psiFile != null) {
            psiFile.acceptChildren(new CsvVisitor() {
                @Override
                public void visitRecord(@NotNull CsvRecord csvRecord) {
                    maxColumnCount.set(Math.max(maxColumnCount.get(), PsiTreeUtil.countChildrenOfType(csvRecord, CsvField.class)));
                }
            });
        }
        return maxColumnCount.get();
    }

    @Nullable
    default PsiElement getFieldAt(int row, int column) {
        CsvRecord record = PsiHelper.getNextNthSiblingOfType(getPsiFile(), row, CsvRecord.class);
        if (record == null) return null;

        if (PsiHelper.getElementType(record.getFirstChild()) == CsvTypes.COMMENT) return record.getFirstChild();

        return PsiHelper.getNthChildOfType(record, column, CsvField.class);
    }

    @NotNull
    String getValue(int rowIndex, int columnIndex);

    void setValue(String value, int rowIndex, int columnIndex);

    void addRow(int focusedRowIndex, boolean before);

    void removeRows(int[] indices);

    void addColumn(int focusedColumnIndex, boolean before);

    void removeColumns(int[] indices);

    void clearCells(int[] rows, int[] columns);

    @Override
    default void dispose() {
        Suspendable.super.dispose();
    }

    default void resume() {
        Suspendable.super.resume();
        notifyUpdate();
    }
}
