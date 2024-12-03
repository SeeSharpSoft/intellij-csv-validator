package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.labels.LinkLabel;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableActions;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorState;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;

public class CsvTableEditorSwing extends CsvTableEditor {

    private static final int COLUMN_WIDTH_MAX_TEXT_SAMPLE_SIZE = 10;
    private static final int TOTAL_CELL_WIDTH_SPACING = 16;

    private JTable tblEditor;
    private JPanel panelMain;
    private LinkLabel<Object> lnkTextEditor;
    private JLabel lblErrorText;
    private JScrollPane tableScrollPane;
    private JPanel panelTop;
    private JTable rowHeadersTable;

    private int baseFontHeight;

    protected final CsvTableEditorActionListeners tableEditorActions;
    protected final CsvTableEditorChangeListener tableEditorListener;
    protected final CsvTableEditorMouseListener tableEditorMouseListener;
    protected final CsvTableEditorKeyListener tableEditorKeyListener;
    protected final CsvTableEditorMouseWheelListener tableEditorMouseWheelListener;
    private boolean listenerApplied = false;

    // temporary stored values when table data is updated
    private int mySelectedColumn;
    private int mySelectedRow;
    private boolean myIsInCellEditMode;

    public CsvTableEditorSwing(@NotNull Project projectArg, @NotNull VirtualFile fileArg) {
        super(projectArg, fileArg);

        this.tableEditorListener = new CsvTableEditorChangeListener(this);
        this.tableEditorMouseListener = new CsvTableEditorMouseListener(this);
        this.tableEditorKeyListener = new CsvTableEditorKeyListener(this);
        this.tableEditorActions = new CsvTableEditorActionListeners(this);
        this.tableEditorMouseWheelListener = new CsvTableEditorMouseWheelListener(this);

        initializedUIComponents();
    }

    protected void createUIComponents() {
        tblEditor = new CsvTable(new CsvTableModelSwing(this));
        tblEditor.setRowSorter(null);
        lnkTextEditor = new LinkLabel<>("Open.file.in.text.editor", null);
    }

    private void initializedUIComponents() {
        lnkTextEditor.setListener(this.tableEditorActions.openTextEditor, null);
        
        EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();

        tblEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblEditor.setFont(getEditorFont());
        tblEditor.setBackground(editorColorsScheme.getDefaultBackground());
        tblEditor.setForeground(editorColorsScheme.getDefaultForeground());
        tblEditor.getColumnModel().addColumnModelListener(tableEditorListener);

        CsvMultiLineCellRenderer cellRenderer = new CsvMultiLineCellRenderer(this.tableEditorKeyListener, this);
        CsvMultiLineCellRenderer cellEditor = new CsvMultiLineCellRenderer(this.tableEditorKeyListener, this);
        tblEditor.setDefaultRenderer(String.class, cellRenderer);
        tblEditor.setDefaultRenderer(Object.class, cellRenderer);
        tblEditor.setDefaultRenderer(CsvTable.CommentColumn.class, new CsvMultiLineCellRenderer.Comment(this.tableEditorKeyListener, this));
        tblEditor.setDefaultEditor(String.class, cellEditor);
        tblEditor.setDefaultEditor(Object.class, cellEditor);
        tblEditor.setDefaultEditor(CsvTable.CommentColumn.class, new CsvMultiLineCellRenderer.Comment(this.tableEditorKeyListener, this));
        tblEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblEditor.setRowHeight(1);

        setFontSize(getGlobalFontSize());
        baseFontHeight = getFontHeight();

        rowHeadersTable = CsvTableRowUtilities.addNumberColumn(this, tblEditor, 1);

        applyEditorState(getTableEditorState());
    }

    protected void applyTableChangeListener() {
        if (!listenerApplied && isEditorSelected() && isEditable()) {
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
        tblEditor.setRowHeight(editorState.getRowHeight());
        if (rowHeadersTable != null) {
            rowHeadersTable.setRowHeight(tblEditor.getRowHeight());
        }
    }

    @Override
    public void updateEditorLayout() {
        setEditable(!getTableModel().hasErrors());

        int currentColumnCount = this.getTableModel().getColumnCount();
        int[] columnWidths = getTableEditorState().getColumnWidths();
        int prevColumnCount = columnWidths.length;
        if (prevColumnCount != currentColumnCount) {
            adjustAllColumnWidths();
            columnWidths = getTableEditorState().getColumnWidths();
        }

        float zoomFactor = getZoomFactor();
        for (int i = 0; i < currentColumnCount; ++i) {
            TableColumn column = this.tblEditor.getColumnModel().getColumn(i);
            column.setPreferredWidth(Math.round(columnWidths[i] * zoomFactor));
            column.setWidth(Math.round(columnWidths[i] * zoomFactor));
        }

        storeCurrentTableLayout();
    }

    private float getZoomFactor() {
        float fontHeight = getFontHeight();
        return fontHeight / baseFontHeight;
    }

    @Override
    protected boolean isInCellEditMode() {
        return getTable() != null && getTable().getCellEditor() != null;
    }

    @Override
    public void beforeTableModelUpdate() {
        if (tblEditor == null) return;

        mySelectedColumn = tblEditor.getSelectedColumn();
        mySelectedRow = tblEditor.getSelectedRow();
        myIsInCellEditMode = tblEditor.isEditing();
    }

    @Override
    public void afterTableModelUpdate() {
        if (tblEditor == null) return;

        removeTableChangeListener();
        try {
            this.tblEditor.tableChanged(new TableModelEvent(tblEditor.getModel(), TableModelEvent.ALL_COLUMNS));
            this.updateEditorLayout();
            int selectedRow = Math.min(tblEditor.getRowCount(), mySelectedRow);
            int selectedCol = Math.min(tblEditor.getColumnCount(), mySelectedColumn);
            tblEditor.changeSelection(selectedRow, selectedCol, false, false);
            if (myIsInCellEditMode) {
                tblEditor.editCellAt(selectedRow, selectedCol);
            }
        } finally {
            applyTableChangeListener();
        }
    }

    @Override
    protected void updateInteractionElements() {
        panelTop.setVisible(getTableModel().hasErrors());
        updateEditActionElements(isEditable());
    }

    private void updateEditActionElements(boolean isEditable) {
        tblEditor.setEnabled(isEditable);
        // TODO support later
//        tblEditor.setDragEnabled(isEditable);
        tblEditor.setDragEnabled(false);
//        tblEditor.getTableHeader().setReorderingAllowed(isEditable);
        tblEditor.getTableHeader().setReorderingAllowed(false);
    }

    protected JTable getTable() {
        return this.tblEditor;
    }

    protected JScrollPane getTableScrollPane() {
        return this.tableScrollPane;
    }

    @Override
    public CsvTableModel getTableModel() {
        return (CsvTableModel) tblEditor.getModel();
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
    public CsvTableActions<CsvTableEditorSwing> getActions() {
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

    @Override
    public void storeCurrentTableLayout() {
        int[] widths = getCurrentColumnsWidths();
        float zoomFactor = getZoomFactor();
        for (int i = 0; i < widths.length; i++) {
            widths[i] /= zoomFactor;
        }
        getTableEditorState().setColumnWidths(widths);
        getTableEditorState().setRowHeight(tblEditor.getRowHeight());
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

    private int getGlobalFontSize() {
        return EditorColorsManager.getInstance().getGlobalScheme().getEditorFontSize();
    }

    private int getFontHeight() {
        return getTable().getFontMetrics(getTable().getFont()).getHeight();
    }

    protected int[] getMaxTextWidthForAllColumns() {
        CsvTableModel tableModel = getTableModel();
        int maxRow = Math.min(tableModel.getRowCount(), COLUMN_WIDTH_MAX_TEXT_SAMPLE_SIZE);
        int[] textLengths = new int[tableModel.getColumnCount()];

        for (int row = 0; row < maxRow; ++row) {
            if (!tableModel.isCommentRow(row)) {
                for (int col = 0; col < textLengths.length; ++col) {
                    String currentText = tableModel.getValue(row, col);
                    textLengths[col] = Math.max(getStringWidth(currentText), textLengths[col]);
                }
            }
        }

        return textLengths;
    }

    @Override
    protected int getStringWidth(String text) {
        if (text == null || text.length() == 0) {
            return 0;
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

    private void setFontSize(int size) {
        Font font = getTable().getFont();
        if (font.getSize() != size) {
            Font newFont = font.deriveFont((float) size);
            getTable().setFont(newFont);
        }
    }
}
