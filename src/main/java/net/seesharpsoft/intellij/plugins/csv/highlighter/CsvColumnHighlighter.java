package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.find.EditorSearchSession;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.CancellablePromise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

// The IDE's identifier cache does not account for selection or Find/Replace state.
public final class CsvColumnHighlighter implements Disposable {
    private final Project project;
    private final Editor editor;
    private final List<RangeHighlighter> highlights = new ArrayList<>();
    private CancellablePromise<?> task;
    private final AtomicLong generation = new AtomicLong();
    private volatile boolean disposed;

    public static void install(Project project, TextEditor textEditor) {
        CsvColumnHighlighter highlighter = new CsvColumnHighlighter(project, textEditor.getEditor());
        Disposer.register(textEditor, highlighter);
    }

    private CsvColumnHighlighter(Project project, Editor editor) {
        this.project = project;
        this.editor = editor;
        editor.getSelectionModel().addSelectionListener(new SelectionListener() {
            @Override
            public void selectionChanged(@NotNull SelectionEvent event) {
                update();
            }
        }, this);
        editor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                if (event.getCaret() == editor.getCaretModel().getPrimaryCaret()) {
                    update();
                }
            }
        }, this);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                requestUpdate();
            }
        }, this);
        if (editor instanceof EditorEx editorEx) {
            editorEx.addPropertyChangeListener(event -> {
                if (EditorEx.PROP_HEADER_COMPONENT.equals(event.getPropertyName())) {
                    requestUpdate();
                }
            }, this);
        }
        update();
    }

    private void requestUpdate() {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            update();
        } else {
            ApplicationManager.getApplication().invokeLater(this::update, ModalityState.any(), ignored -> disposed);
        }
    }

    private void update() {
        cancelTask();
        if (disposed || project.isDisposed() || editor.isDisposed()) return;
        int maxRows = CsvEditorSettings.getInstance().getMaxHighlightingRows();
        if (EditorSearchSession.get(editor) != null || !shouldHighlight() || maxRows == 0) {
            clearHighlights();
            return;
        }

        long request = generation.get();
        long stamp = editor.getDocument().getModificationStamp();
        task = ReadAction.nonBlocking(() -> collectRanges(maxRows))
                .inSmartMode(project)
                .withDocumentsCommitted(project)
                .expireWith(this)
                .expireWhen(() -> disposed || request != generation.get()
                        || stamp != editor.getDocument().getModificationStamp()
                        || maxRows != CsvEditorSettings.getInstance().getMaxHighlightingRows())
                .coalesceBy(this)
                .finishOnUiThread(ModalityState.any(), ranges -> publishRanges(ranges, request, stamp, maxRows))
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private boolean shouldHighlight() {
        return CsvEditorSettings.getInstance().isHighlightActiveColumn()
                && editor.getSelectionModel().hasSelection()
                && UIUtil.isShowing(editor.getContentComponent());
    }

    private List<TextRange> collectRanges(int maxRows) {
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (!(file instanceof CsvFile csvFile) || EditorSearchSession.get(editor) != null
                || !editor.getSelectionModel().hasSelection()) return List.of();

        PsiElement field = getFocusedField(csvFile);
        if (field == null) return List.of();
        int index = CsvHelper.getFieldIndex(field);
        if (index == -1 || !(field.getParent() instanceof CsvRecord record)) return List.of();

        return collectColumnRanges(record, index, maxRows);
    }

    private PsiElement getFocusedField(CsvFile file) {
        int offset = editor.getCaretModel().getPrimaryCaret().getOffset();
        PsiElement element = file.getViewProvider().findElementAt(offset);
        return CsvHelper.getParentFieldElement(element == null ? file.getLastChild() : element);
    }

    private List<TextRange> collectColumnRanges(CsvRecord record, int index, int maxRows) {
        List<TextRange> ranges = new ArrayList<>();
        CsvRecord next = record;
        CsvRecord previous = PsiTreeUtil.getPrevSiblingOfType(record, CsvRecord.class);
        int remaining = maxRows;
        while (remaining != 0 && (next != null || previous != null)) {
            if (next != null) {
                addColumnRange(next, index, ranges);
                next = PsiTreeUtil.getNextSiblingOfType(next, CsvRecord.class);
                if (remaining > 0) remaining--;
            }
            if (previous != null && remaining != 0) {
                addColumnRange(previous, index, ranges);
                previous = PsiTreeUtil.getPrevSiblingOfType(previous, CsvRecord.class);
                if (remaining > 0) remaining--;
            }
        }
        return ranges;
    }

    private void addColumnRange(CsvRecord record, int index, List<TextRange> ranges) {
        ProgressManager.checkCanceled();
        PsiElement field = PsiHelper.getNthChildOfType(record, index, CsvField.class);
        if (field != null) {
            TextRange range = field.getTextRange();
            if (range != null && !range.isEmpty()) {
                ranges.add(range);
            }
        }
    }

    private void publishRanges(List<TextRange> ranges, long request, long stamp, int maxRows) {
        if (disposed || request != generation.get() || project.isDisposed() || editor.isDisposed()
                || stamp != editor.getDocument().getModificationStamp()
                || maxRows != CsvEditorSettings.getInstance().getMaxHighlightingRows()
                || EditorSearchSession.get(editor) != null || !shouldHighlight()) return;
        task = null;
        clearHighlights();
        for (TextRange range : ranges) {
            highlights.add(editor.getMarkupModel().addRangeHighlighter(
                    EditorColors.WRITE_IDENTIFIER_UNDER_CARET_ATTRIBUTES,
                    range.getStartOffset(), range.getEndOffset(),
                    HighlighterLayer.ELEMENT_UNDER_CARET, HighlighterTargetArea.EXACT_RANGE));
        }
    }

    private void cancelTask() {
        generation.incrementAndGet();
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void clearHighlights() {
        for (RangeHighlighter highlight : highlights) {
            highlight.dispose();
        }
        highlights.clear();
    }

    @Override
    public void dispose() {
        disposed = true;
        cancelTask();
        clearHighlights();
    }
}
