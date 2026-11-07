package net.seesharpsoft.intellij.plugins.csv.table;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/** Document, PSI and model must describe the same CSV source throughout the host's lifetime. */
public interface CsvTableSearchHost extends PsiFileHolder {
    @Nullable
    Project getProject();

    @Nullable
    Document getDocument();

    CsvTableModel getTableModel();

    @NotNull
    JComponent getComponent();

    @Nullable
    JComponent getPreferredFocusedComponent();

    boolean isEditable();

    boolean isDisposed();
}
