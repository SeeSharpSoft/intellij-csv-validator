package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter;
import com.intellij.psi.PsiTreeChangeListener;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class CsvTableModelBase<T extends PsiFileHolder> implements CsvTableModel {
    private final T myPsiFileHolder;

    private int myCachedRowCount = -1;
    private int myCachedColumnCount = -1;
    private CsvEscapeCharacter myCachedEscapeCharacter;
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
        myPsiTreeUpdater.dispose();
        getPsiFile().getManager().removePsiTreeChangeListener(myPsiTreeChangeListener);
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
        myCachedEscapeCharacter = null;
    }

    private PsiElement resetPointer() {
        myPointedRecord = PsiTreeUtil.findChildOfType(getPsiFile(), CsvRecord.class);
        myPointedRow = 0;
        return myPointedRecord;
    }

    protected CsvPsiTreeUpdater getPsiTreeUpdater() {
        return myPsiTreeUpdater;
    }

    protected CsvEscapeCharacter getEscapeCharacter() {
        if (myCachedEscapeCharacter == null) {
            myCachedEscapeCharacter = CsvHelper.getEscapeCharacter(getPsiFile());
        }
        return myCachedEscapeCharacter;
    }

    @Override
    public PsiElement getFieldAt(int row, int column) {
        int diffToCurrent = Math.abs(myPointedRow - row);
        if (diffToCurrent > row || myPointedRecord == null) {
            resetPointer();
            diffToCurrent = row;
        }
        // fail gracefully or not?!
        if (myPointedRecord == null) return null;
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
        if (commitImmediately) updater.commit();
    }

    @Override
    public String getValue(int rowIndex, int columnIndex) {
        PsiElement field = getFieldAt(rowIndex, columnIndex);
        return CsvHelper.getFieldValue(field, getEscapeCharacter());
    }

    @Override
    public void addRow(int anchorRowIndex, boolean before) {
        CsvRecord row = PsiHelper.getNthChildOfType(getPsiFile(), anchorRowIndex, CsvRecord.class);
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
    public void addColumn(int anchorColumnIndex, boolean before) {
        CsvPsiTreeUpdater updater = getPsiTreeUpdater();
        getPsiTreeUpdater().addColumn(anchorColumnIndex, before);
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
