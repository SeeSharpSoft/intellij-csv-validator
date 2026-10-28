package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.find.FindBundle;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.find.SearchReplaceComponent;
import com.intellij.find.SearchSession;
import com.intellij.find.editorHeaderActions.NextOccurrenceAction;
import com.intellij.find.editorHeaderActions.PrevOccurrenceAction;
import com.intellij.find.editorHeaderActions.ToggleMatchCase;
import com.intellij.find.editorHeaderActions.ToggleRegex;
import com.intellij.find.editorHeaderActions.ToggleWholeWordsOnlyAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.util.DocumentUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.CancellablePromise;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Search operates on displayed cell values; replacements use the CSV writer to preserve valid quoting.
 * Matches use view coordinates. Subclasses provide selection, editing and highlighting, and override
 * the identity index mappings when their table view reorders or filters model cells.
 */
public abstract class CsvTableSearchSessionBase<T extends CsvTableEditor>
        implements SearchSession, SearchReplaceComponent.Listener, Disposable {
    protected final T editor;
    private final FindModel findModel = new FindModel();
    private final FindManager findManager;
    private final SearchReplaceComponent component;
    private final JLabel statusLabel = new JLabel();
    private final Document document;
    private List<Match> matches = List.of();
    private CancellablePromise<SearchResult> searchTask;
    private final AtomicLong searchGeneration = new AtomicLong();
    private long resultsStamp;
    private boolean searching;
    private int current = -1;
    private Match pendingAnchor;
    private FindModel searchedModel;
    private boolean replacing;
    private volatile boolean disposed;

    protected CsvTableSearchSessionBase(T editor) {
        this.editor = editor;
        findManager = FindManager.getInstance(Objects.requireNonNull(editor.getProject()));
        document = Objects.requireNonNull(editor.getDocument());
        component = SearchReplaceComponent.buildFor(editor.getProject(), Objects.requireNonNull(editor.getPreferredFocusedComponent()))
                .withDataProvider(dataId -> SearchSession.KEY.is(dataId) ? this : null)
                .addPrimarySearchActions(new NextOccurrenceAction(), new PrevOccurrenceAction())
                .addExtraSearchActions(new ToggleMatchCase(), new ToggleWholeWordsOnlyAction(), new ToggleRegex())
                .addPrimaryReplaceActions(new ReplaceAction(false), new ReplaceAction(true))
                .withReplaceAction(this::replaceCurrent)
                .withCloseAction(this::close)
                .withAddSearchResultsToGlobalSearch(false)
                .build();
        configureSearchControls();
        component.addListener(this);
        findModel.addObserver(model -> {
            component.update(model.getStringToFind(), model.getStringToReplace(), model.isReplaceState(), component.isMultiline());
            if (searchedModel == null || !model.getStringToFind().equals(searchedModel.getStringToFind())
                    || model.isCaseSensitive() != searchedModel.isCaseSensitive()
                    || model.isWholeWordsOnly() != searchedModel.isWholeWordsOnly()
                    || model.isRegularExpressions() != searchedModel.isRegularExpressions()) {
                refresh(null);
            }
        });
        registerNavigationShortcut(IdeActions.ACTION_FIND_NEXT, true);
        registerNavigationShortcut(IdeActions.ACTION_FIND_PREVIOUS, false);
        component.setVisible(false);
    }

    protected abstract int getSelectedRow();

    protected abstract int getSelectedColumn();

    protected abstract boolean stopEditing();

    /** Selects, reveals and highlights the match unless the table is currently being edited. */
    protected abstract void selectMatch(Match match);

    protected abstract void clearMatchHighlight();

    /** Snapshots model-to-view row indices on the EDT; null denotes identity and negative indices denote hidden rows. */
    protected int[] getViewRowIndices() {
        return null;
    }

    /** Returns the view index of a model column, or a negative index for a hidden column. */
    protected int convertColumnIndexToView(int column) {
        return column;
    }

    protected int convertRowIndexToModel(int row) {
        return row;
    }

    protected int convertColumnIndexToModel(int column) {
        return column;
    }

    private void configureSearchControls() {
        statusLabel.setBorder(JBUI.Borders.empty(0, 8));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        OnePixelSplitter splitter = Objects.requireNonNull(UIUtil.findComponentOfType(component, OnePixelSplitter.class));
        JComponent originalControls = splitter.getSecondComponent();
        Component[] rows = originalControls.getComponents();
        ((JPanel) rows[0]).add(statusLabel, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        controls.setOpaque(false);
        controls.setBorder(originalControls.getBorder());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        // Keep each toolbar at its preferred width so the native Close action follows the arrows.
        for (int row = 0; row < rows.length; row++) {
            constraints.gridy = row;
            controls.add(rows[row], constraints);
        }
        constraints.gridy = rows.length;
        constraints.weighty = 1;
        controls.add(Box.createVerticalGlue(), constraints);
        for (ActionToolbarImpl toolbar : UIUtil.findComponentsOfType(controls, ActionToolbarImpl.class)) {
            toolbar.setReservePlaceAutoPopupIcon(false);
            toolbar.setForceMinimumSize(true);
        }
        splitter.setSecondComponent(controls);
        splitter.setHonorComponentsMinimumSize(true);
        splitter.setLackOfSpaceStrategy(Splitter.LackOfSpaceStrategy.HONOR_THE_SECOND_MIN_SIZE);
    }

    private void registerNavigationShortcut(String actionId, boolean forward) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                navigate(forward);
            }

            @Override
            public void update(@NotNull AnActionEvent event) {
                event.getPresentation().setEnabled(component.isVisible() && hasMatches());
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(ActionManager.getInstance().getAction(actionId).getShortcutSet(),
                editor.getComponent(), editor);
    }

    public void show(boolean replace) {
        component.setVisible(true);
        findModel.setReplaceState(replace);
        component.update(findModel.getStringToFind(), findModel.getStringToReplace(), replace, component.isMultiline());
        refresh(currentMatch());
        component.requestFocusInTheSearchFieldAndSelectContent(editor.getProject());
    }

    @Override
    public @NotNull FindModel getFindModel() {
        return findModel;
    }

    @Override
    public @NotNull SearchReplaceComponent getComponent() {
        return component;
    }

    @Override
    public boolean hasMatches() {
        return !searching && !replacing && pendingAnchor == null && resultsStamp == document.getModificationStamp() && !matches.isEmpty();
    }

    @Override
    public boolean isSearchInProgress() {
        return searching;
    }

    @Override
    public void searchForward() {
        navigate(true);
    }

    @Override
    public void searchBackward() {
        navigate(false);
    }

    private void navigate(boolean forward) {
        if (!hasMatches() || !stopEditing()) return;
        Match match = currentMatch();
        if (match != null && match.row == getSelectedRow() && match.column == getSelectedColumn()) {
            current = Math.floorMod(current + (forward ? 1 : -1), matches.size());
        } else {
            Match anchor = selectionAnchor();
            current = forward ? 0 : matches.size() - 1;
            for (int index = 0; index < matches.size(); index++) {
                int comparison = matches.get(index).compareTo(anchor);
                if (forward && comparison >= 0) {
                    current = index;
                    break;
                }
                if (!forward && comparison < 0) current = index;
            }
        }
        selectCurrent();
    }

    @Override
    public void searchFieldDocumentChanged() {
        if (!disposed) {
            String query = component.getSearchTextComponent().getText();
            if (!query.equals(findModel.getStringToFind())) current = -1;
            findModel.setStringToFind(query);
        }
    }

    @Override
    public void replaceFieldDocumentChanged() {
        if (!disposed) findModel.setStringToReplace(component.getReplaceTextComponent().getText());
    }

    @Override
    public void toggleSearchReplaceMode() {
        findModel.setReplaceState(!findModel.isReplaceState());
    }

    /** Refreshes results after the table model has invalidated its cached data. */
    public void tableChanged() {
        if (!disposed && !replacing) {
            Match anchor = pendingAnchor == null ? currentMatch() : pendingAnchor;
            pendingAnchor = null;
            refresh(anchor);
        }
    }

    private Match currentMatch() {
        return current >= 0 && current < matches.size() ? matches.get(current) : null;
    }

    private Match selectionAnchor() {
        return new Match(Math.max(0, getSelectedRow()), Math.max(0, getSelectedColumn()), 0, 0);
    }

    private void refresh(Match anchor) {
        if (disposed || pendingAnchor != null || !component.isVisible()) return;
        Match target = anchor == null ? selectionAnchor() : anchor;
        cancelSearch();
        clearMatchHighlight();
        matches = List.of();
        current = -1;
        searchedModel = findModel.clone();
        component.setRegularBackground();
        if (findModel.getStringToFind().isEmpty()) {
            selectCurrent();
            return;
        }
        searching = true;
        setStatusText(CsvPluginManager.getResourceBundle().getString("table.search.searching"));
        component.updateActions();
        long generation = searchGeneration.get();
        FindModel query = searchedModel;
        startSearch(query, target, generation);
    }

    private void startSearch(FindModel query, Match anchor, long generation) {
        if (disposed || generation != searchGeneration.get()) return;
        // Snapshot view indices on the EDT. Background work never reads the table model or its PSI cursor.
        int rowCount = editor.getTableModel().getRowCount();
        int[] rows = getViewRowIndices();
        int[] columns = getViewColumnIndices();
        long stamp = document.getModificationStamp();
        searchTask = ReadAction.nonBlocking(() -> collectMatches(query, rowCount, rows, columns))
                .withDocumentsCommitted(Objects.requireNonNull(editor.getProject()))
                .expireWith(editor)
                .expireWhen(() -> disposed || generation != searchGeneration.get() || stamp != document.getModificationStamp())
                .coalesceBy(this)
                .finishOnUiThread(ModalityState.any(), result -> publishResults(result, anchor, generation, stamp))
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private int[] getViewColumnIndices() {
        int[] columns = new int[editor.getTableModel().getColumnCount()];
        for (int column = 0; column < columns.length; column++) columns[column] = convertColumnIndexToView(column);
        return columns;
    }

    private void publishResults(SearchResult result, Match anchor, long generation, long stamp) {
        if (disposed || generation != searchGeneration.get() || !component.isVisible()) return;
        searching = false;
        searchTask = null;
        resultsStamp = stamp;
        matches = result.matches;
        if (result.invalidPattern) {
            component.setNotFoundBackground();
            setStatusText(FindBundle.message(INCORRECT_REGEXP_MESSAGE_KEY));
            component.updateActions();
            return;
        }
        for (int index = 0; index < matches.size(); index++) {
            if (matches.get(index).compareTo(anchor) >= 0) {
                current = index;
                break;
            }
        }
        if (current < 0 && hasMatches()) current = 0;
        selectCurrent();
    }

    private SearchResult collectMatches(FindModel query, int rowCount, int[] rows, int[] columns) {
        List<Match> found = new ArrayList<>();
        try {
            if (query.isRegularExpressions()) Pattern.compile(query.getStringToFind());
        } catch (PatternSyntaxException exception) {
            return new SearchResult(found, true);
        }
        PsiFile file = editor.getPsiFile();
        if (file == null) return new SearchResult(found, false);
        CsvEscapeCharacter escapeCharacter = CsvHelper.getEscapeCharacter(file);
        int row = 0;
        for (PsiElement child = file.getFirstChild(); child != null && !(child instanceof PsiErrorElement); child = child.getNextSibling()) {
            ProgressManager.checkCanceled();
            if (!(child instanceof CsvRecord)) continue;
            if (row >= rowCount) break;
            int viewRow = rows == null ? row : rows[row];
            row++;
            if (viewRow < 0) continue;
            int column = 0;
            for (PsiElement field = child.getFirstChild(); field != null && column < columns.length; field = field.getNextSibling()) {
                ProgressManager.checkCanceled();
                if (field instanceof CsvField || CsvHelper.isCommentElement(field)) {
                    int viewColumn = columns[column++];
                    if (viewColumn >= 0) {
                        collectCellMatches(CsvHelper.getFieldValue(field, escapeCharacter), viewRow, viewColumn, query, found);
                    }
                }
            }
        }
        found.sort(Match::compareTo);
        return new SearchResult(found, false);
    }

    private void collectCellMatches(String value, int row, int column, FindModel query, List<Match> found) {
        CharSequence text = new StringUtil.BombedCharSequence(value) {
            @Override
            protected void checkCanceled() {
                ProgressManager.checkCanceled();
            }
        };
        int offset = 0;
        while (offset <= value.length()) {
            ProgressManager.checkCanceled();
            FindResult result = findManager.findString(text, offset, query);
            if (!result.isStringFound()) break;
            found.add(new Match(row, column, result.getStartOffset(), result.getEndOffset()));
            offset = Math.max(result.getEndOffset(), result.getStartOffset() + 1);
        }
    }

    private void cancelSearch() {
        searchGeneration.incrementAndGet();
        if (searchTask != null) {
            searchTask.cancel();
            searchTask = null;
        }
        searching = false;
    }

    private void selectCurrent() {
        Match match = currentMatch();
        if (match != null) selectMatch(match);
        if (hasMatches()) {
            setStatusText(String.format(CsvPluginManager.getResourceBundle().getString("table.search.matches"),
                    current + 1, matches.size()));
        } else if (findModel.getStringToFind().isEmpty()) {
            setStatusText("");
        } else {
            component.setNotFoundBackground();
            setStatusText(CsvPluginManager.getResourceBundle().getString("table.search.no.matches"));
        }
        component.updateActions();
    }

    private void setStatusText(String text) {
        component.setStatusText(text);
        statusLabel.setText(text);
        statusLabel.setToolTipText(text);
        statusLabel.setForeground(component.getStatusColor());
    }

    private @Nullable Match getMatchAnchor() {
        if (disposed || replacing || pendingAnchor != null || !editor.isEditable() || editor.getTableModel().hasErrors() || !stopEditing()) return null;
        // Read the fields directly: their document-change notifications are delivered asynchronously.
        searchFieldDocumentChanged();
        replaceFieldDocumentChanged();
        return hasMatches() ? currentMatch() : null;
    }

    public void replaceCurrent() {
        Match anchor = getMatchAnchor();
        if (anchor == null) return;
        List<Match> targets = List.of(anchor);
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(editor);
        PreparedReplacements prepared;
        try {
            prepared = ReadAction.compute(() -> prepareReplacements(targets, updater));
            replacing = true;
            updater.replaceTexts(prepared.replacements);
            updater.commit();
        } catch (FindManager.MalformedReplacementStringException exception) {
            component.setNotFoundBackground();
            setStatusText(exception.getMessage());
            return;
        } finally {
            replacing = false;
            updater.dispose();
        }
        component.addTextToRecent(component.getSearchTextComponent());
        component.addTextToRecent(component.getReplaceTextComponent());
        // Skip the inserted text so replacing with a value containing the query still moves forward.
        pendingAnchor = new Match(anchor.row, anchor.column, prepared.nextOffset, 0);
        // Rebuild matches after the table model has invalidated its cached PSI, including delayed UI updates.
        editor.getTableModel().notifyUpdate();
    }

    public void replaceAll() {
        Match anchor = getMatchAnchor();
        if (anchor == null) return;
        PsiFile file = editor.getPsiFile();
        if (file == null || !document.isWritable()) return;
        CharSequence snapshot = document.getImmutableCharSequence();
        long stamp = document.getModificationStamp();
        long generation = searchGeneration.get();
        FindModel query = findModel.clone();
        List<Match> targets = matches;
        int[] rows = getViewRowIndices();
        int[] columns = getViewColumnIndices();
        CsvTableReplacement.Settings settings = CsvTableReplacement.Settings.capture(file);
        Ref<CsvTableReplacement.Change> change = Ref.create();
        Ref<FindManager.MalformedReplacementStringException> error = Ref.create();
        Ref<CsvTableModel> suspendedModel = Ref.create();
        Ref<Boolean> applied = Ref.create(false);
        replacing = true;
        component.updateActions();
        try {
            if (!runReplaceAllWithProgress(() -> {
                try {
                    change.set(CsvTableReplacement.prepare(snapshot, targets, rows, columns, query, findManager, settings));
                } catch (FindManager.MalformedReplacementStringException exception) {
                    error.set(exception);
                }
                ProgressManager.checkCanceled();
            })) return;
            if (!error.isNull()) {
                component.setNotFoundBackground();
                setStatusText(error.get().getMessage());
                return;
            }
            if (disposed || editor.isDisposed() || !component.isVisible()) return;
            CsvTableReplacement.Change prepared = change.get();
            WriteCommandAction.runWriteCommandAction(editor.getProject(), "CSV Editor Changes", null, () -> {
                if (!canApplyReplacement(file, stamp, generation, query, settings)) return;
                if (prepared != null) {
                    CsvTableModel model = editor.getTableModel();
                    model.suspend();
                    suspendedModel.set(model);
                    DocumentUtil.executeInBulk(document, () -> {
                        document.replaceString(prepared.range().getStartOffset(), prepared.range().getEndOffset(), prepared.text());
                        PsiDocumentManager.getInstance(Objects.requireNonNull(editor.getProject())).commitDocument(document);
                    });
                }
                applied.set(true);
            });
            if (!applied.get()) {
                NotificationGroupManager.getInstance().getNotificationGroup("net.seesharpsoft.intellij.plugins.csv")
                        .createNotification(CsvPluginManager.getLocalizedText("table.search.replace.changed"),
                                NotificationType.WARNING).notify(editor.getProject());
                return;
            }
            component.addTextToRecent(component.getSearchTextComponent());
            component.addTextToRecent(component.getReplaceTextComponent());
            pendingAnchor = anchor;
        } finally {
            replacing = false;
            if (!suspendedModel.isNull() && !editor.isDisposed()) suspendedModel.get().resume();
            if (!disposed && !editor.isDisposed() && component.isVisible()) {
                if (suspendedModel.isNull()) {
                    if (pendingAnchor != null) editor.getTableModel().notifyUpdate();
                    else if (resultsStamp != document.getModificationStamp() || generation != searchGeneration.get()) refresh(anchor);
                }
                component.updateActions();
            }
        }
    }

    private boolean canApplyReplacement(PsiFile file, long stamp, long generation, FindModel query,
                                        CsvTableReplacement.Settings settings) {
        return !disposed && !editor.isDisposed() && component.isVisible()
                && document.getModificationStamp() == stamp && generation == searchGeneration.get()
                && document.isWritable() && editor.isEditable() && file.isWritable()
                && editor.getPsiFile() == file && file.isValid() && settings.isCurrent(file)
                && query.getStringToFind().equals(component.getSearchTextComponent().getText())
                && query.getStringToReplace().equals(component.getReplaceTextComponent().getText());
    }

    protected boolean runReplaceAllWithProgress(Runnable operation) {
        return ProgressManager.getInstance().runProcessWithProgressSynchronously(
                operation, CsvPluginManager.getLocalizedText("table.search.replace.progress"), true, editor.getProject());
    }

    private PreparedReplacements prepareReplacements(List<Match> targets, CsvPsiTreeUpdater updater)
            throws FindManager.MalformedReplacementStringException {
        List<Pair<TextRange, String>> replacements = new ArrayList<>();
        int nextOffset = 0;
        CsvTableModel model = editor.getTableModel();
        for (int index = 0; index < targets.size();) {
            Match first = targets.get(index);
            int row = convertRowIndexToModel(first.row);
            int column = convertColumnIndexToModel(first.column);
            String value = model.getValue(row, column);
            StringBuilder result = new StringBuilder();
            int offset = 0;
            do {
                Match match = targets.get(index++);
                result.append(value, offset, match.start);
                String replacement = findManager.getStringToReplace(value.substring(match.start, match.end), findModel, match.start, value);
                result.append(replacement);
                nextOffset = match.start + replacement.length();
                if (match.start == match.end && replacement.isEmpty()) nextOffset++;
                offset = match.end;
            } while (index < targets.size() && targets.get(index).row == first.row && targets.get(index).column == first.column);
            result.append(value, offset, value.length());
            PsiElement field = model.getFieldAt(row, column);
            PsiElement replacement = model.isCommentRow(row) ? updater.createComment(result.toString())
                    : updater.createField(result.toString(), column == 0);
            if (field != null && replacement != null && !field.getText().equals(replacement.getText())) {
                replacements.add(Pair.create(field.getTextRange(), replacement.getText()));
            }
        }
        return new PreparedReplacements(replacements, nextOffset);
    }

    @Override
    public void close() {
        cancelSearch();
        clearMatchHighlight();
        component.setVisible(false);
        editor.getComponent().revalidate();
        editor.getComponent().repaint();
        JComponent focusComponent = editor.getPreferredFocusedComponent();
        if (focusComponent != null) focusComponent.requestFocusInWindow();
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        cancelSearch();
        clearMatchHighlight();
        matches = List.of();
    }

    protected record Match(int row, int column, int start, int end) implements Comparable<Match> {
        @Override
        public int compareTo(@NotNull Match other) {
            int comparison = Integer.compare(row, other.row);
            if (comparison == 0) comparison = Integer.compare(column, other.column);
            return comparison == 0 ? Integer.compare(start, other.start) : comparison;
        }
    }

    private record PreparedReplacements(List<Pair<TextRange, String>> replacements, int nextOffset) {
    }

    private record SearchResult(List<Match> matches, boolean invalidPattern) {
    }

    private final class ReplaceAction extends DumbAwareAction implements CustomComponentAction {
        private final Runnable operation;

        private ReplaceAction(boolean all) {
            super(ApplicationBundle.message(all ? "editorsearch.replace.all.action.text" : "editorsearch.replace.action.text"));
            operation = all ? CsvTableSearchSessionBase.this::replaceAll : CsvTableSearchSessionBase.this::replaceCurrent;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            operation.run();
        }

        @Override
        public @NotNull JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
            JButton button = new JButton(presentation.getText());
            button.addActionListener(event -> operation.run());
            return button;
        }

        @Override
        public void updateCustomComponent(@NotNull JComponent button, @NotNull Presentation presentation) {
            button.setEnabled(presentation.isEnabled());
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            event.getPresentation().setEnabled(hasMatches() && editor.isEditable() && !editor.getTableModel().hasErrors());
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }
}
