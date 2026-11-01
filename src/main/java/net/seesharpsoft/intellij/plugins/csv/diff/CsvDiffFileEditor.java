package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.DocumentsEditor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.CsvPluginManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.function.BooleanSupplier;

/**
 * Provides the original diff document to the platform's document-scoped undo/redo actions.
 */
final class CsvDiffFileEditor extends UserDataHolderBase implements DocumentsEditor {
    private final JComponent component;
    private final VirtualFile file;
    private Document[] documents;
    private final BooleanSupplier writable;
    private boolean disposed;

    CsvDiffFileEditor(JComponent component, DiffContent content, BooleanSupplier writable) {
        this.component = component;
        Document document = content instanceof DocumentContent documentContent ? documentContent.getDocument() : null;
        VirtualFile documentFile = document == null ? null : FileDocumentManager.getInstance().getFile(document);
        file = documentFile == null ? createFileIdentity(content) : documentFile;
        documents = document == null ? new Document[0] : new Document[]{document};
        this.writable = writable;
    }

    private static VirtualFile createFileIdentity(DiffContent content) {
        FileType fileType = content.getContentType();
        if (fileType == null && content instanceof DocumentContent document && document.getHighlightFile() != null) {
            fileType = document.getHighlightFile().getFileType();
        }
        if (fileType == null) fileType = CsvFileType.INSTANCE;
        // File-less and empty diff sides still need a stable identity for native editor state.
        return new LightVirtualFile("diff." + fileType.getDefaultExtension(), fileType, "");
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public Document @NotNull [] getDocuments() {
        return writable.getAsBoolean() ? documents.clone() : new Document[0];
    }

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return component;
    }

    @Override
    public @NotNull String getName() {
        return CsvPluginManager.getLocalizedText("table.diff");
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return !disposed;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void dispose() {
        disposed = true;
        documents = new Document[0];
    }
}
