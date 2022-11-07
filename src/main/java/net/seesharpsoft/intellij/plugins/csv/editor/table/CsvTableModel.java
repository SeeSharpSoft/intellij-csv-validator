package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public interface CsvTableModel extends PsiFileHolder, Suspendable {

    static int getColumnCount(PsiFile psiFile) {
        final AtomicInteger maxColumnCount = new AtomicInteger(0);
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

    static int getRowCount(PsiFile psiFile) {
        if (psiFile == null) return 0;

        int counter = 0;
        PsiElement rowElement;
        for (rowElement = psiFile.getFirstChild();
             rowElement != null && !(rowElement instanceof PsiErrorElement);
             rowElement = rowElement.getNextSibling())
        {
            if (rowElement instanceof CsvRecord) counter++;
        }

        // TODO support for showing the error in table editor?!
//        if (rowElement instanceof PsiErrorElement) counter++;

        return counter;
    }

    void notifyUpdate();

    default boolean isCommentRow(int rowIndex) {
        PsiElement field = getFieldAt(rowIndex, 0);
        return CsvHelper.isCommentElement(field);
    }

    default boolean hasErrors() {
        PsiFile psiFile = getPsiFile();
        return psiFile != null && PsiTreeUtil.hasErrorElements(psiFile);
    }

    default int getRowCount() {
        return getRowCount(getPsiFile());
    }

    default int getColumnCount() {
        return getColumnCount(getPsiFile());
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

    void addRow(int anchorRowIndex, boolean before);

    default void removeRow(int rowIndex) {
        removeRows(Collections.singletonList(rowIndex));
    }

    void removeRows(Collection<Integer> indices);

    void addColumn(int anchorColumnIndex, boolean before);

    default void removeColumn(int columnIndex) {
        removeColumns(Collections.singletonList(columnIndex));
    }

    void removeColumns(Collection<Integer> indices);

    void clearCells(Collection<Integer> rows, Collection<Integer> columns);

    @Override
    default void dispose() {
        Suspendable.super.dispose();
    }

    default void resume() {
        Suspendable.super.resume();
        notifyUpdate();
    }
}
