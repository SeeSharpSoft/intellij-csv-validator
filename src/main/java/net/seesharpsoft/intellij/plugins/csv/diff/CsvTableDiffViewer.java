package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.diff.DiffContext;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.contents.FileContent;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.openapi.diff.DiffColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.icons.AllIcons;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.util.Alarm;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.JBColor;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableBase;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableModelSwingBase;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvMultiLineCellRenderer;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableRowUtilities;
import net.seesharpsoft.intellij.plugins.csv.highlighter.CsvTextAttributeKeys;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.concurrency.CancellablePromise;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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
        JPanel[] panes = new JPanel[2];
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
            String title = request.getContentTitles().get(side);
            table.getAccessibleContext().setAccessibleName(title);
            JPanel pane = new JPanel(new BorderLayout());
            pane.add(new JLabel(title), BorderLayout.NORTH);
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
    }

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    private Color rowMarker(int row) {
        if (comparison == null || row >= comparison.rows().size()) return null;
        CsvDiffComparison.Change change = comparison.changeAt(row);
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
        FrameDiffTool.ToolbarComponents toolbar = new FrameDiffTool.ToolbarComponents();
        toolbar.statusPanel = status;
        toolbar.toolbarActions = List.of(new NavigationAction(false), new NavigationAction(true));
        return toolbar;
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
            sink.set(CommonDataKeys.PROJECT, context.getProject());
            sink.set(PlatformDataKeys.FILE_EDITOR, fileEditors[focusedSide]);
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
        public void uiDataSnapshot(@NotNull DataSink sink) {
            sink.set(CommonDataKeys.PROJECT, context.getProject());
            sink.set(PlatformDataKeys.FILE_EDITOR, fileEditors[side]);
        }
    }

    private final class DividerHost implements CsvTableDiffDivider.Host {
        @Override
        public CsvDiffComparison comparison() {
            return comparison;
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
        int selected = tables[side].getSelectedRow();
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
        for (JTable table : tables) {
            table.changeSelection(row, column, false, false);
            table.scrollRectToVisible(table.getCellRect(row, column, true));
        }
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
        pending = ReadAction.nonBlocking(() -> CsvDiffComparison.compare(readSides()))
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
        if (!isCurrent(result)) {
            scheduleRefresh();
            return;
        }
        int oldColumns = tables[0].getColumnCount();
        int[][] selectedRows = new int[2][];
        int[][] selectedColumns = new int[2][];
        int[][] widths = new int[2][oldColumns];
        for (int side = 0; side < 2; side++) {
            selectedRows[side] = tables[side].getSelectedRows();
            selectedColumns[side] = tables[side].getSelectedColumns();
            for (int column = 0; column < oldColumns; column++) {
                widths[side][column] = tables[side].getColumnModel().getColumn(column).getWidth();
            }
        }
        comparison = result;
        refreshing = false;
        status.setText(result.error() == null
                ? String.format(CsvPluginManager.getLocalizedText("table.diff.differences"), result.differences().size())
                : CsvPluginManager.getLocalizedText(result.error()));
        for (int side = 0; side < 2; side++) {
            JTable table = tables[side];
            if (oldColumns != comparison.columns()) {
                models[side].fireTableStructureChanged();
                for (int column = 0; column < table.getColumnCount(); column++) {
                    int width = column < oldColumns ? widths[side][column] : CsvEditorSettings.getInstance().getTableDefaultColumnWidth();
                    table.getColumnModel().getColumn(column).setPreferredWidth(width);
                    table.getColumnModel().getColumn(column).setWidth(width);
                }
            } else {
                models[side].fireTableDataChanged();
            }
            table.clearSelection();
            for (int row : selectedRows[side]) {
                if (row < table.getRowCount()) table.addRowSelectionInterval(row, row);
            }
            for (int column : selectedColumns[side]) {
                if (column < table.getColumnCount()) table.addColumnSelectionInterval(column, column);
            }
        }
        updateRowHeights();
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
            for (int row = 0; row < comparison.rowLines().size(); row++) {
                int height = Math.max(minimum, comparison.rowLines().get(row) * lineHeight + JBUI.scale(8));
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
        if (scrolling) return;
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
            return comparison == null ? 0 : comparison.rows().size();
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
            return comparison.value(side, row, column);
        }

        @Override
        public boolean isCommentRow(int row) {
            if (comparison == null || row < 0 || row >= getRowCount() || comparison.isLineBreakRow(row)) return false;
            int index = comparison.rows().get(row).index(side);
            return index >= 0 && comparison.sides().get(side).rows().get(index).comment();
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (comparison == null || row < 0 || row >= getRowCount() ||             refreshing || applying ||
                    !isWritable(side) || column < 0 || comparison.isLineBreakRow(row))
                return false;
            int index = comparison.rows().get(row).index(side);
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
            CsvDiffData.Row record = data.rows().get(comparison.rows().get(row).index(side));
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

    private final class NavigationAction extends DumbAwareAction {
        private final boolean forward;

        NavigationAction(boolean forward) {
            super(CsvPluginManager.getLocalizedText(forward ? "table.diff.next" : "table.diff.previous"));
            this.forward = forward;
            AnAction nativeAction = ActionManager.getInstance().getAction(forward ? IdeActions.ACTION_NEXT_DIFF : IdeActions.ACTION_PREVIOUS_DIFF);
            getTemplatePresentation().setIcon(nativeAction.getTemplatePresentation().getIcon());
            registerCustomShortcutSet(nativeAction.getShortcutSet(), component, CsvTableDiffViewer.this);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            event.getPresentation().setEnabled(nextDifference(forward, sideFor(event)) >= 0);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            focusedSide = sideFor(event);
            revealDifference(nextDifference(forward, focusedSide));
            tables[focusedSide].requestFocusInWindow();
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
        return comparison.changeAt(tables[sideFor(event)].getSelectedRow());
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
        if (Boolean.TRUE.equals(context.getUserData(DiffUserDataKeys.FORCE_READ_ONLY))
                || Boolean.TRUE.equals(request.getUserData(DiffUserDataKeys.FORCE_READ_ONLY))
                || Boolean.TRUE.equals(content.getUserData(DiffUserDataKeys.FORCE_READ_ONLY))) return false;
        boolean[] forced = request.getUserData(DiffUserDataKeys.FORCE_READ_ONLY_CONTENTS);
        return forced == null || side >= forced.length || !forced[side];
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
        protected Color getCellBackgroundColor(JTable table, int row, int column) {
            var scheme = EditorColorsManager.getInstance().getGlobalScheme();
            Color background = super.getCellBackgroundColor(table, row, column);
            if (comparison.changed(row, column)) {
                var aligned = comparison.rows().get(row);
                var key = aligned.index(side) < 0 ? DiffColors.DIFF_ABSENT
                        : aligned.index(1 - side) < 0 ? side == 0 ? DiffColors.DIFF_DELETED : DiffColors.DIFF_INSERTED
                        : DiffColors.DIFF_MODIFIED;
                var attributes = scheme.getAttributes(key);
                Color diffBackground = attributes == null ? null : attributes.getBackgroundColor();
                background = diffBackground == null ? JBColor.LIGHT_GRAY : diffBackground;
            }
            return background;
        }
    }
}
