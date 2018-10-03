package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

@State(
        name = "CsvEditorSettings",
        storages = {@Storage("csv-plugin.xml")}
)
@SuppressWarnings("all")
public class CsvEditorSettingsExternalizable implements PersistentStateComponent<CsvEditorSettingsExternalizable.OptionSet> {

    public enum EditorPrio {
        TEXT_FIRST,
        TABLE_FIRST,
        TEXT_ONLY
// enable when table editor is a real alternative
//        TABLE_ONLY
    }

    public static final class OptionSet {
        public boolean CARET_ROW_SHOWN;
        public boolean USE_SOFT_WRAP;
        public boolean COLUMN_HIGHTLIGHTING;
        public boolean HIGHTLIGHT_TAB_SEPARATOR;
        public boolean SHOW_INFO_BALLOON;
        public String TAB_HIGHLIGHT_COLOR;
        public EditorPrio EDITOR_PRIO;

        public boolean SHOW_TABLE_EDITOR_INFO_PANEL;

        public OptionSet() {
            EditorSettingsExternalizable editorSettingsExternalizable = EditorSettingsExternalizable.getInstance();
            CARET_ROW_SHOWN = editorSettingsExternalizable.isCaretRowShown();
            USE_SOFT_WRAP = editorSettingsExternalizable.isUseSoftWraps();
            COLUMN_HIGHTLIGHTING = false;
            HIGHTLIGHT_TAB_SEPARATOR = true;
            SHOW_INFO_BALLOON = true;
            TAB_HIGHLIGHT_COLOR = "-7984";
            EDITOR_PRIO = EditorPrio.TEXT_FIRST;
            SHOW_TABLE_EDITOR_INFO_PANEL = true;
        }
    }

    private OptionSet myOptions = new OptionSet();
    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    public CsvEditorSettingsExternalizable() {
    }

    public static CsvEditorSettingsExternalizable getInstance() {
        return ApplicationManager.getApplication().isDisposed() ? new CsvEditorSettingsExternalizable() : ServiceManager.getService(CsvEditorSettingsExternalizable.class);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.myPropertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.myPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public OptionSet getState() {
        return this.myOptions;
    }

    @Override
    public void loadState(@NotNull OptionSet state) {
        this.myOptions = state;
    }

    /*********** Settings section **********/

    public boolean isCaretRowShown() {
        return getState().CARET_ROW_SHOWN;
    }
    public void setCaretRowShown(boolean caretRowShown) {
        getState().CARET_ROW_SHOWN = caretRowShown;
    }

    public boolean isUseSoftWraps() {
        return getState().USE_SOFT_WRAP;
    }
    public void setUseSoftWraps(boolean useSoftWraps) {
        getState().USE_SOFT_WRAP = useSoftWraps;
    }

    public boolean isColumnHighlightingEnabled() {
        return getState().COLUMN_HIGHTLIGHTING;
    }
    public void setColumnHighlightingEnabled(boolean columnHighlightingEnabled) {
        getState().COLUMN_HIGHTLIGHTING = columnHighlightingEnabled;
    }

    public boolean isHighlightTabSeparator() {
        return getState().HIGHTLIGHT_TAB_SEPARATOR;
    }
    public void setHighlightTabSeparator(boolean highlightTabSeparator) {
        getState().HIGHTLIGHT_TAB_SEPARATOR = highlightTabSeparator;
    }

    public boolean isShowInfoBalloon() {
        return getState().SHOW_INFO_BALLOON;
    }
    public void setShowInfoBalloon(boolean showInfoBalloon) {
        getState().SHOW_INFO_BALLOON = showInfoBalloon;
    }

    public Color getTabHighlightColor() {
        String color = getState().TAB_HIGHLIGHT_COLOR;
        try {
            return color == null || color.isEmpty() ? null : Color.decode(getState().TAB_HIGHLIGHT_COLOR);
        } catch (NumberFormatException exc) {
            return null;
        }
    }
    public void setTabHighlightColor(Color color) {
        getState().TAB_HIGHLIGHT_COLOR = color == null ? "" : "" + color.getRGB();
    }

    public EditorPrio getEditorPrio() {
        return getState().EDITOR_PRIO;
    }
    public void setEditorPrio(EditorPrio editorPrio) {
        getState().EDITOR_PRIO = editorPrio;
    }

    public boolean showTableEditorInfoPanel() {
        return getState().SHOW_TABLE_EDITOR_INFO_PANEL;
    }
    public void showTableEditorInfoPanel(boolean showInfoPanel) {
        getState().SHOW_TABLE_EDITOR_INFO_PANEL = showInfoPanel;
    }
}