package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CsvTableModelBase<T extends PsiFileHolder> implements CsvTableModel {
    private final T myPsiFileHolder;
    private volatile boolean myDisposed = false;

    private int myCachedRowCount = -1;
    private int myCachedColumnCount = -1;
    private CsvEscapeCharacter myCachedEscapeCharacter;
    private Boolean myCachedHasErrors = null;
    private int myPointedRow = -1;
    private PsiElement myPointedRecord = null;
    private volatile Set<Integer> myCachedCommentRows = Collections.emptySet();
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
        addPsiTreeChangeListener();
        resetPointer();
        getRowCount();
    }

    public T getPsiFileHolder() {
        return myPsiFileHolder;
    }

    protected void addPsiTreeChangeListener() {
        PsiFile psiFile = getPsiFile();
        if (psiFile == null) {
            // Resolve PSI off-EDT and register the listener when available to avoid slow operations on EDT
            ReadAction
                .nonBlocking(this::getPsiFile)
                .coalesceBy(this)
                .finishOnUiThread(ModalityState.any(), pf -> {
                    if (pf == null || myDisposed) return;
                    PsiManager mgr = pf.getManager();
                    if (mgr == null) return;
                    mgr.addPsiTreeChangeListener(myPsiTreeChangeListener, myPsiFileHolder);
                })
                .submit(AppExecutorUtil.getAppExecutorService());
            return;
        }
        PsiManager manager = psiFile.getManager();
        if (manager == null) return;
        manager.addPsiTreeChangeListener(myPsiTreeChangeListener, myPsiFileHolder);
    }

    @Override
    public void dispose() {
        myDisposed = true;
        CsvTableModel.super.dispose();
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
    public boolean isCommentRow(int rowIndex) {
        return myCachedCommentRows.contains(rowIndex);
    }

    @Override
    public void notifyUpdate() {
        this.resetCachedValues();
        this.resetPointer();
        this.getRowCount();
    }

    private void resetCachedValues() {
        myCachedRowCount = -1;
        myCachedColumnCount = -1;
        myCachedHasErrors = null;
        myCachedEscapeCharacter = null;
        myCachedCommentRows = Collections.emptySet();
    }

    private void resetPointer() {
        ReadAction.run(() -> {
            PsiFile psiFile = getPsiFile();
            myPointedRecord = psiFile == null ? null : PsiHelper.getFirstChildOfType(psiFile, CsvRecord.class);
            myPointedRow = 0;
        });
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
        return ReadAction.compute(() -> {
            int diffToCurrent = Math.abs(myPointedRow - row);
            if (diffToCurrent > row || myPointedRecord == null) {
                resetPointer();
                diffToCurrent = row;
            }
            if (myPointedRecord == null) return null;

            CsvRecord record = PsiHelper.getNthSiblingOfType(myPointedRecord, diffToCurrent, CsvRecord.class, myPointedRow > row);
            if (record == null) return null;

            myPointedRecord = record;
            myPointedRow = row;

            if (PsiHelper.getElementType(record.getFirstChild()) == CsvTypes.COMMENT) return record.getFirstChild();

            return PsiHelper.getNthChildOfType(record, column, CsvField.class);
        });
    }

    @Override
    public boolean hasErrors() {
        if (myCachedHasErrors == null) {
            myCachedHasErrors = ReadAction.compute(CsvTableModel.super::hasErrors);
        }
        return myCachedHasErrors;
    }

    @Override
    public int getRowCount() {
        if (myCachedRowCount == -1) {
            myCachedRowCount = ReadAction.compute(() -> {
                PsiFile psiFile = getPsiFile();
                Set<Integer> commentRows = new HashSet<>();
                int rowCount = 0;
                if (psiFile != null) {
                    for (PsiElement child = psiFile.getFirstChild();
                         child != null && !(child instanceof PsiErrorElement);
                         child = child.getNextSibling()) {
                        if (child instanceof CsvRecord) {
                            if (PsiHelper.getElementType(child.getFirstChild()) == CsvTypes.COMMENT) {
                                commentRows.add(rowCount);
                            }
                            rowCount++;
                        }
                    }
                }
                myCachedCommentRows = commentRows;
                return rowCount;
            });
        }
        return myCachedRowCount;
    }

    @Override
    public int getColumnCount() {
        if (myCachedColumnCount == -1) {
            myCachedColumnCount = ReadAction.compute(CsvTableModel.super::getColumnCount);
        }
        return myCachedColumnCount;
    }

    @Override
    public void setValue(String value, int rowIndex, int columnIndex) {
        setValueAt(value, rowIndex, columnIndex, true);
    }

    private int getColumnCount(int rowIndex) {
        PsiFile psiFile = getPsiFile();
        if (psiFile == null) return 0;
        return getColumnCount(PsiHelper.getNthChildOfType(psiFile, rowIndex, CsvRecord.class));
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
            if (field != null) {
                updater.appendField(field, value, true);
                updater.appendEmptyFields(field, columnIndex - currentColumnCount);
            }
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
    public @NotNull String getValue(int rowIndex, int columnIndex) {
        return ReadAction.compute(() -> {
            PsiElement field = getFieldAt(rowIndex, columnIndex);
            return CsvHelper.getFieldValue(field, getEscapeCharacter());
        });
    }

    @Override
    public void addRow(int anchorRowIndex, boolean before) {
        CsvRecord row = PsiHelper.getNthChildOfType(getPsiFile(), anchorRowIndex, CsvRecord.class);
        if (row == null) return;
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
