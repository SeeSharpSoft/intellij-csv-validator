package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ArrayUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorState;
import net.seesharpsoft.intellij.plugins.csv.editor.table.api.TableActions;
import net.seesharpsoft.intellij.plugins.csv.editor.table.api.TableDataChangeEvent;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
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

    private static final int TOTAL_CELL_HEIGHT_SPACING = 3;
    private static final int TOTAL_CELL_WIDTH_SPACING = 8;

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
    private JComponent panelInfo;
    private JComboBox comboRowHeight;
    private JLabel lblTextlines;
    private JCheckBox cbFixedHeaders;
    private JCheckBox cbAutoColumnWidthOnOpen;
    protected LinkLabel lnkAdjustColumnWidth;
    private JScrollPane tableScrollPane;

    private JTable rowHeadersTable;

    private int baseFontHeight;

    protected final CsvTableEditorActionListeners tableEditorActions;
    protected final CsvTableEditorChangeListener tableEditorListener;
    protected final CsvTableEditorMouseListener tableEditorMouseListener;
    protected final CsvTableEditorKeyListener tableEditorKeyListener;
    protected final CsvTableEditorMouseWheelListener tableEditorMouseWheelListener;

    private boolean listenerApplied = false;

    private CsvColumnInfoMap lastColumnInfoMap;

    public CsvTableEditorSwing(@NotNull Project projectArg, @NotNull VirtualFile fileArg) {
        super(projectArg, fileArg);

        this.getDataHandler().addDataChangeListener(this);

        this.tableEditorListener = new CsvTableEditorChangeListener(this);
        this.tableEditorMouseListener = new CsvTableEditorMouseListener(this);
        this.tableEditorKeyListener = new CsvTableEditorKeyListener(this);
        this.tableEditorActions = new CsvTableEditorActionListeners(this);
        this.tableEditorMouseWheelListener = new CsvTableEditorMouseWheelListener(this);

        initializedUIComponents();
    }

    protected void createUIComponents() {
        tblEditor = new JBTable(new DefaultTableModel(0, 0));
        lnkTextEditor = new LinkLabel("Open file in text editor", null);
    }

    private void initializedUIComponents() {
        EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();

        btnRedo.addActionListener(tableEditorActions.redo);
        btnUndo.addActionListener(tableEditorActions.undo);
        btnAddRow.addActionListener(tableEditorActions.addRow);
        btnRemoveRow.addActionListener(tableEditorActions.deleteRow);
        btnAddColumn.addActionListener(tableEditorActions.addColumn);
        btnRemoveColumn.addActionListener(tableEditorActions.deleteColumn);
        lnkAdjustColumnWidth.setListener(this.tableEditorActions.adjustColumnWidthLink, null);
        lnkTextEditor.setListener(this.tableEditorActions.openTextEditor, null);
        lnkPlugin.setListener(this.tableEditorActions.openCsvPluginLink, null);

        panelInfo.setVisible(CsvEditorSettings.getInstance().showTableEditorInfoPanel());
        btnCloseInfoPanel.addActionListener(e -> {
            panelInfo.setVisible(false);
            getFileEditorState().setShowInfoPanel(false);
        });

        comboRowHeight.addActionListener(e -> {
            getFileEditorState().setRowLines(comboRowHeight.getSelectedIndex());
            setTableRowHeight(getPreferredRowHeight());
            updateRowHeights(null);
        });

        cbFixedHeaders.addActionListener(e -> {
            Object[][] values = storeCurrentState();
            getFileEditorState().setFixedHeaders(cbFixedHeaders.isSelected());
            updateTableComponentData(values);
        });

        cbAutoColumnWidthOnOpen.addActionListener(e -> {
            getFileEditorState().setAutoColumnWidthOnOpen(cbAutoColumnWidthOnOpen.isSelected());
        });

        tblEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblEditor.setShowColumns(true);
        tblEditor.setFont(getFont());
        tblEditor.setBackground(editorColorsScheme.getDefaultBackground());
        tblEditor.setForeground(editorColorsScheme.getDefaultForeground());
        setTableRowHeight(0);

        tblEditor.getColumnModel().addColumnModelListener(tableEditorListener);

        MultiLineCellRenderer cellRenderer = new MultiLineCellRenderer(this.tableEditorKeyListener, this);
        MultiLineCellRenderer cellEditor = new MultiLineCellRenderer(this.tableEditorKeyListener, this);
        tblEditor.setDefaultRenderer(String.class, cellRenderer);
        tblEditor.setDefaultRenderer(Object.class, cellRenderer);
        tblEditor.setDefaultEditor(String.class, cellEditor);
        tblEditor.setDefaultEditor(Object.class, cellEditor);
        tblEditor.registerKeyboardAction(this.tableEditorActions.undo,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_FOCUSED);
        tblEditor.registerKeyboardAction(this.tableEditorActions.redo,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), JComponent.WHEN_FOCUSED);
        tblEditor.registerKeyboardAction(this.tableEditorActions.redo,
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_FOCUSED);

        setFontSize(getGlobalFontSize());
        baseFontHeight = getFontHeight();

        applyEditorState(getFileEditorState());

        rowHeadersTable = TableRowUtilities.addNumberColumn(tblEditor, 1);
    }

    protected void applyTableChangeListener() {
        if (!listenerApplied && isEditable()) {
            listenerApplied = true;
            tblEditor.getModel().addTableModelListener(tableEditorListener);
            tblEditor.addMouseListener(this.tableEditorMouseListener);
            tblEditor.getTableHeader().addMouseListener(this.tableEditorMouseListener);
            tblEditor.addKeyListener(this.tableEditorKeyListener);
            tblEditor.addMouseWheelListener(tableEditorMouseWheelListener);
        }
    }

    protected void removeTableChangeListener() {
        if (listenerApplied) {
            tblEditor.getModel().removeTableModelListener(tableEditorListener);
            tblEditor.removeMouseListener(this.tableEditorMouseListener);
            tblEditor.getTableHeader().removeMouseListener(this.tableEditorMouseListener);
            tblEditor.removeKeyListener(this.tableEditorKeyListener);
            tblEditor.removeMouseWheelListener(tableEditorMouseWheelListener);
            listenerApplied = false;
        }
    }

    @Override
    protected void applyEditorState(CsvTableEditorState editorState) {
        cbFixedHeaders.setSelected(editorState.getFixedHeaders());
        if (comboRowHeight.getEditor() != null) {
            comboRowHeight.setSelectedIndex(editorState.getRowLines());
        }
        cbAutoColumnWidthOnOpen.setSelected(editorState.getAutoColumnWidthOnOpen());
        setTableRowHeight(getPreferredRowHeight());
    }

    public void setTableRowHeight(int rowHeight) {
        this.getTable().setRowHeight(rowHeight == 0 ? getPreferredRowHeight() : rowHeight);
    }

    private Object[] generateColumnIdentifiers(Object[][] values, int columnCount) {
        if (getFileEditorState().getFixedHeaders()) {
            return values != null && values.length > 0 ? values[0] : new Object[columnCount];
        }

        int columnOffset = CsvEditorSettings.getInstance().isZeroBasedColumnNumbering() ? 0 : 1;
        Object[] identifiers = new Object[columnCount];
        for (int i = 0; i < columnCount; ++i) {
            identifiers[i] = i + columnOffset;
        }
        return identifiers;
    }

    @Override
    protected void updateEditorLayout() {
        int currentColumnCount = this.getTableModel().getColumnCount();
        int[] columnWidths = getFileEditorState().getColumnWidths();
        int prevColumnCount = columnWidths.length;
        if (prevColumnCount != currentColumnCount) {
            columnWidths = ArrayUtil.realloc(columnWidths, currentColumnCount);
            if (prevColumnCount < currentColumnCount) {
                Arrays.fill(columnWidths, prevColumnCount, currentColumnCount, CsvEditorSettings.getInstance().getTableDefaultColumnWidth());
            }
            getFileEditorState().setColumnWidths(columnWidths);
        }

        float zoomFactor = getZoomFactor();
        for (int i = 0; i < currentColumnCount; ++i) {
            TableColumn column = this.tblEditor.getColumnModel().getColumn(i);
            column.setPreferredWidth(Math.round(columnWidths[i] * zoomFactor));
            column.setWidth(Math.round(columnWidths[i] * zoomFactor));
        }

        this.updateRowHeights(null);
        panelInfo.setVisible(getFileEditorState().showInfoPanel());
    }

    private float getZoomFactor() {
        float fontHeight = getFontHeight();
        return fontHeight / baseFontHeight;
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
            int columnCount = table.getColumnCount();
            int actualFirst = Math.min(first, table.getRowCount());
            int actualLast = Math.min(last, table.getRowCount());
            boolean isCalculated = getFileEditorState().getRowLines() == 0;
            for (int row = actualFirst; row < actualLast; row++) {
                int rowHeight = getPreferredRowHeight();
                if (isCalculated) {
                    for (int column = 0; column < columnCount; column++) {
                        Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
                        rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
                    }
                }
                table.setRowHeight(row, rowHeight);
                rowHeadersTable.setRowHeight(row, rowHeight);
            }
        } finally {
            applyTableChangeListener();
        }
    }

    public void syncTableModelWithUI() {
        updateTableComponentData(this.storeCurrentState());
    }

    @Override
    protected boolean isInCellEditMode() {
        return getTable() != null && getTable().getCellEditor() != null;
    }

    @Override
    protected void beforeTableComponentUpdate() {
        removeTableChangeListener();
    }

    @Override
    protected void afterTableComponentUpdate(Object[][] values) {
        try {
            DefaultTableModel tableModel = this.getTableModel();
            tableModel.setColumnIdentifiers(generateColumnIdentifiers(values, tableModel.getColumnCount()));
            this.updateEditorLayout();
        } finally {
            this.applyTableChangeListener();
        }
    }

    @Override
    protected void setTableComponentData(Object[][] values) {
        DefaultTableModel tableModel = getTableModel();

        boolean fixedHeader = getFileEditorState().getFixedHeaders();
        int firstRow = fixedHeader ? 1 : 0;
        int rowCount = values.length - firstRow;
        int columnCount = values.length == 0 ? 0 : values[0].length;
        tableModel.setRowCount(rowCount);
        tableModel.setColumnCount(columnCount);

        for (int row = 0; row < rowCount; ++row) {
            for (int column = 0; column < columnCount; ++column) {
                tableModel.setValueAt(values[row + firstRow][column], row, column);
            }
        }
    }

    @Override
    protected void updateInteractionElements() {
        updateEditActionElements(isEditable());

        lblErrorText.setVisible(hasErrors());
        lblTextlines.setVisible(!hasErrors());
        comboRowHeight.setVisible(!hasErrors());
        cbFixedHeaders.setVisible(!hasErrors());
        lnkAdjustColumnWidth.setVisible(!hasErrors());
        cbAutoColumnWidthOnOpen.setVisible(!hasErrors());

        this.removeTableChangeListener();
        this.applyTableChangeListener();
    }

    private void updateEditActionElements(boolean isEditable) {
        tblEditor.setEnabled(isEditable);
        tblEditor.setDragEnabled(isEditable);
        tblEditor.getTableHeader().setReorderingAllowed(isEditable);
        btnUndo.setVisible(isEditable);
        btnRedo.setVisible(isEditable);
    }


    protected JBTable getTable() {
        return this.tblEditor;
    }

    protected JScrollPane getTableScrollPane() {
        return this.tableScrollPane;
    }

    protected DefaultTableModel getTableModel() {
        return (DefaultTableModel) tblEditor.getModel();
    }

    @Override
    protected void updateUIComponents() {
        CsvFile csvFile = getCsvFile();
        if (csvFile == null) {
            return;
        }

        CsvColumnInfoMap<PsiElement> columnInfoMap = csvFile.getColumnInfoMap();
        if (Objects.equals(lastColumnInfoMap, columnInfoMap)) {
            return;
        }

        lastColumnInfoMap = columnInfoMap;
        updateInteractionElements();
        DefaultTableModel tableModel = new DefaultTableModel(0, 0);
        if (!columnInfoMap.hasErrors()) {
            int startRow = getFileEditorState().getFixedHeaders() ? 1 : 0;
            for (int columnIndex = 0; columnIndex < columnInfoMap.getColumnInfos().size(); ++columnIndex) {
                CsvColumnInfo<PsiElement> columnInfo = columnInfoMap.getColumnInfo(columnIndex);
                List<PsiElement> elements = columnInfo.getElements();
                if (columnIndex == 0 && CsvEditorSettings.getInstance().isFileEndLineBreak() &&
                        lastColumnInfoMap.hasEmptyLastLine()) {
                    elements.remove(elements.size() - 1);
                }

                tableModel.addColumn(String.format("Column %s (%s entries)", columnIndex + 1, elements.size()),
                        elements.stream()
                                .skip(startRow)
                                .map(psiElement -> psiElement == null ? "" : CsvHelper.unquoteCsvValue(psiElement.getText(), currentEscapeCharacter))
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
    public TableActions getActions() {
        return this.tableEditorActions;
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
        int[] widths = getCurrentColumnsWidths();
        float zoomFactor = getZoomFactor();
        for (int i = 0; i < widths.length; i++) {
            widths[i] /= zoomFactor;
        }
        getFileEditorState().setColumnWidths(widths);
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

    private Object[] getFixedHeaderValues() {
        CsvColumnInfoMap columnInfoMap = getColumnInfoMap();
        Object[] headerValues = new Object[columnInfoMap.getColumnInfos().size()];
        if (!columnInfoMap.hasErrors()) {
            for (int i = 0; i < columnInfoMap.getColumnInfos().size(); ++i) {
                CsvColumnInfo<PsiElement> columnInfo = columnInfoMap.getColumnInfo(i);
                PsiElement psiElement = columnInfo.getHeaderElement();
                headerValues[i] = psiElement == null ? "" : CsvHelper.unquoteCsvValue(psiElement.getText(), currentEscapeCharacter);
            }
        }
        return headerValues;
    }

    protected Object[][] getTableComponentData(TableModel tableModel, boolean tableModelIsLeading) {
        boolean fixedHeader = getFileEditorState().getFixedHeaders();
        int rowCount = tableModel.getRowCount();
        int columnCount = tableModel.getColumnCount();
        Object[][] values;
        if (fixedHeader) {
            values = new Object[rowCount + 1][];
            values[0] = getFixedHeaderValues();
        } else {
            values = new Object[rowCount][];
        }
        for (int row = 0; row < rowCount; ++row) {
            int valuesRowIndex = row + (fixedHeader ? 1 : 0);
            int modelRow = tableModelIsLeading ? row : tblEditor.convertRowIndexToModel(row);
            values[valuesRowIndex] = new Object[columnCount];
            for (int column = 0; column < columnCount; ++column) {
                int modelColumn = tableModelIsLeading ? column : tblEditor.convertColumnIndexToModel(column);
                values[valuesRowIndex][column] = tableModel.getValueAt(modelRow, modelColumn);
            }
        }
        return values;
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

    private int getGlobalFontSize() {
        return EditorColorsManager.getInstance().getGlobalScheme().getEditorFontSize();
    }

    private int getFontHeight() {
        return getTable().getFontMetrics(getTable().getFont()).getHeight();
    }

    @Override
    protected int getStringWidth(String text) {
        if (text == null) {
            return TOTAL_CELL_WIDTH_SPACING;
        }
        JTable table = getTable();
        FontMetrics fontMetrics = getTable().getFontMetrics(table.getFont());
        return CsvHelper.getMaxTextLineLength(text, input ->
                (int) Math.ceil((float) (fontMetrics.stringWidth(input) + TOTAL_CELL_WIDTH_SPACING) / getZoomFactor())
        );
    }

    public void changeFontSize(int changeAmount) {
        if (changeAmount == 0) {
            return;
        }
        int oldSize = getTable().getFont().getSize();
        int newSize = oldSize + changeAmount;
        setFontSize(newSize);
        updateEditorLayout();
    }

    @Override
    public int getPreferredRowHeight() {
        if (getFileEditorState().getRowLines() == 0) {
            return getFontHeight() + TOTAL_CELL_HEIGHT_SPACING;
        }
        return getFileEditorState().getRowLines() * getFontHeight() + TOTAL_CELL_HEIGHT_SPACING;

    }

    private void setFontSize(int size) {
        Font font = getTable().getFont();
        if (font.getSize() != size) {
            Font newFont = font.deriveFont((float) size);
            getTable().setFont(newFont);
        }
    }
}
