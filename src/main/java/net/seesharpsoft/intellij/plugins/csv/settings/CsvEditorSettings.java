package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import net.seesharpsoft.intellij.plugins.csv.CsvStorageHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.regex.Pattern;

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

    public static final EscapeCharacter ESCAPE_CHARACTER_DEFAULT = EscapeCharacter.QUOTE;
    public static final ValueSeparator VALUE_SEPARATOR_DEFAULT = ValueSeparator.COMMA;

    public enum ValueSeparator {
        COMMA(",", "Comma (,)"),
        SEMICOLON(";", "Semicolon (;)"),
        PIPE("|", "Pipe (|)"),
        TAB("\t", "Tab (↹)");

        private final String myCharacter;
        private final String myDisplay;
        private final Pattern myPattern;

        private ValueSeparator(String character, String display) {
            myCharacter = character;
            myDisplay = display;
            myPattern = Pattern.compile(Pattern.quote(myCharacter));
        }

        public String getCharacter() {
            return myCharacter;
        }

        public String getDisplay() {
            return myDisplay;
        }

        public boolean isValueSeparator(String text) {
            return myPattern.matcher(text).matches();
        }
    }

    public enum EscapeCharacter {
        QUOTE("\"", "Double Quote (\")"),
        BACKSLASH("\\", "Backslash (\\)");

        private final String myCharacter;
        private final String myDisplay;
        private final Pattern myPattern;

        private EscapeCharacter(String character, String display) {
            myCharacter = character;
            myDisplay = display;
            myPattern = Pattern.compile(Pattern.quote(myCharacter + "\""));
        }

        public String getCharacter() {
            return myCharacter;
        }

        public String getDisplay() {
            return myDisplay;
        }

        public boolean isEscapedQuote(String text) {
            return myPattern.matcher(text).matches();
        }
    }

    public enum EditorPrio {
        TEXT_FIRST,
        TABLE_FIRST,
        TEXT_ONLY
    }

    public static final class OptionSet {
        public boolean CARET_ROW_SHOWN;
        public boolean USE_SOFT_WRAP;
        public boolean COLUMN_HIGHTLIGHTING;
        public boolean HIGHTLIGHT_TAB_SEPARATOR;
        public boolean SHOW_INFO_BALLOON;
        public String TAB_HIGHLIGHT_COLOR;
        public EditorPrio EDITOR_PRIO;
        public int TABLE_EDITOR_ROW_HEIGHT;
        public int TABLE_AUTO_MAX_COLUMN_WIDTH;
        public int TABLE_DEFAULT_COLUMN_WIDTH;
        public boolean TABLE_AUTO_COLUMN_WIDTH_ON_OPEN;
        public boolean TABLE_COLUMN_HIGHTLIGHTING;
        public boolean ZERO_BASED_COLUMN_NUMBERING;

        public boolean SHOW_TABLE_EDITOR_INFO_PANEL;
        public boolean QUOTING_ENFORCED;
        public boolean FILE_END_LINE_BREAK;
        public EscapeCharacter DEFAULT_ESCAPE_CHARACTER = ESCAPE_CHARACTER_DEFAULT;
        public ValueSeparator DEFAULT_VALUE_SEPARATOR = VALUE_SEPARATOR_DEFAULT;

        public OptionSet() {
            EditorSettingsExternalizable editorSettingsExternalizable = EditorSettingsExternalizable.getInstance();
            CARET_ROW_SHOWN = editorSettingsExternalizable == null ? true : editorSettingsExternalizable.isCaretRowShown();
            USE_SOFT_WRAP = editorSettingsExternalizable == null ? false : editorSettingsExternalizable.isUseSoftWraps();
            COLUMN_HIGHTLIGHTING = true;
            HIGHTLIGHT_TAB_SEPARATOR = true;
            SHOW_INFO_BALLOON = true;
            TAB_HIGHLIGHT_COLOR = "-7984";
            EDITOR_PRIO = EditorPrio.TEXT_FIRST;
            SHOW_TABLE_EDITOR_INFO_PANEL = true;
            TABLE_EDITOR_ROW_HEIGHT = TABLE_EDITOR_ROW_HEIGHT_DEFAULT;
            TABLE_AUTO_MAX_COLUMN_WIDTH = TABLE_AUTO_MAX_COLUMN_WIDTH_DEFAULT;
            TABLE_DEFAULT_COLUMN_WIDTH = TABLE_DEFAULT_COLUMN_WIDTH_DEFAULT;
            TABLE_AUTO_COLUMN_WIDTH_ON_OPEN = false;
            QUOTING_ENFORCED = false;
            TABLE_COLUMN_HIGHTLIGHTING = true;
            ZERO_BASED_COLUMN_NUMBERING = false;
            FILE_END_LINE_BREAK = true;
        }
    }

    private OptionSet myOptions = new OptionSet();
    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    public CsvEditorSettings() {
    }

    public static CsvEditorSettings getInstance() {
        CsvEditorSettings instance = ServiceManager.getService(CsvEditorSettings.class);
        return instance == null ? new CsvEditorSettings() : instance;
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

    public boolean isTableColumnHighlightingEnabled() {
        return getState().TABLE_COLUMN_HIGHTLIGHTING;
    }

    public void setTableColumnHighlightingEnabled(boolean columnHighlightingEnabled) {
        getState().TABLE_COLUMN_HIGHTLIGHTING = columnHighlightingEnabled;
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

    public void setDefaultEscapeCharacter(EscapeCharacter defaultEscapeCharacter) {
        getState().DEFAULT_ESCAPE_CHARACTER = defaultEscapeCharacter;
    }

    public EscapeCharacter getDefaultEscapeCharacter() {
        return getState().DEFAULT_ESCAPE_CHARACTER;
    }

    public void setDefaultValueSeparator(ValueSeparator defaultValueSeparator) {
        getState().DEFAULT_VALUE_SEPARATOR = defaultValueSeparator;
    }

    public ValueSeparator getDefaultValueSeparator() {
        return getState().DEFAULT_VALUE_SEPARATOR;
    }
}
