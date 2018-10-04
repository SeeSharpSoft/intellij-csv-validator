package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.*;
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
import com.intellij.util.ArrayUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorSettingsExternalizable;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CsvTableEditor implements FileEditor, FileEditorLocation {

    public static final int ROW_LINE_HEIGHT = 20;
    public static final int INITIAL_COLUMN_WIDTH = 100;

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
    private LinkLabel lnkPlugin;
    private JButton btnCloseInfoPanel;
    private JPanel panelInfo;
    private JComboBox comboRowHeight;

    protected final Project project;
    protected final VirtualFile file;
    protected final UserDataHolder userDataHolder;
    protected final Document document;
    protected final PropertyChangeSupport changeSupport;
    protected final CsvFile csvFile;
    protected final String currentSeparator;
    protected final CsvTableEditorDataHolder dataManagement;
    protected final CsvTableEditorActions tableEditorActions;
    protected final CsvTableEditorChangeListener tableEditorListener;
    protected final CsvTableEditorMouseListener tableEditorMouseListener;
    protected final CsvTableEditorKeyListener tableEditorKeyListener;

    protected CsvColumnInfoMap<PsiElement> columnInfoMap;

    private JBPopupMenu rowPopupMenu;
    private JBPopupMenu columnPopupMenu;
    private boolean listenerApplied = false;

    protected boolean tableIsEditable = true;

    private Object[][] initialState = null;
    private CsvTableEditorState storedState = null;

    public CsvTableEditor(@NotNull Project projectArg, @NotNull VirtualFile fileArg) {
        this.project = projectArg;
        this.file = fileArg;
        this.userDataHolder = new UserDataHolderBase();
        this.document = FileDocumentManager.getInstance().getDocument(this.file);
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        this.csvFile = (CsvFile) documentManager.getPsiFile(this.document);
        this.changeSupport = new PropertyChangeSupport(this);
        this.currentSeparator = CsvCodeStyleSettings.getCurrentSeparator(this.project, this.csvFile.getLanguage());
        this.dataManagement = new CsvTableEditorDataHolder(this, CsvTableEditorDataHolder.MAX_SIZE);

        this.tableEditorListener = new CsvTableEditorChangeListener(this);
        this.tableEditorMouseListener = new CsvTableEditorMouseListener(this);
        this.tableEditorKeyListener = new CsvTableEditorKeyListener(this);
        this.tableEditorActions = new CsvTableEditorActions(this);

        initializedUIComponents();
    }

    private void createUIComponents() {
        tblEditor = new JBTable(new DefaultTableModel(0, 0));
        lnkTextEditor = new LinkLabel("Open file in text editor", null);
    }

    private void initializedUIComponents() {
        btnRedo.addActionListener(tableEditorActions.redo);
        btnUndo.addActionListener(tableEditorActions.undo);
        btnAddRow.addActionListener(tableEditorActions.addRow);
        btnRemoveRow.addActionListener(tableEditorActions.deleteRow);
        btnAddColumn.addActionListener(tableEditorActions.addColumn);
        btnRemoveColumn.addActionListener(tableEditorActions.deleteColumn);
        lnkTextEditor.setListener(this.tableEditorActions.openTextEditor, null);
        lnkPlugin.setListener(this.tableEditorActions.openCsvPluginLink, null);

        panelInfo.setVisible(CsvEditorSettingsExternalizable.getInstance().showTableEditorInfoPanel());
        btnCloseInfoPanel.addActionListener(e -> {
            panelInfo.setVisible(false);
            getFileEditorState().setShowInfoPanel(false);
        });

        comboRowHeight.addActionListener(e -> {
            getFileEditorState().setRowLines(comboRowHeight.getSelectedIndex());
            setTableRowHeight(getPreferredRowHeight());
            updateRowHeights(null);
        });

        tblEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblEditor.setShowColumns(true);
        setTableRowHeight(0);

        tblEditor.getColumnModel().addColumnModelListener(tableEditorListener);

        tblEditor.setDefaultRenderer(String.class, new MultiLineCellRenderer());
        tblEditor.setDefaultRenderer(Object.class, new MultiLineCellRenderer());
        tblEditor.setDefaultEditor(String.class, new MultiLineCellRenderer());
        tblEditor.setDefaultEditor(Object.class, new MultiLineCellRenderer());
        tblEditor.registerKeyboardAction(this.tableEditorActions.undo, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK), JComponent.WHEN_FOCUSED);
        tblEditor.registerKeyboardAction(this.tableEditorActions.redo, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK), JComponent.WHEN_FOCUSED);
        tblEditor.registerKeyboardAction(this.tableEditorActions.redo, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK), JComponent.WHEN_FOCUSED);
    }


    protected void applyTableChangeListener() {
        if (!listenerApplied && isEditable()) {
            listenerApplied = true;
            tblEditor.getModel().addTableModelListener(tableEditorListener);
            tblEditor.addMouseListener(this.tableEditorMouseListener);
            tblEditor.getTableHeader().addMouseListener(this.tableEditorMouseListener);
            tblEditor.addKeyListener(this.tableEditorKeyListener);
        }
    }

    protected void removeTableChangeListener() {
        if (listenerApplied) {
            tblEditor.getModel().removeTableModelListener(tableEditorListener);
            tblEditor.removeMouseListener(this.tableEditorMouseListener);
            tblEditor.getTableHeader().removeMouseListener(this.tableEditorMouseListener);
            tblEditor.removeKeyListener(this.tableEditorKeyListener);
            listenerApplied = false;
        }
    }

    public void setEditable(boolean editable) {
        this.tableIsEditable = editable;
        this.updateReadOnlyUI();
    }

    public boolean isEditable() {
        return this.tableIsEditable && !this.hasErrors();
    }

    public int getPreferredRowHeight() {
        return ROW_LINE_HEIGHT * getFileEditorState().getRowLines();
    }

    public void setTableRowHeight(int rowHeight) {
        this.getTable().setRowHeight(rowHeight == 0 ? ROW_LINE_HEIGHT : rowHeight);
    }

    private Object[] generateColumnIdentifiers(TableModel tableModel) {
        int columnCount = tableModel.getColumnCount();
        Object[] identifiers = new Object[columnCount];
        for (int i = 0; i < columnCount; ++i) {
            identifiers[i] = tableModel.getColumnName(i);
        }
        return identifiers;
    }

    protected void onEditorDataUpdated() {
        this.removeTableChangeListener();
        try {
            DefaultTableModel tableModel = this.getTableModel();
            tableModel.setColumnIdentifiers(generateColumnIdentifiers(tableModel));
            this.updateTableLayout();
        } finally {
            this.applyTableChangeListener();
        }
    }

    public void updateTableLayout() {
        int currentColumnCount = this.getTableModel().getColumnCount();
        int[] columnWidths = getFileEditorState().getColumnWidths();
        int prevColumnCount = columnWidths.length;
        if (prevColumnCount != currentColumnCount) {
            columnWidths = ArrayUtil.realloc(columnWidths, currentColumnCount);
            if (prevColumnCount < currentColumnCount) {
                Arrays.fill(columnWidths, prevColumnCount, currentColumnCount, INITIAL_COLUMN_WIDTH);
            }
            getFileEditorState().setColumnWidths(columnWidths);
        }

        for (int i = 0; i < currentColumnCount; ++i) {
            TableColumn column = this.tblEditor.getColumnModel().getColumn(i);
            column.setPreferredWidth(columnWidths[i]);
            column.setWidth(columnWidths[i]);
        }

        this.updateRowHeights(null);
        panelInfo.setVisible(getFileEditorState().showInfoPanel());
    }

    public void updateRowHeights(TableModelEvent e) {
        final int first;
        final int last;
        if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
            first = 0;
            last = this.getTable().getRowCount();
        } else {
            first = e.getFirstRow();
            last = e.getLastRow() + 1;
        }

        SwingUtilities.invokeLater(() -> {
            updateRowHeights(first, last);
        });
    }

    private void updateRowHeights(int first, int last) {
        removeTableChangeListener();
        try {
            JTable table = getTable();
            first = Math.min(first, table.getRowCount());
            last = Math.min(last, table.getRowCount());
            for (int row = first; row < last; row++) {
                int rowHeight = getPreferredRowHeight();
                if (rowHeight == 0) {
                    for (int column = 0; column < table.getColumnCount(); column++) {
                        Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
                        rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
                    }
                }
                if (rowHeight != table.getRowHeight(row)) {
                    table.setRowHeight(row, rowHeight);
                }
            }
        } finally {
            applyTableChangeListener();
        }
    }

    public void syncTableModelWithUI() {
        updateTableData(this.storeCurrentState());
    }

    public void updateTableData(Object[][] values) {
        removeTableChangeListener();

        try {
            DefaultTableModel tableModel = getTableModel();

            int rowCount = values.length;
            int columnCount = values.length == 0 ? 0 : values[0].length;
            tableModel.setRowCount(rowCount);
            tableModel.setColumnCount(columnCount);

            for (int row = 0; row < rowCount; ++row) {
                for (int column = 0; column < columnCount; ++column) {
                    tableModel.setValueAt(values[row][column], row, column);
                }
            }
        } finally {
            onEditorDataUpdated();
            applyTableChangeListener();
        }

    }

    public boolean hasErrors() {
        return columnInfoMap.hasErrors();
    }

    public void updateReadOnlyUI() {
        updateReadOnlyUI(!isEditable());
    }

    protected void updateReadOnlyUI(boolean isReadOnly) {
        lblErrorText.setVisible(hasErrors());
        tblEditor.setEnabled(!isReadOnly);
        tblEditor.setDragEnabled(!isReadOnly);
        tblEditor.getTableHeader().setReorderingAllowed(!isReadOnly);
        btnUndo.setVisible(!isReadOnly);
        btnRedo.setVisible(!isReadOnly);
        btnAddColumn.setVisible(!isReadOnly);
        btnAddRow.setVisible(!isReadOnly);

        this.removeTableChangeListener();
        this.applyTableChangeListener();
    }

    protected void updateUIComponents() {
        CsvColumnInfoMap<PsiElement> newColumnInfoMap = csvFile.getMyColumnInfoMap();
        if (Objects.equals(columnInfoMap, newColumnInfoMap)) {
            return;
        }

        columnInfoMap = csvFile.getMyColumnInfoMap();
        updateReadOnlyUI();
        DefaultTableModel tableModel = new DefaultTableModel(0, 0);
        if (!columnInfoMap.hasErrors()) {
            for (int i = 0; i < columnInfoMap.getColumnInfos().size(); ++i) {
                CsvColumnInfo<PsiElement> columnInfo = columnInfoMap.getColumnInfo(i);
                List<PsiElement> elements = columnInfo.getElements();

                tableModel.addColumn(String.format("Column %s (%s entries)", i + 1, elements.size()),
                        elements.stream()
                                .map(psiElement -> psiElement == null ? "" : CsvHelper.unquoteCsvValue(psiElement.getText()))
                                .collect(Collectors.toList()).toArray(new String[0]));
            }
        }
        Object[][] values = getTableData(tableModel, true);
        updateTableData(dataManagement.addState(values));
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
        return "Table View";
    }

    protected CsvTableEditorState getFileEditorState() {
        if (storedState == null) {
            storedState = new CsvTableEditorState();
        }
        return storedState;
    }

    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return getFileEditorState();
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
        CsvTableEditorState tableEditorState = (CsvTableEditorState) fileEditorState;
        this.storedState = tableEditorState;

        comboRowHeight.setSelectedIndex(storedState.getRowLines());
        setTableRowHeight(getPreferredRowHeight());
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
        this.initialState = dataManagement.getCurrentState();
    }

    @Override
    public void deselectNotify() {
        if (!this.dataManagement.equalsCurrentState(initialState)) {
            saveChanges(generateCsv(this.dataManagement.getCurrentState()));
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
        if (hasErrors()) {
            return;
        }
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

    public void storeCurrentTableLayout() {
        getFileEditorState().setColumnWidths(getCurrentColumnsWidths());
    }

    protected Object[][] storeCurrentState() {
        return storeStateChange(false);
    }

    protected Object[][] storeStateChange(boolean tableModelIsLeading) {
        return this.dataManagement.addState(getTableData(this.getTableModel(), tableModelIsLeading));
    }

    protected int[] getCurrentColumnsWidths() {
        TableColumnModel columnModel = tblEditor.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        int[] width = new int[columnCount];
        for (int i = 0; i < columnCount; ++i) {
            width[i] = columnModel.getColumn(i).getWidth();
        }
        return width;
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

    protected JBPopupMenu getRowPopupMenu() {
        if (rowPopupMenu == null) {
            rowPopupMenu = new JBPopupMenu();
            JMenuItem menuItem = new JMenuItem("New row before (Ctrl+Shift+Enter)", btnAddRowBefore.getIcon());
            menuItem.addActionListener(tableEditorActions.addRowBefore);
            rowPopupMenu.add(menuItem);
            menuItem = new JMenuItem("New row after (Ctrl+Enter)", btnAddRow.getIcon());
            menuItem.addActionListener(tableEditorActions.addRowAfter);
            rowPopupMenu.add(menuItem);
            menuItem = new JMenuItem("Delete selected row(s) (Ctrl+Del)", btnRemoveRow.getIcon());
            menuItem.addActionListener(tableEditorActions.deleteRow);
            rowPopupMenu.add(menuItem);
        }
        return rowPopupMenu;
    }

    protected JBPopupMenu getColumnPopupMenu() {
        if (columnPopupMenu == null) {
            columnPopupMenu = new JBPopupMenu();
            JMenuItem menuItem = new JMenuItem("New column before (Alt+Shift+Enter)", btnAddColumnBefore.getIcon());
            menuItem.addActionListener(tableEditorActions.addColumnBefore);
            columnPopupMenu.add(menuItem);
            menuItem = new JMenuItem("New column after (Alt+Enter)", btnAddColumn.getIcon());
            menuItem.addActionListener(tableEditorActions.addColumnAfter);
            columnPopupMenu.add(menuItem);
            menuItem = new JMenuItem("Delete selected column (Alt+Del)", btnRemoveColumn.getIcon());
            menuItem.addActionListener(tableEditorActions.deleteColumn);
            columnPopupMenu.add(menuItem);
        }
        return columnPopupMenu;
    }

    public void updateUndoRedoButtonsEnabled() {
        btnUndo.setEnabled(this.dataManagement.canGetLastState());
        btnRedo.setEnabled(this.dataManagement.canGetNextState());
    }
}
