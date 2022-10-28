package net.seesharpsoft.intellij.plugins.csv.editor.table.api;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvVisitor;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public interface CsvTableModel {

    @NotNull
    CsvTableEditor getEditor();

    void notifyUpdate();

    default CsvEscapeCharacter getEscapeCharacter() {
        return this.getEditor().getEscapeCharacter();
    }

    default CsvValueSeparator getValueSeparator() {
        return this.getEditor().getValueSeparator();
    }

    default boolean hasErrors() {
        if (!getEditor().isValid()) {
            return true;
        }
        return PsiTreeUtil.hasErrorElements(getEditor().getCsvFile());
    }

    default int getRowCount() {
        if (getEditor().isValid()) {
            return PsiTreeUtil.countChildrenOfType(this.getEditor().getCsvFile(), CsvRecord.class);
        }
        return 0;
    }

    default int getColumnCount() {
        final AtomicInteger maxColumnCount = new AtomicInteger(0);
        if (getEditor().isValid()) {
            getEditor().getCsvFile().acceptChildren(new CsvVisitor() {
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
        CsvRecord record = PsiHelper.getNextNthSiblingOfType(this.getEditor().getCsvFile(), row, CsvRecord.class);
        if (record == null) return null;

        if (PsiHelper.getElementType(record.getFirstChild()) == CsvTypes.COMMENT) return record.getFirstChild();

        return PsiHelper.getNthChildOfType(record, column, CsvField.class);
    }

    @NotNull
    String getValueAt(int rowIndex, int columnIndex);

    void setValueAt(String value, int rowIndex, int columnIndex);

    void addRow(int focusedRowIndex, boolean before);

    void removeRows(int[] indices);

    void addColumn(int focusedColumnIndex, boolean before);

    void removeColumns(int[] indices);

    void clearCells(int[] rows, int[] columns);
}
