package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import com.intellij.util.ArrayUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvSeparatorHolder;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

@SuppressWarnings({"checkstyle:membername", "checkstyle:visibilitymodifier"})
public class CsvCodeStyleSettings extends CustomCodeStyleSettings {

    public boolean SPACE_BEFORE_SEPARATOR = false;
    public boolean SPACE_AFTER_SEPARATOR = false;
    public boolean TRIM_LEADING_WHITE_SPACES = false;
    public boolean TRIM_TRAILING_WHITE_SPACES = false;
    public boolean TABULARIZE = true;
    public boolean WHITE_SPACES_OUTSIDE_QUOTES = true;
    public boolean LEADING_WHITE_SPACES = false;
    public int SEPARATOR_INDEX = 0;
    public boolean ENABLE_WIDE_CHARACTER_DETECTION = false;
    public boolean TREAT_AMBIGUOUS_CHARACTERS_AS_WIDE = false;

    public static final String DEFAULT_SEPARATOR = ",";
    public static final String TAB_SEPARATOR = "\t";
    public static final String[] SUPPORTED_SEPARATORS = new String[]{",", ";", "|", TAB_SEPARATOR};
    public static final String[] SUPPORTED_SEPARATORS_DISPLAY = new String[]{"Comma (,)", "Semicolon (;)", "Pipe (|)", "Tab (↹)"};
    public static final Pattern REPLACE_DEFAULT_SEPARATOR_PATTERN = Pattern.compile(CsvCodeStyleSettings.DEFAULT_SEPARATOR);

    public static String getCurrentSeparator(CodeStyleSettings codeStyleSettings) {
        if (codeStyleSettings != null) {
            CsvCodeStyleSettings csvCodeStyleSettings = codeStyleSettings.getCustomSettings(CsvCodeStyleSettings.class);
            if (csvCodeStyleSettings != null) {
                return csvCodeStyleSettings.getSeparator();
            }
        }
        return DEFAULT_SEPARATOR;
    }

    public static String getCurrentSeparator(@Nullable Project project) {
        if (!ApplicationManager.getApplication().isUnitTestMode() && project != null) {
            return getCurrentSeparator(CodeStyleSettingsManager.getInstance(project).getCurrentSettings());
        }
        return DEFAULT_SEPARATOR;
    }

    public static String getCurrentSeparator(@Nullable Project project, @Nullable Language language) {
        if (language != null && language instanceof CsvSeparatorHolder) {
            return ((CsvSeparatorHolder)language).getSeparator();
        }
        return getCurrentSeparator(project);
    }

    public static String getCurrentSeparator(@Nullable PsiFile psiFile) {
        if (psiFile == null) {
            return getCurrentSeparator((Project)null);
        }
        Project project = psiFile.getProject();
        VirtualFile virtualFile = psiFile.getVirtualFile();
        CsvFileAttributes csvFileAttributes = project != null && virtualFile != null ? ServiceManager.getService(project, CsvFileAttributes.class) : null;
        String separator = csvFileAttributes != null ? csvFileAttributes.getFileSeparator(virtualFile) : null;
        return separator != null ? separator : getCurrentSeparator(project, psiFile.getLanguage());
    }

    public static String getSeparatorDisplayText(String separator) {
        return SUPPORTED_SEPARATORS_DISPLAY[ArrayUtil.find(SUPPORTED_SEPARATORS, separator)];
    }

    public CsvCodeStyleSettings(CodeStyleSettings settings) {
        super("CsvCodeStyleSettings", settings);
    }

    public String getSeparator() {
        if (SEPARATOR_INDEX < 0 || SEPARATOR_INDEX >= SUPPORTED_SEPARATORS.length) {
            SEPARATOR_INDEX = 0;
        }
        return SUPPORTED_SEPARATORS[SEPARATOR_INDEX];
    }
}
