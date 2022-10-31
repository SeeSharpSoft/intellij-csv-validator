package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.table.api.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class CsvTableModelBase<T extends PsiFileHolder> implements CsvTableModel {
    private final T myPsiFileHolder;

    private int myCachedRowCount = -1;
    private int myCachedColumnCount = -1;
    private Boolean myCachedHasErrors = null;

    private int myPointedRow = -1;
    private PsiElement myPointedRecord = null;

    private final CsvPsiTreeUpdater myPsiTreeUpdater;

    private final PsiTreeChangeListener myPsiTreeChangeListener = new PsiTreeAnyChangeAbstractAdapter() {
        @Override
        protected void onChange(@Nullable PsiFile file) {
            onPsiTreeChanged(file);
        }
    };

    public CsvTableModelBase(@NotNull T psiFileHolder) {
        myPsiFileHolder = psiFileHolder;
        myPsiTreeUpdater = new CsvPsiTreeUpdater(psiFileHolder);
        myPsiTreeUpdater.addCommitListener(() -> onPsiTreeChanged(getPsiFile()));
        getPsiFile().getManager().addPsiTreeChangeListener(myPsiTreeChangeListener, myPsiFileHolder);
    }

    public T getPsiFileHolder() {
        return myPsiFileHolder;
    }

    @Override
    public void dispose() {
        CsvTableModel.super.dispose();
        getPsiFile().getManager().removePsiTreeChangeListener(myPsiTreeChangeListener);
        myPsiTreeUpdater.dispose();
    }

    private void onPsiTreeChanged(@Nullable PsiFile file) {
        if (file == getPsiFile() && !myPsiTreeUpdater.isSuspended() && !isSuspended()) {
            notifyUpdate();
        }
    }

    @Override
    public PsiFile getPsiFile() {
        return myPsiFileHolder.getPsiFile();
    }

    @Override
    public void notifyUpdate() {
        this.resetCachedValues();
        this.resetPointer();
    }

    private void resetCachedValues() {
        myCachedRowCount = -1;
        myCachedColumnCount = -1;
        myCachedHasErrors = null;
    }

    private PsiElement resetPointer() {
        myPointedRecord = PsiTreeUtil.findChildOfType(getPsiFile(), CsvRecord.class);
        myPointedRow = 0;
        return myPointedRecord;
    }

    protected CsvPsiTreeUpdater getPsiTreeUpdater() {
        return myPsiTreeUpdater;
    }

    @Override
    public PsiElement getFieldAt(int row, int column) {
        int diffToCurrent = Math.abs(myPointedRow - row);
        if (diffToCurrent > row || myPointedRecord == null) {
            resetPointer();
            diffToCurrent = row;
        }
        assert myPointedRecord != null;

        CsvRecord record = PsiHelper.getNthSiblingOfType(myPointedRecord, diffToCurrent, CsvRecord.class, myPointedRow > row);
        if (record == null) return null;

        myPointedRecord = record;
        myPointedRow = row;

        if (PsiHelper.getElementType(record.getFirstChild()) == CsvTypes.COMMENT) return record.getFirstChild();

        return PsiHelper.getNthChildOfType(record, column, CsvField.class);
    }

    @Override
    public boolean hasErrors() {
        if (myCachedHasErrors == null) {
            myCachedHasErrors = CsvTableModel.super.hasErrors();
        }
        return myCachedHasErrors;
    }

    @Override
    public int getRowCount() {
        if (myCachedRowCount == -1) {
            myCachedRowCount = CsvTableModel.super.getRowCount();
        }
        return myCachedRowCount;
    }

    @Override
    public int getColumnCount() {
        if (myCachedColumnCount == -1) {
            myCachedColumnCount = CsvTableModel.super.getColumnCount();
        }
        return myCachedColumnCount;
    }

    @Override
    public void setValue(String value, int rowIndex, int columnIndex) {
        setValueAt(value, rowIndex, columnIndex, true);
    }

    private void createMissingColumns(int rowIndex, int columnIndex) {
        int currentColumnCount = getColumnCount(rowIndex);
        PsiElement field = getFieldAt(rowIndex, currentColumnCount - 1);
        getPsiTreeUpdater().appendEmptyFields(field, columnIndex - currentColumnCount + 1);
    }

    private int getColumnCount(int rowIndex) {
        return getColumnCount(PsiHelper.getNthChildOfType(getPsiTreeUpdater().getPsiFile(), rowIndex, CsvRecord.class));
    }

    private int getColumnCount(PsiElement record) {
        if (record == null) return -1;
        return PsiTreeUtil.countChildrenOfType(record, CsvField.class);
    }

    public void setValueAt(String value, int rowIndex, int columnIndex, boolean commitImmediately) {
        PsiElement field = getFieldAt(rowIndex, columnIndex);
        CsvPsiTreeUpdater updater = getPsiTreeUpdater();
        if (field == null) {
            int currentColumnCount = getColumnCount(rowIndex);
            field = getFieldAt(rowIndex, currentColumnCount - 1);
            updater.appendField(field, value, true);
            updater.appendEmptyFields(field, columnIndex - currentColumnCount);
        } else {
            if (CsvHelper.isCommentElement(field)) {
                updater.replaceComment(field, value);
            } else {
                updater.replaceField(field, value, columnIndex == 0);
            }
        }
        updater.commit();
    }

    @Override
    public String getValue(int rowIndex, int columnIndex) {
        PsiElement field = getFieldAt(rowIndex, columnIndex);
        String value = field == null ? "" : field.getText();
        return CsvHelper.isCommentElement(field) ?
                value.substring(CsvEditorSettings.getInstance().getCommentIndicator().length()) :
                CsvHelper.unquoteCsvValue(value, getEscapeCharacter());
    }

    @Override
    public void addRow(int focusedRowIndex, boolean before) {
        CsvRecord row = PsiHelper.getNthChildOfType(getPsiFile(), focusedRowIndex, CsvRecord.class);
        getPsiTreeUpdater().addRow(row, before);
        getPsiTreeUpdater().commit();
    }

    @Override
    public void removeRows(Collection<Integer> indices) {
        CsvPsiTreeUpdater updater = getPsiTreeUpdater();
        updater.deleteRows(indices);
        updater.commit();
    }

    @Override
    public void addColumn(int focusedColumnIndex, boolean before) {
        CsvPsiTreeUpdater updater = getPsiTreeUpdater();
        getPsiTreeUpdater().addColumn(focusedColumnIndex, before);
        updater.commit();
    }

    @Override
    public void removeColumns(Collection<Integer> indices) {
        CsvPsiTreeUpdater updater = getPsiTreeUpdater();
        updater.deleteColumns(indices);
        updater.commit();
    }

    @Override
    public void clearCells(Collection<Integer> rows, Collection<Integer> columns) {
        for (int currentColumn : columns) {
            for (int currentRow : rows) {
                setValueAt("", currentRow, currentColumn, false);
            }
        }
        getPsiTreeUpdater().commit();
    }
}
