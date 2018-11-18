package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ArrayUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorSettingsExternalizable;
import net.seesharpsoft.intellij.plugins.csv.editor.table.*;
import net.seesharpsoft.intellij.plugins.csv.editor.table.api.TableDataChangeEvent;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CsvTableEditorSwing extends CsvTableEditor implements TableDataChangeEvent.Listener {

    private JBTable tblEditor;
    private JPanel panelMain;
    private JButton btnUndo;
    private JButton btnRedo;
    protected JButton btnAddRow;
    private LinkLabel lnkTextEditor;
    private JLabel lblErrorText;
    protected JButton btnAddColumn;
    protected JButton btnRemoveRow;
    protected JButton btnRemoveColumn;
    protected JButton btnAddRowBefore;
    protected JButton btnAddColumnBefore;
    private LinkLabel lnkPlugin;
    private JButton btnCloseInfoPanel;
    private JPanel panelInfo;
    private JComboBox comboRowHeight;
    private JLabel lblTextlines;

    protected final CsvTableEditorActions tableEditorActions;
    protected final CsvTableEditorChangeListener tableEditorListener;
    protected final CsvTableEditorMouseListener tableEditorMouseListener;
    protected final CsvTableEditorKeyListener tableEditorKeyListener;


    private boolean listenerApplied = false;

    public CsvTableEditorSwing(@NotNull Project projectArg, @NotNull VirtualFile fileArg) {
        super(projectArg, fileArg);

        this.getDataHandler().addDataChangeListener(this);

        this.tableEditorListener = new CsvTableEditorChangeListener(this);
        this.tableEditorMouseListener = new CsvTableEditorMouseListener(this);
        this.tableEditorKeyListener = new CsvTableEditorKeyListener(this);
        this.tableEditorActions = new CsvTableEditorActions(this);

        initializedUIComponents();
    }

    protected void createUIComponents() {
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

        tblEditor.setDefaultRenderer(String.class, new MultiLineCellRenderer(this.tableEditorKeyListener));
        tblEditor.setDefaultRenderer(Object.class, new MultiLineCellRenderer(this.tableEditorKeyListener));
        tblEditor.setDefaultEditor(String.class, new MultiLineCellRenderer(this.tableEditorKeyListener));
        tblEditor.setDefaultEditor(Object.class, new MultiLineCellRenderer(this.tableEditorKeyListener));
        tblEditor.registerKeyboardAction(this.tableEditorActions.undo,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK), JComponent.WHEN_FOCUSED);
        tblEditor.registerKeyboardAction(this.tableEditorActions.redo,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK), JComponent.WHEN_FOCUSED);
        tblEditor.registerKeyboardAction(this.tableEditorActions.redo,
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK), JComponent.WHEN_FOCUSED);
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

    protected void applyRowLines(int rowLines) {
        comboRowHeight.setSelectedIndex(rowLines);
        setTableRowHeight(getPreferredRowHeight());
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

    public void updateEditorLayout() {
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
            int actualFirst = Math.min(first, table.getRowCount());
            int actualLast = Math.min(last, table.getRowCount());
            for (int row = actualFirst; row < actualLast; row++) {
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
        updateTableComponentData(this.storeCurrentState());
    }

    @Override
    protected void beforeTableComponentUpdate() {
        removeTableChangeListener();
    }

    @Override
    protected void afterTableComponentUpdate() {
        try {
            DefaultTableModel tableModel = this.getTableModel();
            tableModel.setColumnIdentifiers(generateColumnIdentifiers(tableModel));
            this.updateEditorLayout();
        } finally {
            this.applyTableChangeListener();
        }
    }

    @Override
    protected void setTableComponentData(Object[][] values) {
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
    }

    @Override
    protected void updateInteractionElements() {
        updateEditActionElements(isEditable());

        lblErrorText.setVisible(hasErrors());
        lblTextlines.setVisible(!hasErrors());
        comboRowHeight.setVisible(!hasErrors());

        this.removeTableChangeListener();
        this.applyTableChangeListener();
    }

    private void updateEditActionElements(boolean isEditable) {
        tblEditor.setEnabled(isEditable);
        tblEditor.setDragEnabled(isEditable);
        tblEditor.getTableHeader().setReorderingAllowed(isEditable);
        btnUndo.setVisible(isEditable);
        btnRedo.setVisible(isEditable);
        btnAddColumn.setVisible(isEditable);
        btnAddRow.setVisible(isEditable);
    }


    protected JBTable getTable() {
        return this.tblEditor;
    }

    protected DefaultTableModel getTableModel() {
        return (DefaultTableModel) tblEditor.getModel();
    }

    @Override
    protected void updateUIComponents() {
        CsvColumnInfoMap<PsiElement> newColumnInfoMap = csvFile.getMyColumnInfoMap();
        if (Objects.equals(columnInfoMap, newColumnInfoMap)) {
            return;
        }

        columnInfoMap = csvFile.getMyColumnInfoMap();
        updateInteractionElements();
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
        Object[][] values = getTableComponentData(tableModel, true);
        updateTableComponentData(dataManagement.addState(values));
    }

    public void selectColumn(int currentColumn, boolean append) {
        if (!append) {
            getTable().clearSelection();
        }
        getTable().addColumnSelectionInterval(currentColumn, currentColumn);
        int currentRowCount = getTable().getRowCount();
        if (currentRowCount > 0) {
            getTable().addRowSelectionInterval(0, currentRowCount - 1);
        }
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

    public void storeCurrentTableLayout() {
        getFileEditorState().setColumnWidths(getCurrentColumnsWidths());
    }

    protected Object[][] storeCurrentState() {
        return storeStateChange(false);
    }

    protected Object[][] storeStateChange(boolean tableModelIsLeading) {
        return super.storeStateChange(getTableComponentData(this.getTableModel(), tableModelIsLeading));
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

    protected Object[][] getTableComponentData(TableModel tableModel, boolean tableModelIsLeading) {
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

    @Override
    public void onTableDataChanged(TableDataChangeEvent event) {
        btnUndo.setEnabled(this.dataManagement.canGetLastState());
        btnRedo.setEnabled(this.dataManagement.canGetNextState());
    }

    @Override
    protected String generateCsv(Object[][] data) {
        return super.generateCsv(data);
    }
}
