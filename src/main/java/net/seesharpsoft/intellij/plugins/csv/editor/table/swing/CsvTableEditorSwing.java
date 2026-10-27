package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.util.DocumentUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.labels.LinkLabel;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvLexerFactory;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableReorder;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableActions;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorState;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModelBase.ModelState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class CsvTableEditorSwing extends CsvTableEditor {

    protected static final int COLUMN_WIDTH_MAX_TEXT_SAMPLE_SIZE = 10;
    private static final int TOTAL_CELL_WIDTH_SPACING = 16;

    private JTable tblEditor;
    private JPanel panelMain;
    private LinkLabel<Object> lnkTextEditor;
    private JLabel lblErrorText;
    private JScrollPane tableScrollPane;
    private JPanel panelTop;
    private JTable rowHeadersTable;
    private final JPanel editorComponent;
    private CsvTableSearchSession searchSession;

    private int baseFontHeight;

    protected final CsvTableEditorActionListeners tableEditorActions;
    protected final CsvTableEditorChangeListener tableEditorListener;
    protected final CsvTableEditorMouseListener tableEditorMouseListener;
    protected final CsvTableEditorKeyListener tableEditorKeyListener;
    protected final CsvTableEditorMouseWheelListener tableEditorMouseWheelListener;
    private boolean listenerApplied = false;
    private boolean committingColumnOrder;
    private Selection selectionAfterReorder;
    private volatile boolean reordering;
    private static final int BACKGROUND_REORDER_THRESHOLD = 128 * 1024;

    private record Selection(int[] rows, int[] columns) {
    }

    // temporary stored values when table data is updated
    private int[] mySelectedColumns = new int[0];
    private int[] mySelectedRows = new int[0];
    private boolean myIsInCellEditMode;

    public CsvTableEditorSwing(@NotNull Project projectArg, @NotNull VirtualFile fileArg) {
        super(projectArg, fileArg);

        this.tableEditorListener = new CsvTableEditorChangeListener(this);
        this.tableEditorMouseListener = new CsvTableEditorMouseListener(this);
        this.tableEditorKeyListener = new CsvTableEditorKeyListener(this);
        this.tableEditorActions = new CsvTableEditorActionListeners(this);
        this.tableEditorMouseWheelListener = new CsvTableEditorMouseWheelListener(this);

        initializedUIComponents();
        editorComponent = new JPanel(new BorderLayout());
        editorComponent.add(panelMain, BorderLayout.CENTER);
        registerSearchShortcut(IdeActions.ACTION_FIND, false);
        registerSearchShortcut(IdeActions.ACTION_REPLACE, true);
    }

    private void registerSearchShortcut(String actionId, boolean replace) {
        DumbAwareAction.create(event -> showSearch(replace)).registerCustomShortcutSet(
                ActionManager.getInstance().getAction(actionId).getShortcutSet(), editorComponent, this);
    }

    public void showSearch(boolean replace) {
        if (isReordering()) return;
        if (tblEditor.isEditing() && !tblEditor.getCellEditor().stopCellEditing()) return;
        if (searchSession == null) {
            searchSession = new CsvTableSearchSession(this);
            editorComponent.add(searchSession.getComponent(), BorderLayout.NORTH);
        }
        searchSession.show(replace);
        editorComponent.revalidate();
        editorComponent.repaint();
    }

    @Override
    public void dispose() {
        getTable().cancelRowDragging();
        if (searchSession != null) searchSession.dispose();
        super.dispose();
    }

    protected void createUIComponents() {
        tblEditor = createTable(new CsvTableModelSwing(this));
        tblEditor.setRowSorter(null);
        lnkTextEditor = new LinkLabel<>("Open.file.in.text.editor", null);
    }

    protected CsvTable createTable(CsvTableModelSwing model) {
        return new CsvTable(model);
    }

    protected CsvMultiLineCellRenderer createCellRenderer(boolean comment) {
        return comment ? new CsvMultiLineCellRenderer.Comment(this.tableEditorKeyListener, this)
                : new CsvMultiLineCellRenderer(this.tableEditorKeyListener, this);
    }

    private void initializedUIComponents() {
        lnkTextEditor.setListener(this.tableEditorActions.openTextEditor, null);

        EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();

        tblEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblEditor.setFont(getEditorFont());
        tblEditor.setBackground(editorColorsScheme.getDefaultBackground());
        tblEditor.setForeground(editorColorsScheme.getDefaultForeground());
        tblEditor.getColumnModel().addColumnModelListener(tableEditorListener);

        CsvMultiLineCellRenderer cellRenderer = createCellRenderer(false);
        CsvMultiLineCellRenderer cellEditor = createCellRenderer(false);
        tblEditor.setDefaultRenderer(String.class, cellRenderer);
        tblEditor.setDefaultRenderer(Object.class, cellRenderer);
        tblEditor.setDefaultRenderer(CsvTable.CommentColumn.class, createCellRenderer(true));
        tblEditor.setDefaultEditor(String.class, cellEditor);
        tblEditor.setDefaultEditor(Object.class, cellEditor);
        tblEditor.setDefaultEditor(CsvTable.CommentColumn.class, createCellRenderer(true));
        tblEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblEditor.setRowHeight(1);

        setFontSize(getGlobalFontSize());
        baseFontHeight = getFontHeight();

        rowHeadersTable = CsvTableRowUtilities.addNumberColumn(this, tblEditor, 1);
        CsvTableRowDragListener rowDragListener = new CsvTableRowDragListener(this, rowHeadersTable);
        rowHeadersTable.addMouseListener(rowDragListener);
        rowHeadersTable.addMouseMotionListener(rowDragListener);
        rowHeadersTable.addMouseListener(tableEditorMouseListener);
        ((CsvTableHeader) tblEditor.getTableHeader()).setColumnReorderingHandler(this::finishColumnReordering);
        tblEditor.getTableHeader().addMouseListener(tableEditorMouseListener);
        tableScrollPane.getViewport().addChangeListener(event -> {
            if (getFrozenColumnCount() > 0) getTable().updateFrozenColumnLayout();
        });

        applyEditorState(getTableEditorState());
    }

    protected void applyTableChangeListener() {
        if (!listenerApplied && isEditorSelected() && isEditable()) {
            listenerApplied = true;
            tblEditor.getModel().addTableModelListener(tableEditorListener);
            tblEditor.addMouseListener(this.tableEditorMouseListener);
            tblEditor.addKeyListener(this.tableEditorKeyListener);
            tblEditor.addMouseWheelListener(tableEditorMouseWheelListener);
        }
    }

    protected void removeTableChangeListener() {
        if (listenerApplied) {
            tblEditor.getModel().removeTableModelListener(tableEditorListener);
            tblEditor.removeMouseListener(this.tableEditorMouseListener);
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
        getTable().setFrozenColumnCount(editorState.getFrozenColumnCount());
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

        setFrozenColumnCount(Math.min(getTableEditorState().getFrozenColumnCount(), currentColumnCount));
        storeCurrentTableLayout();
    }

    public int getFrozenColumnCount() {
        return getTable().getFrozenColumnCount();
    }

    public void setFrozenColumnCount(int count) {
        getTable().setFrozenColumnCount(count);
        getTableEditorState().setFrozenColumnCount(getTable().getFrozenColumnCount());
    }

    public boolean canFreezeColumns() {
        return !isReordering() && tblEditor.getSelectedColumn() >= 0;
    }

    public void freezeColumnsThroughSelection() {
        int column = tblEditor.getColumnModel().getSelectionModel().getMaxSelectionIndex();
        if (column >= 0) setFrozenColumnCount(column + 1);
    }

    protected float getZoomFactor() {
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

        getTable().cancelRowDragging();
        ((CsvTableHeader) tblEditor.getTableHeader()).cancelColumnReordering();
        mySelectedColumns = selectionAfterReorder == null ? tblEditor.getSelectedColumns() : selectionAfterReorder.columns();
        mySelectedRows = selectionAfterReorder == null ? tblEditor.getSelectedRows() : selectionAfterReorder.rows();
        selectionAfterReorder = null;
        myIsInCellEditMode = tblEditor.isEditing();
    }

    @Override
    public void afterTableModelUpdate() {
        if (tblEditor == null) return;

        removeTableChangeListener();
        try {
            this.tblEditor.tableChanged(new TableModelEvent(tblEditor.getModel(), TableModelEvent.ALL_COLUMNS));
            this.updateEditorLayout();
            restoreSelection(mySelectedRows, mySelectedColumns);
            if (myIsInCellEditMode && tblEditor.getSelectedRow() >= 0 && tblEditor.getSelectedColumn() >= 0) {
                tblEditor.editCellAt(tblEditor.getSelectedRow(), tblEditor.getSelectedColumn());
            }
        } finally {
            applyTableChangeListener();
        }
        if (searchSession != null) searchSession.tableChanged();
    }

    @Override
    protected void updateInteractionElements() {
        panelTop.setVisible(getTableModel().hasErrors());
        updateEditActionElements(isEditable());
    }

    private void updateEditActionElements(boolean isEditable) {
        tblEditor.setEnabled(isEditable);
        tblEditor.setDragEnabled(false);
        tblEditor.getTableHeader().setReorderingAllowed(isEditable);
        tblEditor.getTableHeader().setResizingAllowed(!reordering);
        rowHeadersTable.setEnabled(!reordering);
    }

    @Override
    public boolean isEditable() {
        return !reordering && super.isEditable();
    }

    public boolean isReordering() {
        return reordering;
    }

    public boolean canMoveSelection(boolean columns, boolean backwards) {
        if (!isEditable() || getTableModel().hasErrors()) return false;
        ListSelectionModel selected = columns ? tblEditor.getColumnModel().getSelectionModel() : tblEditor.getSelectionModel();
        int first = selected.getMinSelectionIndex();
        int last = selected.getMaxSelectionIndex();
        int count = columns ? tblEditor.getColumnCount() : getTableModel().getMovableRowCount();
        return first >= 0 && last < count && (backwards ? first > 0 : last < count - 1);
    }

    public void moveSelection(boolean columns, boolean backwards) {
        if (!canMoveSelection(columns, backwards) || !stopCellEditing()) return;
        int[] rows = tblEditor.getSelectedRows();
        int[] cols = tblEditor.getSelectedColumns();
        int[] selected = columns ? cols : rows;
        int[] order = IntStream.range(0, columns ? tblEditor.getColumnCount() : tblEditor.getRowCount()).toArray();
        int offset = backwards ? -1 : 1;
        for (int i = 0; i < selected.length; i++) {
            int index = selected[backwards ? i : selected.length - 1 - i];
            int adjacent = order[index + offset];
            order[index + offset] = order[index];
            order[index] = adjacent;
        }
        int[] moved = Arrays.stream(selected).map(index -> index + offset).toArray();
        applyOrder(columns, order, columns ? rows : moved, columns ? moved : cols);
    }

    void moveRow(int from, int to) {
        int count = getTableModel().getMovableRowCount();
        if (!isEditable() || getTableModel().hasErrors() || from < 0 || to < 0 ||
                from >= count || to >= count || from == to || !stopCellEditing()) return;
        int[] order = IntStream.range(0, tblEditor.getRowCount()).toArray();
        int step = from < to ? 1 : -1;
        for (int index = from; index != to; index += step) order[index] = order[index + step];
        order[to] = from;
        int[] columns = IntStream.range(0, tblEditor.getColumnCount()).toArray();
        getTable().scrollCellToVisible(to, 0);
        applyOrder(false, order, new int[]{to}, columns);
    }

    boolean stopCellEditing() {
        if (!tblEditor.isEditing()) return true;
        if (!tblEditor.getCellEditor().stopCellEditing()) return false;
        getTable().getModel().updateTableImmediately();
        return true;
    }

    void finishColumnReordering() {
        if (committingColumnOrder) return;
        committingColumnOrder = true;
        try {
            int[] order = IntStream.range(0, tblEditor.getColumnCount()).map(tblEditor::convertColumnIndexToModel).toArray();
            boolean changed = IntStream.range(0, order.length).anyMatch(index -> order[index] != index);
            if (!changed) return;
            int[] rows = tblEditor.getSelectedRows();
            int[] columns = tblEditor.getSelectedColumns();
            // Normalize Swing's view mapping before changing the underlying document.
            normalizeColumns();
            storeCurrentTableLayout();
            if (isEditable() && !getTableModel().hasErrors() && stopCellEditing()) {
                applyOrder(true, order, rows, columns);
            }
        } finally {
            committingColumnOrder = false;
        }
    }

    private void applyOrder(boolean columns, int[] order, int[] selectedRows, int[] selectedColumns) {
        CsvTableModelSwing model = getTable().getModel();
        int[] widths = getTableEditorState().getColumnWidths().clone();
        AtomicBoolean suspended = new AtomicBoolean();
        reordering = true;
        try {
            updateInteractionElements();
            removeTableChangeListener();
            if (getDocument().getTextLength() >= BACKGROUND_REORDER_THRESHOLD || tblEditor.getRowCount() >= 10_000) {
                if (!applyLargeReorder(columns, order, model, suspended) || isDisposed()) return;
            } else {
                model.suspend();
                suspended.set(true);
                if (columns) model.reorderColumns(order);
                else model.reorderRows(order);
            }
            if (columns) setAllColumnWidths(Arrays.stream(order).map(index -> widths[index]).toArray());
            selectionAfterReorder = new Selection(selectedRows, selectedColumns);
        } finally {
            try {
                if (suspended.get() && !isDisposed()) model.resume();
            } finally {
                reordering = false;
                if (!isDisposed()) {
                    updateInteractionElements();
                    applyTableChangeListener();
                    storeCurrentTableLayout();
                }
            }
        }
    }

    private boolean applyLargeReorder(boolean columns, int[] order, CsvTableModelSwing model, AtomicBoolean suspended) {
        Document document = Objects.requireNonNull(getDocument());
        CharSequence snapshot = document.getImmutableCharSequence();
        long stamp = document.getModificationStamp();
        var separator = CsvHelper.getValueSeparator(project, file);
        var escape = CsvHelper.getEscapeCharacter(project, file);
        String comment = CsvEditorSettings.getInstance().getCommentIndicator();
        var lexer = CsvLexerFactory.getInstance().createLexer(separator, escape, comment);
        Ref<ModelState> state = Ref.create();
        boolean completed;
        try {
            completed = runReorderWithProgress(() -> {
                CsvTableReorder.Change change = CsvTableReorder.prepare(snapshot, lexer, order, columns, separator, escape, comment);
                ProgressManager.checkCanceled();
                ModalityState modality = ModalityState.defaultModalityState();
                // Once written, finish refreshing the model even if Cancel is pressed.
                ProgressManager.getInstance().executeNonCancelableSection(() -> {
                    ApplicationManager.getApplication().invokeAndWait(() ->
                            WriteCommandAction.runWriteCommandAction(project, "CSV Editor Changes", null, () -> {
                                if (!isValid() || document.getModificationStamp() != stamp ||
                                        !document.isWritable() || !file.isWritable() ||
                                        !separator.getCharacter().equals(CsvHelper.getValueSeparator(project, file).getCharacter()) ||
                                        !escape.getCharacter().equals(CsvHelper.getEscapeCharacter(project, file).getCharacter()) ||
                                        !comment.equals(CsvEditorSettings.getInstance().getCommentIndicator())) return;
                                model.suspend();
                                suspended.set(true);
                                if (change != null) DocumentUtil.executeInBulk(document, () ->
                                        document.replaceString(change.range().getStartOffset(), change.range().getEndOffset(), change.text()));
                            }), modality);
                    if (suspended.get()) state.set(ReadAction.nonBlocking(model::captureState)
                            .withDocumentsCommitted(project).expireWith(this).executeSynchronously());
                });
            });
        } finally {
            if (suspended.get() && !isDisposed()) {
                if (state.get() == null) PsiDocumentManager.getInstance(project).commitDocument(document);
                model.prepareUpdate(state.get());
            }
        }
        if (completed && !suspended.get() && !isDisposed()) {
            NotificationGroupManager.getInstance().getNotificationGroup("net.seesharpsoft.intellij.plugins.csv")
                    .createNotification(CsvPluginManager.getLocalizedText("table.reorder.changed"), NotificationType.WARNING).notify(project);
        }
        return suspended.get();
    }

    protected boolean runReorderWithProgress(Runnable operation) {
        return ProgressManager.getInstance().runProcessWithProgressSynchronously(
                operation, CsvPluginManager.getLocalizedText("table.reorder.progress"), true, project);
    }

    private void normalizeColumns() {
        for (int column = 0; column < tblEditor.getColumnCount(); column++) {
            int current = tblEditor.convertColumnIndexToView(column);
            if (current != column) tblEditor.getColumnModel().moveColumn(current, column);
        }
    }

    private void restoreSelection(int[] rows, int[] columns) {
        CsvTableRowUtilities.setSelection(tblEditor.getSelectionModel(), rows, tblEditor.getRowCount());
        CsvTableRowUtilities.setSelection(tblEditor.getColumnModel().getSelectionModel(), columns, tblEditor.getColumnCount());
    }

    protected CsvTable getTable() {
        return (CsvTable) this.tblEditor;
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
        return this.editorComponent;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.tblEditor;
    }

    @Override
    public void storeCurrentTableLayout() {
        if (isReordering()) return;
        int[] widths = getCurrentColumnsWidths();
        float zoomFactor = getZoomFactor();
        for (int i = 0; i < widths.length; i++) {
            widths[i] = (int) (widths[i] / zoomFactor);
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
        if (text == null || text.isEmpty()) {
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
