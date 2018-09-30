package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.table.JBTable;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CsvTableEditor implements FileEditor, FileEditorLocation {

    public static final String CHANGE_EVENT_TABLE_UPDATE = "tableUpdated";

    private JBTable tblEditor;
    private JPanel panelMain;
    private JButton btnUndo;
    private JButton btnRedo;
    private JButton btnAddRow;
    private LinkLabel lnkTextEditor;
    private JLabel lblErrorText;
    private JButton btnAddColumn;
    private JButton btnRemoveRow;
    private JButton btnRemoveColumn;
    private JButton btnAddRowBefore;
    private JButton btnAddColumnBefore;

    protected final Project project;
    protected final VirtualFile file;
    protected final UserDataHolder userDataHolder;
    protected final Document document;
    protected final PropertyChangeSupport changeSupport;
    protected final CsvFile csvFile;
    protected final String currentSeparator;
    protected final CsvTableEditorStatesHolder stateManagement;
    protected final CsvTableEditorActions tableEditorActions;
    protected final CsvTableEditorChangeListener tableEditorListener;
    protected final CsvTableEditorMouseListener tableEditorMouseListener;

    protected CsvColumnInfoMap<PsiElement> columnInfoMap;

    private JBPopupMenu rowPopupMenu;
    private JBPopupMenu columnPopupMenu;
    private boolean listenerApplied = false;

    protected boolean requiresEditorUpdate = false;

    private Object[][] initialState = null;

    public CsvTableEditor(@NotNull Project projectArg, @NotNull VirtualFile fileArg) {
        this.project = projectArg;
        this.file = fileArg;
        this.userDataHolder = new UserDataHolderBase();
        this.document = FileDocumentManager.getInstance().getDocument(this.file);
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        this.csvFile = (CsvFile) documentManager.getPsiFile(this.document);
        this.changeSupport = new PropertyChangeSupport(this);
        this.currentSeparator = CsvCodeStyleSettings.getCurrentSeparator(this.project, this.csvFile.getLanguage());
        this.stateManagement = new CsvTableEditorStatesHolder(this, CsvTableEditorStatesHolder.MAX_SIZE);

        this.tableEditorListener = new CsvTableEditorChangeListener(this);
        this.tableEditorMouseListener = new CsvTableEditorMouseListener(this);

        this.tableEditorActions = new CsvTableEditorActions(this);
        btnRedo.addActionListener(tableEditorActions.redo);
        btnUndo.addActionListener(tableEditorActions.undo);
        btnAddRow.addActionListener(tableEditorActions.addRow);
        btnRemoveRow.addActionListener(tableEditorActions.deleteRow);
        btnAddColumn.addActionListener(tableEditorActions.addColumn);
        btnRemoveColumn.addActionListener(tableEditorActions.deleteColumn);
        lnkTextEditor.setListener(this.tableEditorActions.openTextEditor, null);
    }

    protected void applyTableChangeListener() {
        if (!listenerApplied && !columnInfoMap.hasErrors()) {
            tblEditor.getColumnModel().addColumnModelListener(tableEditorListener);
            tblEditor.getModel().addTableModelListener(tableEditorListener);
            tblEditor.addMouseListener(this.tableEditorMouseListener);
            tblEditor.getTableHeader().addMouseListener(this.tableEditorMouseListener);
            listenerApplied = true;
        }
    }

    protected void removeTableChangeListener() {
        if (listenerApplied) {
            tblEditor.getColumnModel().removeColumnModelListener(tableEditorListener);
            tblEditor.getModel().removeTableModelListener(tableEditorListener);
            tblEditor.removeMouseListener(this.tableEditorMouseListener);
            tblEditor.getTableHeader().removeMouseListener(this.tableEditorMouseListener);
            listenerApplied = false;
        }
    }

    protected void fireDataUpdatedEvent() {
        this.changeSupport.firePropertyChange(CHANGE_EVENT_TABLE_UPDATE, false, true);
    }

    public void syncTableModelWithUI() {
        syncTableModelWithUI(false);
    }

    public void syncTableModelWithUI(boolean forceUpdate) {
        if (forceUpdate || this.requiresEditorUpdate) {
            try {
                updateEditorTable(this.storeCurrentState());
            } finally {
                fireDataUpdatedEvent();
                this.requiresEditorUpdate = false;
            }
        }
    }

    public void updateEditorTable(Object[][] values) {
        DefaultTableModel tableModel = getTableModel();
        removeTableChangeListener();

        int rowCount = values.length;
        int columnCount = values.length > 0 ? values[0].length : 0;
        tableModel.setRowCount(rowCount);
        tableModel.setColumnCount(columnCount);

        for (int row = 0; row < rowCount; ++row) {
            for (int column = 0; column < columnCount; ++column) {
                tableModel.setValueAt(values[row][column], row, column);
            }
        }

        tblEditor.createDefaultColumnsFromModel();
        applyTableChangeListener();
    }

    public boolean hasErrors() {
        return columnInfoMap.hasErrors();
    }

    protected void applyErrorUI(boolean hasErrors) {
        lblErrorText.setVisible(hasErrors);
        tblEditor.setEnabled(!hasErrors);
        tblEditor.setDragEnabled(!hasErrors);
        btnUndo.setVisible(!hasErrors);
        btnRedo.setVisible(!hasErrors);
        btnAddColumn.setVisible(!hasErrors);
        btnAddRow.setVisible(!hasErrors);
    }

    protected void updateUIComponents() {
        CsvColumnInfoMap<PsiElement> newColumnInfoMap = csvFile.getMyColumnInfoMap();
        if (Objects.equals(columnInfoMap, newColumnInfoMap)) {
            // nothing to update
            return;
        }

        columnInfoMap = csvFile.getMyColumnInfoMap();
        applyErrorUI(columnInfoMap.hasErrors());
        DefaultTableModel tableModel = new DefaultTableModel(0, 0);
        if (!columnInfoMap.hasErrors()) {
            for (int i = 0; i < columnInfoMap.getColumnInfos().size(); ++i) {
                CsvColumnInfo<PsiElement> columnInfo = columnInfoMap.getColumnInfo(i);
                List<PsiElement> elements = columnInfo.getElements();

                tableModel.addColumn(String.format("Column %s (%s entries)", i + 1, elements.size()),
                        elements.stream().map(psiElement -> psiElement == null ? "" : CsvHelper.unquoteCsvValue(psiElement.getText())).collect(Collectors.toList()).toArray(new String[0]));
            }
        }
        Object[][] state = getTableData(tableModel, true);
        updateEditorTable(stateManagement.addState(state));
    }

    private void createUIComponents() {
        tblEditor = new JBTable(new DefaultTableModel(0, 0));
        tblEditor.setShowColumns(true);

        lnkTextEditor = new LinkLabel("Open file in text editor", null);
    }

    protected JBTable getTable() {
        return this.tblEditor;
    }

    protected DefaultTableModel getTableModel() {
        return (DefaultTableModel) tblEditor.getModel();
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return this.panelMain;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.tblEditor;
    }

    @NotNull
    @Override
    public String getName() {
        return "Table Editor";
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {

    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public boolean isValid() {
        return this.csvFile.isValid();
    }

    @Override
    public void selectNotify() {
        updateUIComponents();
        this.initialState = stateManagement.getCurrentState();
    }

    @Override
    public void deselectNotify() {
        if (!this.stateManagement.equalsCurrentState(initialState)) {
            saveChanges(generateCsv(this.stateManagement.getCurrentState()));
        }
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


    public void saveChanges(final String content) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!this.document.isWritable() && ReadonlyStatusHandler.getInstance(this.project).ensureFilesWritable(this.file).hasReadonlyFiles()) {
                return;
            }

            ApplicationManager.getApplication()
                    .runWriteAction(() -> CommandProcessor.getInstance().executeCommand(this.project, () -> {
                        this.document.setText(content);
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

    protected Object[][] storeCurrentState() {
        return storeStateChange(false);
    }

    protected Object[][] storeStateChange(boolean tableModelIsLeading) {
        return this.stateManagement.addState(getTableData(this.getTableModel(), tableModelIsLeading));
    }

    protected Object[][] getCurrentTableData(boolean tableModelIsLeading) {
        return getTableData(this.getTableModel(), tableModelIsLeading);
    }

    protected Object[][] getTableData(TableModel tableModel, boolean tableModelIsLeading) {
        int rowCount = tableModel.getRowCount();
        int columnCount = tableModel.getColumnCount();
        Object[][] result = new Object[rowCount][];
        for (int row = 0; row < rowCount; ++row) {
            int modelRow = tableModelIsLeading ? row : tblEditor.convertRowIndexToModel(row);
            result[row] = new Object[columnCount];
            for (int column = 0; column < columnCount; ++column) {
                int modelColumn = tableModelIsLeading ? column : tblEditor.convertColumnIndexToModel(column);
                result[row][column] = tableModel.getValueAt(modelRow, modelColumn);
            }
        }
        return result;
    }

    public void requiresEditorUpdate() {
        this.requiresEditorUpdate = true;
    }

    protected JBPopupMenu getRowPopupMenu() {
        if (rowPopupMenu == null) {
            rowPopupMenu = new JBPopupMenu();
            JMenuItem menuItem = new JMenuItem("New row before (Strg+Shift+Enter)", btnAddRowBefore.getIcon());
            menuItem.addActionListener(tableEditorActions.addRowBefore);
            rowPopupMenu.add(menuItem);
            menuItem = new JMenuItem("New row after (Strg+Enter)", btnAddRow.getIcon());
            menuItem.addActionListener(tableEditorActions.addRowAfter);
            rowPopupMenu.add(menuItem);
            menuItem = new JMenuItem("Delete selected row(s)", btnRemoveRow.getIcon());
            menuItem.addActionListener(tableEditorActions.deleteRow);
            rowPopupMenu.add(menuItem);
        }
        return rowPopupMenu;
    }

    protected JBPopupMenu getColumnPopupMenu() {
        if (columnPopupMenu == null) {
            columnPopupMenu = new JBPopupMenu();
            JMenuItem menuItem = new JMenuItem("New column before (Strg+Shift+Space)", btnAddColumnBefore.getIcon());
            menuItem.addActionListener(tableEditorActions.addColumnBefore);
            columnPopupMenu.add(menuItem);
            menuItem = new JMenuItem("New column after (Strg+Space)", btnAddColumn.getIcon());
            menuItem.addActionListener(tableEditorActions.addColumnAfter);
            columnPopupMenu.add(menuItem);
            menuItem = new JMenuItem("Delete selected column", btnRemoveColumn.getIcon());
            menuItem.addActionListener(tableEditorActions.deleteColumn);
            columnPopupMenu.add(menuItem);
        }
        return columnPopupMenu;
    }

    public void updateUndoRedoButtonsEnabled() {
        btnUndo.setEnabled(this.stateManagement.canGetLastState());
        btnRedo.setEnabled(this.stateManagement.canGetNextState());
    }
}
