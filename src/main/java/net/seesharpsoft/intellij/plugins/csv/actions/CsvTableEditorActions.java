package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableActions;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableEditorSwing;
import org.jetbrains.annotations.NotNull;

public abstract class CsvTableEditorActions extends AnAction implements ActionUpdateThreadAware {

    public static class FreezeColumns extends CsvTableEditorActions {
        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            if (getTableEditor(event) instanceof CsvTableEditorSwing editor) {
                editor.freezeColumnsThroughSelection();
            }
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            event.getPresentation().setEnabled(getTableEditor(event) instanceof CsvTableEditorSwing editor && editor.canFreezeColumns());
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    public static class UnfreezeColumns extends CsvTableEditorActions {
        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            if (getTableEditor(event) instanceof CsvTableEditorSwing editor) {
                editor.setFrozenColumnCount(0);
            }
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            event.getPresentation().setEnabled(getTableEditor(event) instanceof CsvTableEditorSwing editor && editor.getFrozenColumnCount() > 0);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    public static class AddRowBefore extends CsvTableEditorActions {
        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            getTableActions(anActionEvent).addRow(getTableEditor(anActionEvent), true);
        }
    }

    public static class AddRowAfter extends CsvTableEditorActions {
        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            getTableActions(anActionEvent).addRow(getTableEditor(anActionEvent), false);
        }
    }

    public static class AddColumnBefore extends CsvTableEditorActions {
        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            getTableActions(anActionEvent).addColumn(getTableEditor(anActionEvent), true);
        }
    }

    public static class AddColumnAfter extends CsvTableEditorActions {
        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            getTableActions(anActionEvent).addColumn(getTableEditor(anActionEvent), false);
        }
    }

    public static class DeleteSelectedRows extends CsvTableEditorActions {
        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            getTableActions(anActionEvent).deleteSelectedRows(getTableEditor(anActionEvent));
        }
    }

    public static class DeleteSelectedColumns extends CsvTableEditorActions {
        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            getTableActions(anActionEvent).deleteSelectedColumns(getTableEditor(anActionEvent));
        }
    }

    public static class AdjustColumnWidths extends CsvTableEditorActions {
        @Override
        protected boolean requiresEditableTable() {
            return false;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            getTableActions(anActionEvent).adjustColumnWidths(getTableEditor(anActionEvent));
        }
    }

    public static class ResetColumnWidths extends CsvTableEditorActions {
        @Override
        protected boolean requiresEditableTable() {
            return false;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            getTableActions(anActionEvent).resetColumnWidths(getTableEditor(anActionEvent));
        }
    }

    public static CsvTableEditor getTableEditor(@NotNull AnActionEvent anActionEvent) {
        FileEditor fileEditor = anActionEvent.getData(PlatformDataKeys.FILE_EDITOR);
        if (!(fileEditor instanceof CsvTableEditor)) {
            return null;
        }
        return (CsvTableEditor) fileEditor;
    }

    public static CsvTableActions getTableActions(@NotNull AnActionEvent anActionEvent) {
        CsvTableEditor tableEditor = getTableEditor(anActionEvent);
        return tableEditor == null ? CsvTableActions.DUMMY : tableEditor.getActions();
    }

    protected boolean requiresEditableTable() {
        return true;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        CsvTableEditor editor = getTableEditor(event);
        event.getPresentation().setEnabled(editor != null && (!requiresEditableTable() || editor.isEditable()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
