package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TraceableDisposable;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.Collection;

public abstract class CsvTableEditor implements FileEditor, PsiFileHolder {

    public static final String EDITOR_NAME = "Table Editor";

    private boolean myDisposed = false;

    protected final Project project;
    protected final VirtualFile file;
    protected final UserDataHolder userDataHolder;
    protected final PropertyChangeSupport changeSupport;

    protected Document document;
    protected PsiFile psiFile;
    protected CsvValueSeparator currentSeparator;
    protected CsvEscapeCharacter currentEscapeCharacter;

    private CsvTableEditorState storedState = null;

    protected boolean tableIsEditable = true;

    public CsvTableEditor(@NotNull Project projectArg, @NotNull VirtualFile fileArg) {
        this.project = projectArg;
        this.file = fileArg;
        this.userDataHolder = new UserDataHolderBase();
        this.changeSupport = new PropertyChangeSupport(this);
    }

    @NotNull
    public abstract CsvTableActions getActions();

    protected abstract boolean isInCellEditMode();

    protected abstract void updateInteractionElements();

    protected abstract void applyEditorState(CsvTableEditorState editorState);

    public abstract void updateEditorLayout();

    public abstract void storeCurrentTableLayout();

    public abstract CsvTableModel getTableModel();

    public abstract void beforeTableModelUpdate();

    public abstract void afterTableModelUpdate();

    public void setEditable(boolean editable) {
        this.tableIsEditable = editable;
        this.updateInteractionElements();
    }

    public boolean isEditable() {
        return this.tableIsEditable && file.isWritable();
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

    public <T extends CsvTableEditorState> T getTableEditorState() {
        if (storedState == null) {
            storedState = new CsvTableEditorState();
        }
        return (T) storedState;
    }

    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return getTableEditorState();
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
        CsvTableEditorState tableEditorState = fileEditorState instanceof CsvTableEditorState ? (CsvTableEditorState) fileEditorState : new CsvTableEditorState();
        this.storedState = tableEditorState;
        applyEditorState(getTableEditorState());
    }

    @Override
    public boolean isModified() {
//        return this.dataManagement != null && initialState != null && !this.dataManagement.equalsCurrentState(initialState);
        return false;
    }

    @Override
    public boolean isValid() {
        if (this.isDisposed() || file == null || !file.isValid()) {
            return false;
        }
        CsvFile csvFile = this.getCsvFile();
        return csvFile != null && csvFile.isValid();
    }

    @Override
    public void selectNotify() {
        getTableModel().resume();
    }

    @Override
    public void deselectNotify() {
        // auto save on change - nothing to do here
        getTableModel().suspend();
    }

    public boolean isEditorSelected() {
        return !this.isDisposed() &&
                (ApplicationManager.getApplication().isUnitTestMode() || FileEditorManager.getInstance(this.project).getSelectedEditor(this.getFile()) == this);
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

    @Override
    public void dispose() {
        if (this.isDisposed()) return;

        this.deselectNotify();
        this.myDisposed = true;
        getTableModel().dispose();
    }

    public boolean isDisposed() {
        return this.myDisposed;
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

    public Document getDocument() {
        CsvFile csvFile = getCsvFile();
        if (csvFile == null) return null;
        return this.document;
    }

    public PsiFile getPsiFile() {
        return getCsvFile();
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

    public CsvValueSeparator getValueSeparator() {
        return this.currentSeparator;
    }

    public CsvEscapeCharacter getEscapeCharacter() {
        return this.currentEscapeCharacter;
    }

    public Font getEditorFont() {
        return UIUtil.getFontWithFallback(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN));
    }

    protected int getStringWidth(String text) {
        int fontSize = getEditorFont().getSize();
        return CsvHelper.getMaxTextLineLength(text, input -> fontSize * input.length());
    }

    public final void resetAllColumnWidths() {
        int[] widths = new int[getTableModel().getColumnCount()];
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
        getTableEditorState().setColumnWidths(widths);
    }

    abstract protected int[] getMaxTextWidthForAllColumns();

    protected int[] calculateDistributedColumnWidths() {
        int[] maxTextWidths = getMaxTextWidthForAllColumns();
        int[] widths = new int[maxTextWidths.length];
        int tableAutoMaxColumnWidth = CsvEditorSettings.getInstance().getTableAutoMaxColumnWidth();
        int colIndex = 0;

        for (int maxTextWidth : maxTextWidths) {
            if (tableAutoMaxColumnWidth > 0) {
                maxTextWidth = Math.min(tableAutoMaxColumnWidth, maxTextWidth);
            }
            widths[colIndex] = maxTextWidth == 0 ? CsvEditorSettings.getInstance().getTableDefaultColumnWidth() : maxTextWidth;
            ++colIndex;
        }

        return widths;
    }

    public final void addRow(int focusedRowIndex, boolean before) {
        this.getTableModel().addRow(focusedRowIndex, before);
    }

    public final void removeRows(Collection<Integer> indices) {
        this.getTableModel().removeRows(indices);
    }

    public final void addColumn(int focusedColumnIndex, boolean before) {
        this.getTableModel().addColumn(focusedColumnIndex, before);
    }

    public final void removeColumns(Collection<Integer> indices) {
        this.getTableModel().removeColumns(indices);
    }

    public final void clearCells(Collection<Integer> rows, Collection<Integer> columns) {
        this.getTableModel().clearCells(rows, columns);
    }
}
