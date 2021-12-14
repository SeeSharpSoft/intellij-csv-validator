package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.google.common.primitives.Ints;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.*;
import net.seesharpsoft.intellij.plugins.csv.editor.table.api.TableActions;
import net.seesharpsoft.intellij.plugins.csv.editor.table.api.TableDataHandler;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class CsvTableEditor implements FileEditor, FileEditorLocation {

    public static final String EDITOR_NAME = "Table Editor";

    protected final Project project;
    protected final VirtualFile file;
    protected final UserDataHolder userDataHolder;
    protected final PropertyChangeSupport changeSupport;
    protected final TableDataHandler dataManagement;

    protected Document document;
    protected PsiFile psiFile;
    protected CsvValueSeparator currentSeparator;
    protected CsvEscapeCharacter currentEscapeCharacter;

    private Object[][] initialState = null;
    private CsvTableEditorState storedState = null;

    protected boolean tableIsEditable = true;

    public CsvTableEditor(@NotNull Project projectArg, @NotNull VirtualFile fileArg) {
        this.project = projectArg;
        this.file = fileArg;
        this.userDataHolder = new UserDataHolderBase();
        this.changeSupport = new PropertyChangeSupport(this);
        this.dataManagement = new TableDataHandler(this, TableDataHandler.MAX_SIZE);
    }

    @NotNull
    public abstract TableActions getActions();

    protected abstract boolean isInCellEditMode();

    protected abstract void updateUIComponents();

    protected abstract void updateInteractionElements();

    protected abstract void applyEditorState(CsvTableEditorState editorState);

    protected abstract void setTableComponentData(Object[][] values);

    protected abstract void beforeTableComponentUpdate();

    protected abstract void afterTableComponentUpdate(Object[][] values);

    public abstract int getPreferredRowHeight();

    protected abstract void updateEditorLayout();

    public final void updateTableComponentData(Object[][] values) {
        beforeTableComponentUpdate();
        try {
            setTableComponentData(values);
            saveChanges();
        } finally {
            afterTableComponentUpdate(values);
        }
    }

    public void setEditable(boolean editable) {
        this.tableIsEditable = editable;
        this.updateInteractionElements();
    }

    public boolean isEditable() {
        return this.tableIsEditable && !this.hasErrors() && !hasComments() && file.isWritable();
    }

    public CsvColumnInfoMap<PsiElement> getColumnInfoMap() {
        CsvFile csvFile = getCsvFile();
        return csvFile == null ? null : csvFile.getColumnInfoMap();
    }

    public boolean hasErrors() {
        if (!isValid()) {
            return true;
        }
        CsvColumnInfoMap columnInfoMap = getColumnInfoMap();
        return (columnInfoMap != null && columnInfoMap.hasErrors());
    }

    public boolean hasComments() {
        if (!isValid()) {
            return false;
        }
        CsvColumnInfoMap columnInfoMap = getColumnInfoMap();
        return (columnInfoMap != null && columnInfoMap.hasComments());
    }

    protected Object[][] storeStateChange(Object[][] data) {
        Object[][] result = this.dataManagement.addState(data);
        saveChanges();
        return result;
    }

    public void saveChanges() {
        if (isModified() && !ApplicationManager.getApplication().isUnitTestMode()) {
            saveChanges(generateCsv(this.dataManagement.getCurrentState()));
        }
    }

    public void saveChanges(final String content) {
        if (hasErrors()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project == null || project.isDisposed() ||
                    (!this.document.isWritable() && ReadonlyStatusHandler.getInstance(this.project).ensureFilesWritable(Collections.singleton(this.file)).hasReadonlyFiles())) {
                return;
            }
            ApplicationManager.getApplication().runWriteAction(() ->
                    CommandProcessor.getInstance().executeCommand(this.project, () -> {
                        this.document.setText(content);
                        this.initialState = dataManagement.getCurrentState();
                    }, "Csv Table Editor changes", null));
        });
    }

    protected String sanitizeFieldValue(Object value) {
        if (value == null) {
            return "";
        }
        return CsvHelper.quoteCsvField(value.toString(), this.currentEscapeCharacter, this.currentSeparator, CsvEditorSettings.getInstance().isQuotingEnforced());
    }

    protected String generateCsv(Object[][] data) {
        CsvColumnInfoMap columnInfoMap = getColumnInfoMap();
        boolean newLineAtEnd = CsvEditorSettings.getInstance().isFileEndLineBreak() && (columnInfoMap == null || columnInfoMap.hasEmptyLastLine());
        StringBuilder result = new StringBuilder();
        for (int row = 0; row < data.length; ++row) {
            for (int column = 0; column < data[row].length; ++column) {
                Object value = data[row][column];
                result.append(sanitizeFieldValue(value));
                if (column < data[row].length - 1) {
                    result.append(this.currentSeparator.getCharacter());
                }
            }
            if (row < data.length - 1 || newLineAtEnd) {
                result.append("\n");
            }
        }
        return result.toString();
    }

    @NotNull
    @Override
    public abstract JComponent getComponent();

    @Nullable
    @Override
    public abstract JComponent getPreferredFocusedComponent();

    @NotNull
    @Override
    public String getName() {
        return EDITOR_NAME;
    }

    protected <T extends CsvTableEditorState> T getFileEditorState() {
        if (storedState == null) {
            storedState = new CsvTableEditorState();
        }
        return (T) storedState;
    }

    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return getFileEditorState();
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
        CsvTableEditorState tableEditorState = fileEditorState instanceof CsvTableEditorState ? (CsvTableEditorState) fileEditorState : new CsvTableEditorState();
        this.storedState = tableEditorState;

        applyEditorState(getFileEditorState());
    }

    @Override
    public boolean isModified() {
        return this.dataManagement != null && initialState != null && !this.dataManagement.equalsCurrentState(initialState);
    }

    @Override
    public boolean isValid() {
        if (file == null || !file.isValid()) {
            return false;
        }
        CsvFile csvFile = this.getCsvFile();
        return csvFile != null && csvFile.isValid();
    }

    @Override
    public void selectNotify() {
        this.initialState = null;
        updateUIComponents();
        this.initialState = dataManagement.getCurrentState();

        if (getFileEditorState().getAutoColumnWidthOnOpen()) {
            adjustAllColumnWidths();
        }
    }

    @Override
    public void deselectNotify() {
        // auto save on change - nothing to do here
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
        this.changeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
        this.changeSupport.removePropertyChangeListener(propertyChangeListener);
    }

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return this;
    }

    @Override
    public void dispose() {
        this.deselectNotify();
    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return userDataHolder.getUserData(key);
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {
        userDataHolder.putUserData(key, t);
    }

    @NotNull
    @Override
    public FileEditor getEditor() {
        return this;
    }

    @Override
    public int compareTo(@NotNull FileEditorLocation o) {
        return 1;
    }

    @Nullable
    public StructureViewBuilder getStructureViewBuilder() {
        return isValid() ? StructureViewBuilder.PROVIDER.getStructureViewBuilder(file.getFileType(), file, this.project) : null;
    }

    @Nullable
    public VirtualFile getFile() {
        return this.file;
    }

    @Nullable
    public Project getProject() {
        return this.project;
    }

    @Nullable
    public final CsvFile getCsvFile() {
        if (project == null || project.isDisposed()) {
            return null;
        }
        if (this.psiFile == null || !this.psiFile.isValid()) {
            this.document = FileDocumentManager.getInstance().getDocument(this.file);
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            this.psiFile = documentManager.getPsiFile(this.document);
            this.currentSeparator = CsvHelper.getValueSeparator(this.psiFile);
            this.currentEscapeCharacter = CsvHelper.getEscapeCharacter(this.psiFile);
        }
        return this.psiFile instanceof CsvFile ? (CsvFile) psiFile : null;
    }

    public final TableDataHandler getDataHandler() {
        return this.dataManagement;
    }

    public final int getRowCount() {
        return getDataHandler().getCurrentState().length;
    }

    public Font getFont() {
        return UIUtil.getFontWithFallback(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN));
    }

    protected int getStringWidth(String text) {
        int fontSize = getFont().getSize();
        return CsvHelper.getMaxTextLineLength(text, input -> fontSize * input.length());
    }

    public final void resetAllColumnWidths() {
        int[] widths = new int[getColumnCount()];
        Arrays.fill(widths, CsvEditorSettings.getInstance().getTableDefaultColumnWidth());
        setAllColumnWidths(widths);
    }

    public final void adjustAllColumnWidths() {
        setAllColumnWidths(calculateDistributedColumnWidths());
    }

    protected final void setAllColumnWidths(int[] widths) {
        if (widths == null) {
            return;
        }
        getFileEditorState().setColumnWidths(widths);
        updateEditorLayout();
    }

    protected int[] calculateDistributedColumnWidths() {
        CsvColumnInfoMap csvColumnInfoMap = this.getColumnInfoMap();
        if (csvColumnInfoMap == null || csvColumnInfoMap.hasErrors()) {
            return null;
        }
        Map<Integer, CsvColumnInfo<PsiElement>> columnInfos = csvColumnInfoMap.getColumnInfos();
        Object[][] data = getDataHandler().getCurrentState();
        int[] widths = new int[columnInfos.size()];
        int tableAutoMaxColumnWidth = CsvEditorSettings.getInstance().getTableAutoMaxColumnWidth();

        for (Map.Entry<Integer, CsvColumnInfo<PsiElement>> columnInfoEntry : columnInfos.entrySet()) {
            CsvColumnInfo<PsiElement> columnInfo = columnInfoEntry.getValue();
            int currentWidth = getStringWidth(data[columnInfo.getMaxLengthRowIndex()][columnInfo.getColumnIndex()].toString());
            if (tableAutoMaxColumnWidth != 0) {
                currentWidth = Math.min(tableAutoMaxColumnWidth, currentWidth);
            }
            widths[columnInfoEntry.getKey()] = currentWidth;
        }

        return widths;
    }

    public final int getColumnCount() {
        Object[][] currentData = getDataHandler().getCurrentState();
        return currentData.length > 0 ? currentData[0].length : 0;
    }

    public final Object[][] addRow(int focusedRowIndex, boolean before) {
        int index = (before ? (focusedRowIndex == -1 ? 0 : focusedRowIndex) : (focusedRowIndex == -1 ? getRowCount() : focusedRowIndex + 1)) +
                (getFileEditorState().getFixedHeaders() ? 1 : 0);
        TableDataHandler dataHandler = getDataHandler();
        Object[][] currentData = dataHandler.getCurrentState();
        Object[][] newData = ArrayUtil.insert(currentData, Math.min(index, currentData.length), new Object[getColumnCount()]);
        updateTableComponentData(dataHandler.addState(newData));
        return newData;
    }

    public final Object[][] removeRows(int[] indices) {
        List<Integer> currentRows = Ints.asList(indices);
        currentRows.sort(Collections.reverseOrder());
        TableDataHandler dataHandler = getDataHandler();
        Object[][] currentData = dataHandler.getCurrentState();
        int offset = getFileEditorState().getFixedHeaders() ? 1 : 0;
        for (int currentRow : currentRows) {
            currentData = ArrayUtil.remove(currentData, currentRow + offset);
        }
        updateTableComponentData(dataHandler.addState(currentData));
        return currentData;
    }

    public final Object[][] addColumn(int focusedColumnIndex, boolean before) {
        int index = before ? (focusedColumnIndex == -1 ? 0 : focusedColumnIndex) : (focusedColumnIndex == -1 ? getColumnCount() : focusedColumnIndex + 1);
        boolean fixedHeaders = getFileEditorState().getFixedHeaders();
        TableDataHandler dataHandler = getDataHandler();
        Object[][] currentData = dataHandler.getCurrentState();
        for (int i = 0; i < currentData.length; ++i) {
            currentData[i] = ArrayUtil.insert(currentData[i], index, fixedHeaders && i == 0 ? "" : null);
        }
        updateTableComponentData(dataHandler.addState(currentData));
        return currentData;
    }

    public final Object[][] removeColumns(int[] indices) {
        List<Integer> currentColumns = Ints.asList(indices);
        currentColumns.sort(Collections.reverseOrder());
        TableDataHandler dataHandler = getDataHandler();
        Object[][] currentData = dataHandler.getCurrentState();
        for (int currentColumn : currentColumns) {
            for (int i = 0; i < currentData.length; ++i) {
                currentData[i] = ArrayUtil.remove(currentData[i], currentColumn);
            }
        }
        updateTableComponentData(dataHandler.addState(currentData));
        return currentData;
    }

    public final Object[][] clearCells(int[] columns, int[] rows) {
        TableDataHandler dataHandler = getDataHandler();
        Object[][] currentData = dataHandler.getCurrentState();
        int offset = getFileEditorState().getFixedHeaders() ? 1 : 0;
        for (int currentColumn : columns) {
            for (int currentRow : rows) {
                currentData[currentRow + offset][currentColumn] = "";
            }
        }
        updateTableComponentData(dataHandler.addState(currentData));
        return currentData;
    }
}
