package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.ResourceBundle;

import static net.seesharpsoft.intellij.plugins.csv.CsvPluginManager.getResourceBundle;

public class CsvLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
    @NotNull
    @Override
    public Language getLanguage() {
        return CsvLanguage.INSTANCE;
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        ResourceBundle bundle = getResourceBundle();
        if (settingsType == SettingsType.LANGUAGE_SPECIFIC) {
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "SPACE_BEFORE_SEPARATOR",
                    bundle.getString("group.separator.space.before"),
                    bundle.getString("group.separator"));
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "SPACE_AFTER_SEPARATOR",
                    bundle.getString("group.separator.space.after"),
                    bundle.getString("group.separator"));

            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "TABULARIZE",
                    bundle.getString("group.tabularize.as.table"),
                    bundle.getString("group.tabularize"));
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "WHITE_SPACES_OUTSIDE_QUOTES",
                    bundle.getString("group.tabularize.keep.quoted"),
                    bundle.getString("group.tabularize"));
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "LEADING_WHITE_SPACES",
                    bundle.getString("group.tabularize.align.right"),
                    bundle.getString("group.tabularize"));
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "ENABLE_WIDE_CHARACTER_DETECTION",
                    bundle.getString("group.tabularize.enhanced.width.calculation"),
                    bundle.getString("group.tabularize"));
//            consumer.showCustomOption(CsvCodeStyleSettings.class,
//                    "TREAT_AMBIGUOUS_CHARACTERS_AS_WIDE",
//                    "Ambiguous as wide characters",
//                    bundle.getString("group.tabularize"));

            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "TRIM_LEADING_WHITE_SPACES",
                    bundle.getString("group.trimming.leading"),
                    bundle.getString("group.trimming"));
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "TRIM_TRAILING_WHITE_SPACES",
                    bundle.getString("group.trimming.trailing"),
                    bundle.getString("group.trimming"));
        } else if (settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS) {
            consumer.showStandardOptions(
                    CodeStyleSettingsCustomizable.WrappingOrBraceOption.WRAP_LONG_LINES.name(),
                    CodeStyleSettingsCustomizable.WrappingOrBraceOption.WRAP_ON_TYPING.name()
            );
        } else if (settingsType == SettingsType.INDENT_SETTINGS) {
            consumer.showStandardOptions(
                    CodeStyleSettingsCustomizable.IndentOption.SMART_TABS.name(),
                    CodeStyleSettingsCustomizable.IndentOption.USE_TAB_CHARACTER.name(),
                    CodeStyleSettingsCustomizable.IndentOption.TAB_SIZE.name(),
                    CodeStyleSettingsCustomizable.IndentOption.KEEP_INDENTS_ON_EMPTY_LINES.name()
            );
        }
    }

    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        return """
            ID,Name,Age,Misc
            ,,,
              1,  "  Mike ""Mute"" Masters",  29, spaces front
            2  ,"Mustermann, Max   "  ,23 ,spaces after  \s
            3,Ally Allison,48,no space & no quote
             42 ,  "  Berta  Boston  "  ,  75  ,  spaces everywhere
              169 ,  Charlie   Chaplin  ,  33  ,  spaces everywhere & no quote
            200,汉字宋,00,char test
            """;
    }

    protected void customizeDefaults(@NotNull CommonCodeStyleSettings commonSettings, @NotNull CommonCodeStyleSettings.IndentOptions indentOptions) {
        super.customizeDefaults(commonSettings, indentOptions);
        indentOptions.USE_TAB_CHARACTER = true;
        indentOptions.SMART_TABS = false;
        indentOptions.KEEP_INDENTS_ON_EMPTY_LINES = true;
        commonSettings.WRAP_ON_TYPING = CommonCodeStyleSettings.WrapOnTyping.NO_WRAP.intValue;
        commonSettings.WRAP_LONG_LINES = false;
    }
}
