package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
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
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.table.api.TableDataHandler;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public abstract class CsvTableEditor implements FileEditor, FileEditorLocation {

    public static final String EDITOR_NAME = "Table Editor";

    public static final int ROW_LINE_HEIGHT = 20;
    public static final int INITIAL_COLUMN_WIDTH = 100;

    protected final Project project;
    protected final VirtualFile file;
    protected final UserDataHolder userDataHolder;
    protected final Document document;
    protected final PropertyChangeSupport changeSupport;
    protected final PsiFile psiFile;
    protected final String currentSeparator;
    protected final TableDataHandler dataManagement;

    private Object[][] initialState = null;
    private CsvTableEditorState storedState = null;

    protected CsvColumnInfoMap<PsiElement> columnInfoMap;
    protected boolean tableIsEditable = true;

    public CsvTableEditor(@NotNull Project projectArg, @NotNull VirtualFile fileArg) {
        this.project = projectArg;
        this.file = fileArg;
        this.userDataHolder = new UserDataHolderBase();
        this.document = FileDocumentManager.getInstance().getDocument(this.file);
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        this.psiFile = documentManager.getPsiFile(this.document);
        this.changeSupport = new PropertyChangeSupport(this);
        this.currentSeparator = CsvCodeStyleSettings.getCurrentSeparator(this.project, this.psiFile.getLanguage());
        this.dataManagement = new TableDataHandler(this, TableDataHandler.MAX_SIZE);
    }

    protected abstract void updateUIComponents();

    protected abstract void updateInteractionElements();

    protected abstract void applyRowLines(int rowLines);

    protected abstract void setTableComponentData(Object[][] values);

    protected abstract void beforeTableComponentUpdate();

    protected abstract void afterTableComponentUpdate();

    public final void updateTableComponentData(Object[][] values) {
        beforeTableComponentUpdate();
        try {
            setTableComponentData(values);
            saveChanges();
        } finally {
            afterTableComponentUpdate();
        }
    }

    public int getPreferredRowHeight() {
        return ROW_LINE_HEIGHT * getFileEditorState().getRowLines();
    }

    public void setEditable(boolean editable) {
        this.tableIsEditable = editable;
        this.updateInteractionElements();
    }

    public boolean isEditable() {
        return this.tableIsEditable && !this.hasErrors();
    }

    public boolean hasErrors() {
        return !isValid() || (columnInfoMap != null && columnInfoMap.hasErrors());
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
            if (!this.document.isWritable() && ReadonlyStatusHandler.getInstance(this.project).ensureFilesWritable(this.file).hasReadonlyFiles()) {
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
        return CsvHelper.quoteCsvField(value.toString());
    }

    protected String generateCsv(Object[][] data) {
        StringBuilder result = new StringBuilder();
        for (int row = 0; row < data.length; ++row) {
            for (int column = 0; column < data[row].length; ++column) {
                Object value = data[row][column];
                result.append(sanitizeFieldValue(value));
                if (column < data[row].length - 1) {
                    result.append(this.currentSeparator);
                }
            }
            if (row < data.length - 1) {
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

        applyRowLines(getFileEditorState().getRowLines());
    }

    @Override
    public boolean isModified() {
        return this.dataManagement != null && initialState != null && !this.dataManagement.equalsCurrentState(initialState);
    }

    @Override
    public boolean isValid() {
        CsvFile csvFile = this.getCsvFile();
        return csvFile != null && csvFile.isValid();
    }

    @Override
    public void selectNotify() {
        updateUIComponents();
        this.initialState = dataManagement.getCurrentState();
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
        return file != null && file.isValid() ? StructureViewBuilder.PROVIDER.getStructureViewBuilder(file.getFileType(), file, this.project) : null;
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
    public CsvFile getCsvFile() {
        return this.psiFile instanceof CsvFile ? (CsvFile)psiFile : null;
    }

    public TableDataHandler getDataHandler() {
        return this.dataManagement;
    }
}
