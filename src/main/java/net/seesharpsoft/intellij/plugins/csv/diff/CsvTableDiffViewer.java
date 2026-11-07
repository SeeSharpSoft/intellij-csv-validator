package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.diff.DiffContext;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.contents.FileContent;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.tools.util.base.HighlightPolicy;
import com.intellij.diff.tools.util.base.IgnorePolicy;
import com.intellij.diff.tools.util.base.TextDiffSettingsHolder;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.diff.util.DiffUtil;
import com.intellij.diff.util.LineCol;
import com.intellij.diff.util.Range;
import com.intellij.diff.util.TextDiffType;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diff.DiffColors;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.Navigatable;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.Alarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;
import net.seesharpsoft.intellij.plugins.csv.highlighter.CsvTextAttributeKeys;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.table.ui.CsvMultiLineCellRenderer;
import net.seesharpsoft.intellij.plugins.csv.table.ui.CsvTableBase;
import net.seesharpsoft.intellij.plugins.csv.table.ui.CsvTableModelSwingBase;
import net.seesharpsoft.intellij.plugins.csv.table.ui.CsvTableRowUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.CancellablePromise;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class CsvTableDiffViewer implements FrameDiffTool.DiffViewer {
    private static final int ROW_MARKER_WIDTH = 5;

    private final DiffContext context;
    private final ContentDiffRequest request;
    private final JPanel component = new DiffPanel();
    private final JTable[] tables = new JTable[2];
    private final Model[] models = new Model[2];
    private final JScrollPane[] scrollPanes = new JScrollPane[2];
    private final JTable[] rowHeaders = new JTable[2];
    private final CsvDiffFileEditor[] fileEditors = new CsvDiffFileEditor[2];
    private final CsvTableDiffDivider divider;
    private final JLabel status = new JLabel();
    private final Alarm refreshAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    private final TextDiffSettingsHolder.TextDiffSettings settings;
    private final FoldRenderer foldRenderer = new FoldRenderer();
    private CsvDiffFolding folding = CsvDiffFolding.expanded(0);
    private List<CsvDiffFolding.Fold> revealed = List.of();
    private CancellablePromise<CsvDiffComparison> pending;
    private CancellablePromise<CsvDiffChange> pendingChange;
    private boolean applying;
    private boolean refreshing;
    private int focusedSide = 1;
    private CsvDiffComparison comparison;
    private boolean scrolling;
    private volatile boolean disposed;

    CsvTableDiffViewer(DiffContext context, ContentDiffRequest request) {
        this.context = context;
        this.request = request;
        this.settings = textSettings(context);
        JPanel[] panes = new JPanel[2];
        List<JComponent> titles = DiffUtil.createSimpleTitles(this, request);
        for (int side = 0; side < 2; side++) {
            models[side] = new Model(side);
            CsvTableBase table = new DiffTable(side, models[side]);
            tables[side] = table;
            int currentSide = side;
            fileEditors[side] = new CsvDiffFileEditor(table, request.getContents().get(side), () -> isWritable(currentSide));
            Disposer.register(this, fileEditors[side]);
            table.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent event) {
                    focusedSide = currentSide;
                }
            });
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setDefaultRenderer(Object.class, new DiffCellRenderer(side, false));
            table.setDefaultEditor(Object.class, new DiffCellRenderer(side, false));
            table.setDefaultRenderer(CsvTableBase.CommentColumn.class, new DiffCellRenderer(side, true));
            table.setDefaultEditor(CsvTableBase.CommentColumn.class, new DiffCellRenderer(side, true));
            table.setFont(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN));
            table.getTableHeader().setReorderingAllowed(false);
            installFoldExpansion(table);
            String title = request.getContentTitles().get(side);
            table.getAccessibleContext().setAccessibleName(title);
            JPanel pane = new JPanel(new BorderLayout());
            JComponent titleComponent = titles.get(side);
            if (titleComponent != null) {
                JPanel titlePanel = new JPanel(new BorderLayout());
                titlePanel.setBorder(JBUI.Borders.emptyLeft(8));
                titlePanel.add(titleComponent, BorderLayout.CENTER);
                pane.add(titlePanel, BorderLayout.NORTH);
            }
            scrollPanes[side] = new JBScrollPane(table);
            scrollPanes[side].getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
            rowHeaders[side] = CsvTableRowUtilities.addRowMarkerColumn(table, JBUI.scale(ROW_MARKER_WIDTH), this::rowMarker);
            pane.add(scrollPanes[side], BorderLayout.CENTER);
            panes[side] = pane;
        }
        divider = new CsvTableDiffDivider(tables[0], tables[1], new DividerHost());
        component.add(panes[0]);
        component.add(divider);
        component.add(panes[1]);
        for (int side = 0; side < 2; side++) {
            int source = side;
            scrollPanes[side].getViewport().addChangeListener(event -> {
                synchronizeScrolling(source);
                divider.repaint();
            });
        }
        updateColors();
        ApplicationManager.getApplication().getMessageBus().connect(this)
                .subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) scheme -> updateColors());
        settings.addListener(new TextDiffSettingsHolder.TextDiffSettings.Listener.Adapter() {
            @Override
            public void foldingChanged() {
                revealed = List.of();
                updateFolding();
            }

            @Override
            public void ignorePolicyChanged() {
                refreshOptions();
            }

            @Override
            public void highlightPolicyChanged() {
                refreshOptions();
            }

            @Override
            public void scrollingChanged() {
                synchronizeScrolling(focusedSide);
            }
        }, this);
    }

    private void refreshOptions() {
        finishEditing();
        revealed = List.of();
        scheduleRefresh();
    }

    /**
     * Shares the diff place's text preferences with the table viewer.
     */
    private static TextDiffSettingsHolder.TextDiffSettings textSettings(DiffContext context) {
        TextDiffSettingsHolder.TextDiffSettings settings = context.getUserData(TextDiffSettingsHolder.TextDiffSettings.KEY);
        if (settings == null) {
            settings = TextDiffSettingsHolder.TextDiffSettings.getSettings(context.getUserData(DiffUserDataKeys.PLACE));
            context.putUserData(TextDiffSettingsHolder.TextDiffSettings.KEY, settings);
        }
        return settings;
    }

    private void installFoldExpansion(JTable table) {
        MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) expandFold(table.rowAtPoint(event.getPoint()));
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                boolean fold = folding.isFold(table.rowAtPoint(event.getPoint()));
                table.setCursor(fold ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
            }
        };
        table.addMouseListener(listener);
        table.addMouseMotionListener(listener);
    }

    private void expandFold(int row) {
        CsvDiffFolding.Fold fold = folding.foldAt(row);
        if (fold == null) return;
        revealed = CsvDiffFolding.reveal(revealed, fold);
        updateFolding();
    }

    private boolean isCollapsingUnchanged() {
        return settings.getContextRange() >= 0 && !settings.isExpandByDefault();
    }

    private CsvDiffFolding computeFolding(CsvDiffComparison result) {
        if (result == null) return CsvDiffFolding.expanded(0);
        if (!isCollapsingUnchanged()) return CsvDiffFolding.expanded(result.rows().size());
        return CsvDiffFolding.collapse(result, settings.getContextRange(), revealed);
    }

    private void updateFolding() {
        if (comparison == null) return;
        finishEditing();
        int[][] selectedRows = selectedRows();
        folding = computeFolding(comparison);
        for (Model model : models) model.fireTableDataChanged();
        restoreSelectedRows(selectedRows);
        updateRowHeights();
    }

    /**
     * Commits cell edits before the displayed rows change, since an editor would otherwise write to a shifted row.
     */
    private void finishEditing() {
        for (JTable table : tables) {
            if (table.isEditing() && !table.getCellEditor().stopCellEditing()) table.getCellEditor().cancelCellEditing();
        }
    }

    /**
     * Returns the aligned rows selected on each side, which remain valid while the folding changes.
     */
    private int[][] selectedRows() {
        int[][] rows = new int[2][];
        for (int side = 0; side < 2; side++) {
            rows[side] = Arrays.stream(tables[side].getSelectedRows()).map(folding::firstRow).toArray();
        }
        return rows;
    }

    private void restoreSelectedRows(int[][] rows) {
        for (int side = 0; side < 2; side++) {
            JTable table = tables[side];
            for (int row : rows[side]) {
                int displayRow = row < comparison.rows().size() ? folding.displayRow(row) : -1;
                if (displayRow >= 0 && displayRow < table.getRowCount()) table.addRowSelectionInterval(displayRow, displayRow);
            }
        }
    }

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    private Color rowMarker(int row) {
        if (comparison == null || row >= folding.displayRowCount() || folding.isFold(row)) return null;
        CsvDiffComparison.Change change = comparison.changeAt(folding.firstRow(row));
        return change == null ? null : change.type().getMarkerColor(null);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return tables[1];
    }

    @Override
    public @NotNull FrameDiffTool.ToolbarComponents init() {
        for (int side = 0; side < 2; side++) {
            Document document = document(side);
            if (document != null) document.addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    scheduleRefresh();
                }
            }, this);
        }
        refresh();
        registerApplyShortcuts();
        installNavigationActions();
        return CsvTableDiffToolbar.create(status,
                List.of(new CollapseUnchangedAction(), Separator.getInstance(), createSwapSidesAction()),
                createOptionsActions());
    }

    private List<AnAction> createOptionsActions() {
        List<AnAction> actions = new ArrayList<>();
        actions.add(new SynchronizeScrollingAction());
        actions.add(Separator.create(CsvPluginManager.getLocalizedText("table.diff.ignore.differences")));
        for (IgnorePolicy policy : List.of(IgnorePolicy.DEFAULT, IgnorePolicy.TRIM_WHITESPACES,
                IgnorePolicy.IGNORE_WHITESPACES, IgnorePolicy.FORMATTING)) {
            actions.add(new OptionAction<>(CsvPluginManager.getLocalizedText("table.diff.ignore." + policy.name()), policy,
                    () -> CsvDiffOptions.capture(settings).ignore(), settings::setIgnorePolicy));
        }
        actions.add(Separator.create(CsvPluginManager.getLocalizedText("table.diff.highlight.differences")));
        for (HighlightPolicy policy : List.of(HighlightPolicy.BY_LINE, HighlightPolicy.BY_WORD,
                HighlightPolicy.BY_CHAR, HighlightPolicy.DO_NOT_HIGHLIGHT)) {
            actions.add(new OptionAction<>(CsvPluginManager.getLocalizedText("table.diff.highlight." + policy.name()), policy,
                    () -> CsvDiffOptions.capture(settings).highlight(), settings::setHighlightPolicy));
        }
        actions.add(Separator.getInstance());
        actions.add(new DumbAwareAction(CsvPluginManager.getLocalizedText("table.diff.help"), null, AllIcons.Actions.Help) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                BrowserUtil.browse("https://github.com/SeeSharpSoft/intellij-csv-validator/blob/main/README.md#table-diffs");
            }
        });
        return List.copyOf(actions);
    }

    private static final class OptionAction<T> extends DumbAwareAction implements Toggleable {
        private final T value;
        private final Supplier<T> selected;
        private final Consumer<T> select;

        OptionAction(String text, T value, Supplier<T> selected, Consumer<T> select) {
            super(text);
            this.value = value;
            this.selected = selected;
            this.select = select;
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            Toggleable.setSelected(event.getPresentation(), value.equals(selected.get()));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            select.accept(value);
        }
    }

    private final class SynchronizeScrollingAction extends DumbAwareToggleAction {
        SynchronizeScrollingAction() {
            super(CsvPluginManager.getLocalizedText("table.diff.synchronize.scrolling"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent event) {
            return settings.isEnableSyncScroll();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent event, boolean state) {
            focusedSide = sideFor(event);
            settings.setEnableSyncScroll(state);
        }
    }

    private AnAction createSwapSidesAction() {
        AnAction action = new AnActionWrapper(ActionManager.getInstance().getAction("SwapSidesInDiffWindow")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                finishEditing();
                super.actionPerformed(event);
            }
        };
        action.registerCustomShortcutSet(action.getShortcutSet(), component, this);
        return action;
    }

    private void installNavigationActions() {
        ActionManager manager = ActionManager.getInstance();
        DefaultActionGroup group = (DefaultActionGroup) manager.getAction(CsvTableDiffNavigationActions.GROUP_ID);
        for (AnAction action : group.getChildren(manager)) {
            action.registerCustomShortcutSet(component, this);
        }
        // The host builds its common toolbar after DiffViewer.init() returns.
        ApplicationManager.getApplication().invokeLater(
                () -> CsvTableDiffNavigationActions.prependToToolbar(this),
                ModalityState.any(),
                ignored -> disposed || context.getProject() != null && context.getProject().isDisposed());
    }

    private void registerApplyShortcuts() {
        for (int targetSide = 0; targetSide < 2; targetSide++) {
            AnAction nativeAction = ActionManager.getInstance()
                    .getAction(targetSide == 0 ? "Diff.ApplyRightSide" : "Diff.ApplyLeftSide");
            new ApplyAction(targetSide).registerCustomShortcutSet(nativeAction.getShortcutSet(), component, this);
        }
    }

    private final class DiffPanel extends JPanel implements UiDataProvider {
        DiffPanel() {
            super(new CsvTableDiffDivider.Layout());
        }

        @Override
        public void uiDataSnapshot(@NotNull DataSink sink) {
            provideData(sink, focusedSide);
        }
    }

    private final class DiffTable extends CsvTableBase implements UiDataProvider {
        private final int side;

        DiffTable(int side, Model model) {
            super(model);
            this.side = side;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return getModel().isCellEditable(row, column) && super.isCellEditable(row, column);
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            return folding.isFold(row) ? foldRenderer : super.getCellRenderer(row, column);
        }

        @Override
        public void uiDataSnapshot(@NotNull DataSink sink) {
            provideData(sink, side);
        }
    }

    private void provideData(DataSink sink, int side) {
        sink.set(CsvTableDiffNavigationActions.VIEWER, disposed ? null : this);
        sink.set(CommonDataKeys.PROJECT, context.getProject());
        sink.set(PlatformDataKeys.FILE_EDITOR, fileEditors[side]);
        sink.set(DiffDataKeys.DIFF_CONTEXT, context);
        sink.set(DiffDataKeys.DIFF_REQUEST, request);
        sink.set(DiffDataKeys.CURRENT_CONTENT, request.getContents().get(side));
        sink.set(DiffDataKeys.NAVIGATABLE, ReadAction.compute(() -> getNavigatable(side)));
    }

    private Navigatable getNavigatable(int side) {
        if (disposed) return null;
        int row = tables[side].getSelectionModel().getLeadSelectionIndex();
        int column = tables[side].getColumnModel().getSelectionModel().getLeadSelectionIndex();
        int alignedRow = comparison != null && isCurrent(comparison) && row >= 0 && row < folding.displayRowCount()
                ? folding.firstRow(row) : -1;
        Navigatable navigatable = getNavigatable(side, alignedRow, column);
        return navigatable != null ? navigatable : getNavigatable(1 - side, alignedRow, column);
    }

    private Navigatable getNavigatable(int side, int alignedRow, int column) {
        DiffContent content = request.getContents().get(side);
        if (!(content instanceof DocumentContent documentContent)) return content.getNavigatable();
        Document document = documentContent.getDocument();
        int offset = 0;
        if (alignedRow >= 0) {
            CsvDiffData data = comparison.sides().get(side);
            int record = comparison.rows().get(alignedRow).index(side);
            if (record < 0) {
                Range range = Objects.requireNonNull(comparison.changeAt(alignedRow),
                        "A missing record must belong to a difference").records();
                record = side == 0 ? range.end1 : range.end2;
            }
            if (record >= data.rows().size()) {
                offset = document.getTextLength();
            } else {
                CsvDiffData.Row row = data.rows().get(record);
                offset = !row.comment() && column >= 0 && column < row.cells().size()
                        ? row.cells().get(column).range().getStartOffset() : row.range().getStartOffset();
            }
        }
        return documentContent.getNavigatable(LineCol.fromOffset(document, offset));
    }

    private final class DividerHost implements CsvTableDiffDivider.Host {
        @Override
        public CsvDiffComparison comparison() {
            return comparison;
        }

        @Override
        public CsvDiffFolding folding() {
            return folding;
        }

        @Override
        public boolean isWritable(int side) {
            return CsvTableDiffViewer.this.isWritable(side);
        }

        @Override
        public void apply(CsvDiffComparison.Change change, int targetSide) {
            applyChange(change, targetSide);
        }
    }

    private int nextDifference(boolean forward, int side) {
        if (comparison == null || isBusy()) return -1;
        int selectedRow = tables[side].getSelectedRow();
        int selected = selectedRow < 0 ? -1 : forward ? folding.lastRow(selectedRow) : folding.firstRow(selectedRow);
        List<CsvDiffComparison.Change> changes = comparison.differences();
        for (int step = 0; step < changes.size(); step++) {
            int index = forward ? step : changes.size() - 1 - step;
            int row = changes.get(index).startRow();
            if (forward ? row > selected : row < selected) return row;
        }
        return -1;
    }

    private void revealDifference(int row) {
        if (row < 0) return;
        int column = comparison.firstChangedColumn(row);
        int displayRow = folding.displayRow(row);
        for (JTable table : tables) {
            table.changeSelection(displayRow, column, false, false);
            table.scrollRectToVisible(verticallyCentered(table, table.getCellRect(displayRow, column, true)));
        }
    }

    private static Rectangle verticallyCentered(JTable table, Rectangle cell) {
        int extent = table.getVisibleRect().height;
        if (extent <= cell.height) return cell;
        int maxY = Math.max(0, table.getHeight() - extent);
        int y = Math.clamp(cell.y - (extent - cell.height) / 2, 0, maxY);
        return new Rectangle(cell.x, y, cell.width, extent);
    }

    private Document document(int side) {
        return request.getContents().get(side) instanceof DocumentContent content ? content.getDocument() : null;
    }

    private void scheduleRefresh() {
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(this::scheduleRefresh,
                    ModalityState.stateForComponent(component), ignored -> disposed);
            return;
        }
        if (disposed) return;
        refreshing = true;
        for (JTable table : tables) {
            if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        }
        if (pending != null) pending.cancel();
        if (pendingChange != null) pendingChange.cancel();
        applying = false;
        refreshAlarm.cancelAllRequests();
        refreshAlarm.addRequest(this::refresh, 50);
    }

    private void refresh() {
        refreshing = true;
        status.setText(CsvPluginManager.getLocalizedText("table.diff.comparing"));
        CsvDiffOptions options = CsvDiffOptions.capture(settings);
        pending = ReadAction.nonBlocking(() -> CsvDiffComparison.compare(readSides(), options))
                .expireWith(this)
                .expireWhen(() -> disposed || context.getProject() != null && context.getProject().isDisposed())
                .finishOnUiThread(ModalityState.any(), this::showComparison)
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private List<CsvDiffData> readSides() {
        return request.getContents().stream().map(content -> content instanceof DocumentContent document
                ? CsvDiffData.read(document, CsvDiffData.Format.forContent(context.getProject(), document))
                : new CsvDiffData("", -1, null, List.of(), List.of(), true)).toList();
    }

    private void showComparison(CsvDiffComparison result) {
        if (!result.options().equals(CsvDiffOptions.capture(settings)) || !isCurrent(result)) {
            scheduleRefresh();
            return;
        }
        int oldColumns = tables[0].getColumnCount();
        int[][] selectedRows = selectedRows();
        int[][] selectedColumns = new int[2][];
        int[][] widths = new int[2][oldColumns];
        for (int side = 0; side < 2; side++) {
            selectedColumns[side] = tables[side].getSelectedColumns();
            for (int column = 0; column < oldColumns; column++) {
                widths[side][column] = tables[side].getColumnModel().getColumn(column).getWidth();
            }
        }
        comparison = result;
        folding = computeFolding(result);
        refreshing = false;
        status.setText(result.error() == null
                ? String.format(CsvPluginManager.getLocalizedText("table.diff.differences"), result.differences().size())
                : CsvPluginManager.getLocalizedText(result.error()));
        for (int side = 0; side < 2; side++) {
            refreshTable(side, widths[side], selectedColumns[side]);
        }
        restoreSelectedRows(selectedRows);
        updateRowHeights();
    }

    private void refreshTable(int side, int[] widths, int[] selectedColumns) {
        JTable table = tables[side];
        if (widths.length != comparison.columns()) {
            models[side].fireTableStructureChanged();
            for (int column = 0; column < table.getColumnCount(); column++) {
                int width = column < widths.length ? widths[column] : CsvEditorSettings.getInstance().getTableDefaultColumnWidth();
                table.getColumnModel().getColumn(column).setPreferredWidth(width);
                table.getColumnModel().getColumn(column).setWidth(width);
            }
        } else {
            models[side].fireTableDataChanged();
        }
        table.clearSelection();
        for (int column : selectedColumns) {
            if (column < table.getColumnCount()) table.addColumnSelectionInterval(column, column);
        }
    }

    private void updateColors() {
        var scheme = EditorColorsManager.getInstance().getGlobalScheme();
        for (int side = 0; side < 2; side++) {
            CsvTextAttributeKeys.clearColumnTextAttributes(fileEditors[side]);
            tables[side].updateUI();
            tables[side].setFont(scheme.getFont(EditorFontType.PLAIN));
            scrollPanes[side].getViewport().setBackground(tables[side].getBackground());
        }
        if (comparison != null) updateRowHeights();
    }

    private void updateRowHeights() {
        for (int side = 0; side < 2; side++) {
            JTable table = tables[side];
            int lineHeight = table.getFontMetrics(table.getFont()).getHeight();
            int minimum = CsvEditorSettings.getInstance().getTableEditorRowHeight();
            table.setRowHeight(minimum);
            rowHeaders[side].setRowHeight(minimum);
            for (int row = 0; row < folding.displayRowCount(); row++) {
                if (folding.isFold(row)) continue;
                int height = Math.max(minimum, comparison.rowLines().get(folding.firstRow(row)) * lineHeight + JBUI.scale(8));
                if (height == minimum) continue;
                table.setRowHeight(row, height);
                rowHeaders[side].setRowHeight(row, height);
            }
        }
        // Row number digits and fonts determine the divider width.
        divider.revalidate();
        divider.repaint();
    }

    private void synchronizeScrolling(int side) {
        if (scrolling || !settings.isEnableSyncScroll()) return;
        scrolling = true;
        try {
            JViewport source = scrollPanes[side].getViewport();
            JViewport target = scrollPanes[1 - side].getViewport();
            Point position = source.getViewPosition();
            position.x = Math.min(position.x, Math.max(0, target.getViewSize().width - target.getExtentSize().width));
            position.y = Math.min(position.y, Math.max(0, target.getViewSize().height - target.getExtentSize().height));
            target.setViewPosition(position);
        } finally {
            scrolling = false;
        }
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (pending != null) pending.cancel();
        if (pendingChange != null) pendingChange.cancel();
        pending = null;
        pendingChange = null;
        comparison = null;
        folding = CsvDiffFolding.expanded(0);
        for (JTable table : tables) {
            if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        }
        for (Model model : models) model.fireTableDataChanged();
        divider.repaint();
    }

    private final class Model extends CsvTableModelSwingBase {
        private final int side;

        Model(int side) {
            this.side = side;
        }

        @Override
        public int getRowCount() {
            return folding.displayRowCount();
        }

        @Override
        public int getColumnCount() {
            return comparison == null ? 0 : comparison.columns();
        }

        @Override
        protected String getHeaderText(int column) {
            if (comparison == null) return "";
            List<String> headers = comparison.sides().get(side).headers();
            return column < headers.size() ? headers.get(column) : "";
        }

        @Override
        public String getValue(int row, int column) {
            return folding.isFold(row) ? "" : comparison.value(side, folding.firstRow(row), column);
        }

        /**
         * Fold rows span all columns like comments.
         */
        @Override
        public boolean isCommentRow(int row) {
            if (comparison == null || row < 0 || row >= getRowCount()) return false;
            if (folding.isFold(row)) return true;
            int alignedRow = folding.firstRow(row);
            if (comparison.isLineBreakRow(alignedRow)) return false;
            int index = comparison.rows().get(alignedRow).index(side);
            return index >= 0 && comparison.sides().get(side).rows().get(index).comment();
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (comparison == null || row < 0 || row >= getRowCount() || refreshing || applying ||
                    !isWritable(side) || column < 0 || folding.isFold(row)) return false;
            int alignedRow = folding.firstRow(row);
            if (comparison.isLineBreakRow(alignedRow)) return false;
            int index = comparison.rows().get(alignedRow).index(side);
            if (index < 0) return false;
            CsvDiffData.Row record = comparison.sides().get(side).rows().get(index);
            return !record.comment() || column == 0;
        }

        @Override
        public void setValue(String value, int row, int column) {
            if (!isCellEditable(row, column)) {
                status.setText(CsvPluginManager.getLocalizedText("table.diff.read.only"));
                return;
            }
            if (!isCurrent(comparison)) {
                status.setText(CsvPluginManager.getLocalizedText("table.diff.changed"));
                scheduleRefresh();
                return;
            }
            CsvDiffData data = comparison.sides().get(side);
            CsvDiffData.Row record = data.rows().get(comparison.rows().get(folding.firstRow(row)).index(side));
            String text = value;
            TextRange range;
            if (record.comment()) {
                if (text.contains("\n") || text.contains("\r")) {
                    status.setText(CsvPluginManager.getLocalizedText("table.diff.invalid.comment"));
                    return;
                }
                range = record.range();
                text = data.format().comment() + text;
            } else if (column < record.cells().size()) {
                range = record.cells().get(column).range();
                boolean quoted = range.substring(data.text()).trim().startsWith("\"");
                text = quote(data.format(), text, column == 0, quoted);
            } else {
                range = new TextRange(record.range().getEndOffset(), record.range().getEndOffset());
                text = data.format().separator().getCharacter().repeat(column + 1 - record.cells().size())
                        + quote(data.format(), text, false, false);
            }
            replaceDocument(side, data.stamp(), new CsvDiffChange(range, text));
        }
    }

    boolean canNavigate(boolean forward, AnActionEvent event) {
        return !disposed && nextDifference(forward, sideFor(event)) >= 0;
    }

    void navigate(boolean forward, AnActionEvent event) {
        if (!canNavigate(forward, event)) return;
        focusedSide = sideFor(event);
        revealDifference(nextDifference(forward, focusedSide));
        tables[focusedSide].requestFocusInWindow();
    }

    private final class CollapseUnchangedAction extends DumbAwareToggleAction {
        CollapseUnchangedAction() {
            super(CsvPluginManager.getLocalizedText("table.diff.collapse.unchanged"), null, AllIcons.Actions.Collapseall);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            super.update(event);
            event.getPresentation().setVisible(settings.getContextRange() >= 0);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent event) {
            return !settings.isExpandByDefault();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent event, boolean state) {
            settings.setExpandByDefault(!state);
        }
    }

    /**
     * Renders a fold row as a wavy line through all columns, like the text diff's collapsed unchanged fragments.
     */
    private final class FoldRenderer extends JComponent implements TableCellRenderer {
        private int originX;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            CsvDiffFolding.Fold fold = folding.foldAt(row);
            setToolTipText(fold == null ? null
                    : String.format(CsvPluginManager.getLocalizedText("table.diff.folded.rows"), fold.size()));
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            originX = CsvDiffFoldWave.originX(table) + table.getCellRect(row, column, false).x;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            CsvDiffFoldWave.paint(g, 0, getWidth(), 0, getHeight(), originX);
        }
    }

    private final class ApplyAction extends DumbAwareAction {
        private final int targetSide;

        ApplyAction(int targetSide) {
            super(CsvPluginManager.getLocalizedText(targetSide == 0 ? "table.diff.apply.left" : "table.diff.apply.right"),
                    null, targetSide == 0 ? AllIcons.Diff.Arrow : AllIcons.Diff.ArrowRight);
            this.targetSide = targetSide;
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            event.getPresentation().setEnabled(!isBusy() && comparison != null && isWritable(targetSide)
                    && selectedChange(event) != null);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            applyChange(comparison == null ? null : selectedChange(event), targetSide);
        }
    }

    private void applyChange(CsvDiffComparison.Change change, int targetSide) {
        if (isBusy()) {
            status.setText(CsvPluginManager.getLocalizedText("table.diff.wait"));
            return;
        }
        if (comparison == null || !isWritable(targetSide)) {
            status.setText(CsvPluginManager.getLocalizedText("table.diff.read.only"));
            return;
        }
        if (change == null) {
            status.setText(CsvPluginManager.getLocalizedText("table.diff.select.change"));
            return;
        }
        CsvDiffComparison snapshot = comparison;
        applying = true;
        status.setText(CsvPluginManager.getLocalizedText("table.diff.applying"));
        pendingChange = ReadAction.nonBlocking(() -> CsvDiffChange.prepare(snapshot, change, targetSide))
                .expireWith(this)
                .expireWhen(() -> disposed)
                .finishOnUiThread(ModalityState.current(), prepared -> {
                    applying = false;
                    if (!isWritable(targetSide) || !isCurrent(snapshot)) {
                        status.setText(CsvPluginManager.getLocalizedText("table.diff.changed"));
                        scheduleRefresh();
                        return;
                    }
                    replaceDocument(targetSide, snapshot.sides().get(targetSide).stamp(), prepared);
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private CsvDiffComparison.Change selectedChange(AnActionEvent event) {
        int row = tables[sideFor(event)].getSelectedRow();
        return row < 0 || folding.isFold(row) ? null : comparison.changeAt(folding.firstRow(row));
    }

    private int sideFor(AnActionEvent event) {
        int side = focusedSide;
        var editor = event.getData(PlatformDataKeys.FILE_EDITOR);
        for (int candidate = 0; candidate < fileEditors.length; candidate++) {
            if (editor == fileEditors[candidate]) side = candidate;
        }
        return side;
    }

    private boolean isBusy() {
        return refreshing || applying || tables[0].isEditing() || tables[1].isEditing();
    }

    private static String quote(CsvDiffData.Format format, String text, boolean first, boolean quoted) {
        return CsvHelper.quoteCsvField(text, format.escape(), format.separator(), quoted || format.quoting()
                || first && !format.comment().isEmpty() && text.trim().startsWith(format.comment()));
    }

    private void replaceDocument(int side, long stamp, CsvDiffChange change) {
        Document document = document(side);
        if (document == null || !isWritable(side)) {
            status.setText(CsvPluginManager.getLocalizedText("table.diff.read.only"));
            return;
        }
        if (!CsvPsiTreeUpdater.replaceDocumentText(context.getProject(), document, stamp, change.range(), change.text())) {
            status.setText(CsvPluginManager.getLocalizedText("table.diff.changed"));
            scheduleRefresh();
        }
    }

    private boolean isWritable(int side) {
        if (disposed || context.getProject() != null && context.getProject().isDisposed()) return false;
        var content = request.getContents().get(side);
        Document document = document(side);
        if (document == null || !document.isWritable()) return false;
        var documentFile = FileDocumentManager.getInstance().getFile(document);
        if (documentFile != null && !documentFile.isWritable()) return false;
        if (content instanceof FileContent file && !file.getFile().isWritable()) return false;
        return !isForcedReadOnly(side, content);
    }

    private boolean isForcedReadOnly(int side, DiffContent content) {
        if (Boolean.TRUE.equals(context.getUserData(DiffUserDataKeys.FORCE_READ_ONLY))
                || Boolean.TRUE.equals(request.getUserData(DiffUserDataKeys.FORCE_READ_ONLY))
                || Boolean.TRUE.equals(content.getUserData(DiffUserDataKeys.FORCE_READ_ONLY))) return true;
        boolean[] forced = request.getUserData(DiffUserDataKeys.FORCE_READ_ONLY_CONTENTS);
        return forced != null && side < forced.length && forced[side];
    }

    private boolean isCurrent(CsvDiffComparison result) {
        for (int side = 0; side < 2; side++) {
            if (request.getContents().get(side) instanceof DocumentContent content) {
                CsvDiffData data = result.sides().get(side);
                if (content.getDocument().getModificationStamp() != data.stamp()
                        || !data.format().isCurrent(context.getProject(), content)) return false;
            }
        }
        return true;
    }

    private final class DiffCellRenderer extends CsvMultiLineCellRenderer {
        private final int side;
        private final boolean comment;

        DiffCellRenderer(int side, boolean comment) {
            super(fileEditors[side]);
            this.side = side;
            this.comment = comment;
        }

        @Override
        protected TextAttributes getColumnTextAttributes(int column) {
            return comment ? CsvTextAttributeKeys.getCommentTextAttributes() : super.getColumnTextAttributes(column);
        }

        @Override
        protected boolean usePlainFocusedCellColors() {
            return false;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            getTextArea().getHighlighter().removeAllHighlights();
            Component renderer = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            var fragments = comparison.fragments(folding.firstRow(row), column);
            if (selected || fragments.isEmpty()) return renderer;
            var painter = new DefaultHighlighter.DefaultHighlightPainter(TextDiffType.MODIFIED.getColor(null));
            for (var fragment : fragments) {
                int start = side == 0 ? fragment.getStartOffset1() : fragment.getStartOffset2();
                int end = side == 0 ? fragment.getEndOffset1() : fragment.getEndOffset2();
                try {
                    getTextArea().getHighlighter().addHighlight(start, end, painter);
                } catch (BadLocationException exception) {
                    throw new IllegalStateException("CSV diff fragment outside its cell", exception);
                }
            }
            return renderer;
        }

        @Override
        protected Color getCellBackgroundColor(JTable table, int row, int column) {
            var scheme = EditorColorsManager.getInstance().getGlobalScheme();
            Color background = super.getCellBackgroundColor(table, row, column);
            int alignedRow = folding.firstRow(row);
            if (comparison.options().highlight() != HighlightPolicy.DO_NOT_HIGHLIGHT && comparison.changed(alignedRow, column)) {
                var aligned = comparison.rows().get(alignedRow);
                var key = aligned.index(side) < 0 ? DiffColors.DIFF_ABSENT
                        : aligned.index(1 - side) < 0 ? side == 0 ? DiffColors.DIFF_DELETED : DiffColors.DIFF_INSERTED
                        : DiffColors.DIFF_MODIFIED;
                var attributes = scheme.getAttributes(key);
                Color diffBackground = attributes == null ? null : attributes.getBackgroundColor();
                if (!comparison.fragments(alignedRow, column).isEmpty()) {
                    diffBackground = TextDiffType.MODIFIED.getIgnoredColor(null);
                }
                background = diffBackground == null ? JBColor.LIGHT_GRAY : diffBackground;
            }
            return background;
        }
    }
}
