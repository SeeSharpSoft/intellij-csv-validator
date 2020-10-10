package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.util.xmlb.annotations.OptionTag;
import net.seesharpsoft.intellij.plugins.csv.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

@State(
        name = "CsvEditorSettings",
        storages = {@Storage(CsvStorageHelper.CSV_STATE_STORAGE_FILE)}
)
@SuppressWarnings("all")
public class CsvEditorSettings implements PersistentStateComponent<CsvEditorSettings.OptionSet> {

    public static final int TABLE_EDITOR_ROW_HEIGHT_MIN = 0;
    public static final int TABLE_EDITOR_ROW_HEIGHT_MAX = 10;
    public static final int TABLE_EDITOR_ROW_HEIGHT_DEFAULT = 3;
    public static final int TABLE_AUTO_MAX_COLUMN_WIDTH_DEFAULT = 300;
    public static final int TABLE_DEFAULT_COLUMN_WIDTH_DEFAULT = 100;

    public static final CsvEscapeCharacter ESCAPE_CHARACTER_DEFAULT = CsvEscapeCharacter.QUOTE;
    public static final CsvValueSeparator VALUE_SEPARATOR_DEFAULT = CsvValueSeparator.COMMA;

    public static final String COMMENT_INDICATOR_DEFAULT = "#";

    // only required for testing
    private static final CsvEditorSettings STATIC_TEST_INSTANCE = new CsvEditorSettings();

    public enum EditorPrio {
        TEXT_FIRST,
        TABLE_FIRST,
        TEXT_ONLY
    }

    public enum ValueColoring {
        RAINBOW("Rainbow (Column Color)"),
        SIMPLE("Simple (Text Color)");

        private final String display;

        ValueColoring(String displayArg) {
            this.display = displayArg;
        }

        public String getDisplay() {
            return this.display;
        }
    }

    public static final class OptionSet {
        public String CURRENT_PLUGIN_VERSION;

        public boolean CARET_ROW_SHOWN;
        public boolean USE_SOFT_WRAP;
        public boolean HIGHTLIGHT_TAB_SEPARATOR = true;
        public boolean SHOW_INFO_BALLOON = true;
        public String TAB_HIGHLIGHT_COLOR = "-7984";
        public EditorPrio EDITOR_PRIO = EditorPrio.TEXT_FIRST;
        public int TABLE_EDITOR_ROW_HEIGHT = TABLE_EDITOR_ROW_HEIGHT_DEFAULT;
        public int TABLE_AUTO_MAX_COLUMN_WIDTH = TABLE_AUTO_MAX_COLUMN_WIDTH_DEFAULT;
        public int TABLE_DEFAULT_COLUMN_WIDTH = TABLE_DEFAULT_COLUMN_WIDTH_DEFAULT;
        public boolean TABLE_AUTO_COLUMN_WIDTH_ON_OPEN = false;
        public boolean ZERO_BASED_COLUMN_NUMBERING = false;
        public boolean TABLE_HEADER_ROW_FIXED = true;

        public boolean SHOW_TABLE_EDITOR_INFO_PANEL = true;
        public boolean QUOTING_ENFORCED = false;
        public boolean FILE_END_LINE_BREAK = true;
        @OptionTag(converter = CsvEscapeCharacter.CsvEscapeCharacterConverter.class)
        public CsvEscapeCharacter DEFAULT_ESCAPE_CHARACTER = ESCAPE_CHARACTER_DEFAULT;
        @OptionTag(converter = CsvValueSeparator.CsvValueSeparatorConverter.class)
        public CsvValueSeparator DEFAULT_VALUE_SEPARATOR = VALUE_SEPARATOR_DEFAULT;
        public boolean KEEP_TRAILING_SPACES = false;
        public String COMMENT_INDICATOR = COMMENT_INDICATOR_DEFAULT;
        public ValueColoring VALUE_COLORING = ValueColoring.RAINBOW;

        public OptionSet() {
            EditorSettingsExternalizable editorSettingsExternalizable = EditorSettingsExternalizable.getInstance();
            CARET_ROW_SHOWN = editorSettingsExternalizable == null ? true : editorSettingsExternalizable.isCaretRowShown();
            USE_SOFT_WRAP = editorSettingsExternalizable == null ? false : editorSettingsExternalizable.isUseSoftWraps();
        }
    }

    private OptionSet myOptions = new OptionSet();
    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    public CsvEditorSettings() {
    }

    public static CsvEditorSettings getInstance() {
        Application application = ApplicationManager.getApplication();
        if (application.isUnitTestMode()) {
            return CsvEditorSettings.STATIC_TEST_INSTANCE;
        }
        return application.isDisposed() ? new CsvEditorSettings() :  ServiceManager.getService(CsvEditorSettings.class);
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

    public int getTableEditorRowHeight() {
        return getState().TABLE_EDITOR_ROW_HEIGHT;
    }

    public void setTableEditorRowHeight(int rowHeight) {
        int finalRowHeight = rowHeight;
        if (finalRowHeight > TABLE_EDITOR_ROW_HEIGHT_MAX) finalRowHeight = TABLE_EDITOR_ROW_HEIGHT_MAX;
        if (finalRowHeight < TABLE_EDITOR_ROW_HEIGHT_MIN) finalRowHeight = TABLE_EDITOR_ROW_HEIGHT_MIN;
        getState().TABLE_EDITOR_ROW_HEIGHT = finalRowHeight;
    }

    public boolean isQuotingEnforced() {
        return getState().QUOTING_ENFORCED;
    }

    public void setQuotingEnforced(boolean quotingEnforced) {
        getState().QUOTING_ENFORCED = quotingEnforced;
    }

    public boolean isZeroBasedColumnNumbering() {
        return getState().ZERO_BASED_COLUMN_NUMBERING;
    }

    public void setZeroBasedColumnNumbering(boolean zeroBasedColumnNumbering) {
        getState().ZERO_BASED_COLUMN_NUMBERING = zeroBasedColumnNumbering;
    }

    public boolean isFileEndLineBreak() {
        return getState().FILE_END_LINE_BREAK;
    }

    public void setFileEndLineBreak(boolean fileEndLineBreak) {
        getState().FILE_END_LINE_BREAK = fileEndLineBreak;
    }

    public int getTableAutoMaxColumnWidth() {
        return getState().TABLE_AUTO_MAX_COLUMN_WIDTH;
    }

    public void setTableAutoMaxColumnWidth(int tableAutoMaxColumnWidth) {
        getState().TABLE_AUTO_MAX_COLUMN_WIDTH = tableAutoMaxColumnWidth;
    }

    public int getTableDefaultColumnWidth() {
        return getState().TABLE_DEFAULT_COLUMN_WIDTH;
    }

    public void setTableDefaultColumnWidth(int tableDefaultColumnWidth) {
        getState().TABLE_DEFAULT_COLUMN_WIDTH = tableDefaultColumnWidth;
    }

    public boolean isTableAutoColumnWidthOnOpen() {
        return getState().TABLE_AUTO_COLUMN_WIDTH_ON_OPEN;
    }

    public void setTableAutoColumnWidthOnOpen(boolean tableAutoColumnWidthOnOpen) {
        getState().TABLE_AUTO_COLUMN_WIDTH_ON_OPEN = tableAutoColumnWidthOnOpen;
    }

    public void setDefaultEscapeCharacter(CsvEscapeCharacter defaultEscapeCharacter) {
        CsvEscapeCharacter oldValue = getDefaultEscapeCharacter();
        getState().DEFAULT_ESCAPE_CHARACTER = defaultEscapeCharacter;
        if (!Objects.equals(oldValue, defaultEscapeCharacter)) {
            myPropertyChangeSupport.firePropertyChange("defaultEscapeCharacter", oldValue, defaultEscapeCharacter);
        }
    }

    public CsvEscapeCharacter getDefaultEscapeCharacter() {
        CsvEscapeCharacter csvValueSeparator = getState().DEFAULT_ESCAPE_CHARACTER;
        return csvValueSeparator == null ? ESCAPE_CHARACTER_DEFAULT : getState().DEFAULT_ESCAPE_CHARACTER;
    }

    public void setDefaultValueSeparator(CsvValueSeparator defaultValueSeparator) {
        CsvValueSeparator oldValue = getDefaultValueSeparator();
        getState().DEFAULT_VALUE_SEPARATOR = defaultValueSeparator;
        if (!Objects.equals(oldValue, defaultValueSeparator)) {
            myPropertyChangeSupport.firePropertyChange("defaultValueSeparator", oldValue, defaultValueSeparator);
        }
    }

    public CsvValueSeparator getDefaultValueSeparator() {
        CsvValueSeparator csvValueSeparator = getState().DEFAULT_VALUE_SEPARATOR;
        return csvValueSeparator == null ? VALUE_SEPARATOR_DEFAULT : csvValueSeparator;
    }

    public void setKeepTrailingSpaces(boolean keepTrailingSpaces) {
        getState().KEEP_TRAILING_SPACES = keepTrailingSpaces;
    }

    public boolean getKeepTrailingSpaces() {
        return getState().KEEP_TRAILING_SPACES;
    }

    public void setCommentIndicator(String commentIndicator) {
        String oldValue = getCommentIndicator();
        getState().COMMENT_INDICATOR = commentIndicator.trim();
        if (commentIndicator != oldValue) {
            myPropertyChangeSupport.firePropertyChange("commentIndicator", oldValue, getCommentIndicator());
        }
    }

    public String getCommentIndicator() {
        return getState().COMMENT_INDICATOR;
    }

    public ValueColoring getValueColoring() {
        return getState().VALUE_COLORING;
    }

    public void setValueColoring(ValueColoring valueColoring) {
        ValueColoring oldValue = getValueColoring();
        getState().VALUE_COLORING = valueColoring;
        if (valueColoring != oldValue) {
            myPropertyChangeSupport.firePropertyChange("valueColoring", oldValue, getValueColoring());
        }
    }

    public boolean isHeaderRowFixed() {
        return getState().TABLE_HEADER_ROW_FIXED;
    }

    public void setHeaderRowFixed(boolean headerRowFixed) {
        getState().TABLE_HEADER_ROW_FIXED = headerRowFixed;
    }

    public boolean checkCurrentPluginVersion(String actualVersion) {
        if (!actualVersion.equals(getState().CURRENT_PLUGIN_VERSION)) {
            getState().CURRENT_PLUGIN_VERSION = actualVersion;
            return false;
        }
        return true;
    }
}
