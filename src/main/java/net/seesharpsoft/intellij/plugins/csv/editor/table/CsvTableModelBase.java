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
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CsvTableModelBase implements CsvTableModel {
    private CsvTableEditor myEditor = null;

    private int myCachedRowCount = -1;
    private int myCachedColumnCount = -1;
    private Boolean myCachedHasErrors = null;

    private int myPointerRow = -1;
    private CsvField myPointerElement = null;

    private final CsvPsiTreeUpdater myPsiTreeUpdater;

    public CsvTableModelBase(@NotNull CsvTableEditor editor) {
        myEditor = editor;
        myPsiTreeUpdater = new CsvPsiTreeUpdater(editor);
        myPsiTreeUpdater.addCommitListener(() -> getEditor().updateUIComponents());
        editor.getCsvFile().getManager().addPsiTreeChangeListener(new PsiTreeAnyChangeAbstractAdapter() {
            @Override
            protected void onChange(@Nullable PsiFile file) {
                if (file == myPsiTreeUpdater.getPsiFile() && getEditor().isEditorSelected() && !myPsiTreeUpdater.isCommitting()) {
                    getEditor().updateUIComponents();
//                    notifyUpdate();
                }
            }
        }, myEditor);
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

    private CsvField resetPointer() {
        myPointerElement = PsiTreeUtil.findChildOfType(this.getEditor().getCsvFile(), CsvField.class);
        myPointerRow = 0;
        return myPointerElement;
    }

    protected CsvPsiTreeUpdater getPsiTreeUpdater() { return myPsiTreeUpdater; }

    @Override
    public PsiElement getFieldAt(int row, int column) {
        int diffToCurrent = Math.abs(myPointerRow - row);
        if (diffToCurrent > row) {
            resetPointer();
            diffToCurrent = row;
        }

        CsvRecord record = PsiHelper.getNthSiblingOfType(myPointerElement.getParent(), diffToCurrent, CsvRecord.class, myPointerRow > row);
        if (record == null) return null;

        if (PsiHelper.getElementType(record.getFirstChild()) == CsvTypes.COMMENT) return record.getFirstChild();

        CsvField field = PsiHelper.getNthChildOfType(record, column, CsvField.class);
        if (field == null) return null;

        myPointerElement = field;
        myPointerRow = row;
        return myPointerElement;
    }

    @Override
    public CsvTableEditor getEditor() {
        return myEditor;
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

    private String sanitizeFieldValue(Object value) {
        return sanitizeFieldValue(value, CsvEditorSettings.getInstance().isQuotingEnforced());
    }

    private String sanitizeFieldValue(Object value, boolean enforceQuoting) {
        if (value == null) {
            return "";
        }
        return CsvHelper.quoteCsvField(value.toString(), this.getEscapeCharacter(), this.getValueSeparator(), enforceQuoting);
    }

    @Override
    public void setValueAt(String value, int rowIndex, int columnIndex) {
        setValueAt(value, rowIndex, columnIndex, true);
    }

    private void createMissingColumns(int rowIndex, int columnIndex) {
        int currentColumnCount = getColumnCount(rowIndex);
        PsiElement field = getFieldAt(rowIndex, currentColumnCount - 1);
        getPsiTreeUpdater().addEmptyColumns(field, columnIndex - currentColumnCount + 1);
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
            updater.addColumn(field, sanitizeFieldValue(value), true);
            updater.addEmptyColumns(field, columnIndex - currentColumnCount);
        } else {
            String sanitizedValue = value;
            if (!CsvHelper.isCommentElement(field)) {
                sanitizedValue = sanitizeFieldValue(value);
            }

            updater.replaceField(field, sanitizedValue, true);
        }
        updater.commit();
    }

    @Override
    public String getValueAt(int rowIndex, int columnIndex) {
        PsiElement field = getFieldAt(rowIndex, columnIndex);
        String value = field == null ? "" : field.getText();
        return CsvHelper.unquoteCsvValue(value, getEscapeCharacter());
    }

    @Override
    public void addRow(int focusedRowIndex, boolean before) {
        CsvRecord row = PsiHelper.getNthChildOfType(getEditor().getCsvFile(), focusedRowIndex, CsvRecord.class);
        getPsiTreeUpdater().addRow(row, before);
        getPsiTreeUpdater().commit();
    }

    @Override
    public void removeRows(int[] indices) {
        CsvPsiTreeUpdater psiTreeUpdater = getPsiTreeUpdater();
        for (int rowIndex : indices) {
            CsvRecord row = PsiHelper.getNthChildOfType(getEditor().getCsvFile(), rowIndex, CsvRecord.class);
            boolean removePreviousLF = rowIndex > 0;
            PsiElement lfElement = PsiHelper.getSiblingOfType(row, CsvTypes.CRLF, removePreviousLF);
            psiTreeUpdater.delete(row);
            if (lfElement != null) {
                psiTreeUpdater.delete(lfElement);
            }
        }
        psiTreeUpdater.commit();
    }

    @Override
    public void addColumn(int focusedColumnIndex, boolean before) {
        CsvPsiTreeUpdater updater = getPsiTreeUpdater();
        // +1 for the one to add
        int targetColumnCount = getColumnCount() + 1;
        int rowIndex = 0;
        for (PsiElement record = getEditor().getCsvFile().getFirstChild(); record != null; record = record.getNextSibling()) {
            if (!CsvRecord.class.isInstance(record)) continue;
            if (CsvHelper.isCommentElement(record.getFirstChild())) continue;
            PsiElement focusedCol = PsiHelper.getNthChildOfType(record, focusedColumnIndex, CsvField.class);
            if (focusedCol == null) {
                createMissingColumns(rowIndex, targetColumnCount);
            } else {
                updater.addColumn(focusedCol, before);
            }
            ++rowIndex;
        }
        updater.commit();
    }

    @Override
    public void removeColumns(int[] indices) {
        for (int columnIndex : indices) {
            removeColumn(columnIndex);
        }
        getPsiTreeUpdater().commit();
    }

    private void removeColumn(int columnIndex) {
        CsvPsiTreeUpdater updater = getPsiTreeUpdater();
        if (getColumnCount() == 1) {
            updater.deleteContent();
            return;
        }
        for (PsiElement record = getEditor().getCsvFile().getFirstChild(); record != null; record = record.getNextSibling()) {
            if (!CsvRecord.class.isInstance(record)) continue;
            if (CsvHelper.isCommentElement(record.getFirstChild())) continue;
            PsiElement focusedCol = PsiHelper.getNthChildOfType(record, columnIndex, CsvField.class);
            // if no field exists in row, we are done
            if (focusedCol != null) {
                boolean removePreviousSeparator = columnIndex > 0;
                PsiElement valueSeparator = PsiHelper.getSiblingOfType(focusedCol,CsvTypes.COMMA, removePreviousSeparator);
                updater.delete(focusedCol);
                if (valueSeparator != null) {
                    updater.delete(valueSeparator);
                }
            }
        }
    }

    @Override
    public void clearCells(int[] rows, int[] columns) {
        for (int currentColumn : columns) {
            for (int currentRow : rows) {
                setValueAt("", currentRow, currentColumn, false);
            }
        }
        getPsiTreeUpdater().commit();
    }
}
