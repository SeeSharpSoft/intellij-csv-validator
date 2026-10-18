package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter;
import com.intellij.psi.PsiTreeChangeListener;
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
    private volatile PsiFile myObservedPsiFile;
    private PsiManager myPsiManager;

    private final PsiTreeChangeListener myPsiTreeChangeListener = new PsiTreeAnyChangeAbstractAdapter() {
        @Override
        protected void onChange(@Nullable PsiFile file) {
            onPsiTreeChanged(file);
        }
    };

    public CsvTableModelBase(@NotNull T psiFileHolder) {
        myPsiFileHolder = psiFileHolder;
        myPsiTreeUpdater = new CsvPsiTreeUpdater(psiFileHolder);
        myPsiTreeUpdater.addCommitListener(this::onUpdaterCommitted);
        updatePsiFile(getPsiFile());
        resetPointer();
        getRowCount();
    }

    public T getPsiFileHolder() {
        return myPsiFileHolder;
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    private void updatePsiFile(@Nullable PsiFile psiFile) {
        myObservedPsiFile = psiFile;
        invalidateModelState();
        updatePsiListener(psiFile);
    }

    private void updatePsiListener(@Nullable PsiFile psiFile) {
        PsiManager newManager = psiFile == null ? null : psiFile.getManager();
        if (newManager == myPsiManager) return;

        detachPsiListener();
        if (newManager != null && !myDisposed) {
            newManager.addPsiTreeChangeListener(myPsiTreeChangeListener, myPsiFileHolder);
            myPsiManager = newManager;
        }
    }

    private void detachPsiListener() {
        if (myPsiManager != null) {
            myPsiManager.removePsiTreeChangeListener(myPsiTreeChangeListener);
            myPsiManager = null;
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;
        detachPsiListener();
        myObservedPsiFile = null;
        invalidateModelState();
        CsvTableModel.super.dispose();
        myPsiTreeUpdater.dispose();
    }

    private void onPsiTreeChanged(@Nullable PsiFile file) {
        if (myDisposed || !canNotifyUpdates()) return;

        PsiFile currentPsiFile = getPsiFile();
        if (file != null && !isSameVirtualFile(file, currentPsiFile)) {
            return;
        }

        notifyUpdate();
    }

    private boolean canNotifyUpdates() {
        return !myDisposed && !myPsiTreeUpdater.isSuspended() && !isSuspended();
    }

    private void onUpdaterCommitted() {
        if (canNotifyUpdates()) notifyUpdate();
    }

    private static boolean isSameVirtualFile(@Nullable PsiFile first, @Nullable PsiFile second) {
        if (first == second) return true;
        if (first == null || second == null) return false;
        return first.getVirtualFile() != null && first.getVirtualFile().equals(second.getVirtualFile());
    }

    @Override
    public PsiFile getPsiFile() {
        PsiFile psiFile = myPsiFileHolder.getPsiFile();
        if (psiFile != myObservedPsiFile) {
            updatePsiFile(psiFile);
        }
        return psiFile;
    }

    @Override
    public boolean isCommentRow(int rowIndex) {
        getRowCount();
        return myCachedCommentRows.contains(rowIndex);
    }

    @Override
    public void notifyUpdate() {
        invalidateModelState();
        resetPointer();
        getRowCount();
    }

    private void invalidateModelState() {
        myCachedRowCount = -1;
        myCachedColumnCount = -1;
        myCachedHasErrors = null;
        myCachedEscapeCharacter = null;
        myCachedCommentRows = Collections.emptySet();
        myPointedRecord = null;
        myPointedRow = 0;
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
            PsiFile psiFile = getPsiFile();
            if (psiFile == null) return null;

            int diffToCurrent = Math.abs(myPointedRow - row);
            if (diffToCurrent > row || myPointedRecord == null) {
                myPointedRecord = PsiHelper.getFirstChildOfType(psiFile, CsvRecord.class);
                myPointedRow = 0;
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
        getPsiFile();
        if (myCachedHasErrors == null) {
            myCachedHasErrors = ReadAction.compute(() -> {
                PsiFile psiFile = getPsiFile();
                return psiFile != null && PsiTreeUtil.hasErrorElements(psiFile);
            });
        }
        return myCachedHasErrors;
    }

    @Override
    public int getRowCount() {
        getPsiFile();
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
        getPsiFile();
        if (myCachedColumnCount == -1) {
            myCachedColumnCount = ReadAction.compute(() -> CsvTableModel.getColumnCount(getPsiFile()));
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
        ReadAction.run(() -> prepareSetValueAt(value, rowIndex, columnIndex));
        if (commitImmediately) getPsiTreeUpdater().commit();
    }

    private void prepareSetValueAt(String value, int rowIndex, int columnIndex) {
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
        ReadAction.run(() -> {
            CsvRecord row = PsiHelper.getNthChildOfType(getPsiFile(), anchorRowIndex, CsvRecord.class);
            if (row == null) return;
            getPsiTreeUpdater().addRow(row, before);
        });
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
        updater.addColumn(anchorColumnIndex, before);
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
        ReadAction.run(() -> {
            for (int currentColumn : columns) {
                for (int currentRow : rows) {
                    prepareSetValueAt("", currentRow, currentColumn);
                }
            }
        });
        getPsiTreeUpdater().commit();
    }
}
