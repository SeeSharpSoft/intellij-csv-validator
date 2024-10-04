package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;

public class CsvLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
    @NotNull
    @Override
    public Language getLanguage() {
        return CsvLanguage.INSTANCE;
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        if (settingsType == SettingsType.LANGUAGE_SPECIFIC) {
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "SPACE_BEFORE_SEPARATOR",
                    "Space before separator",
                    "Separator");
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "SPACE_AFTER_SEPARATOR",
                    "Space after separator",
                    "Separator");

            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "TABULARIZE",
                    "Format as table",
                    "Tabularize");
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "WHITE_SPACES_OUTSIDE_QUOTES",
                    "Keep quoted value as is",
                    "Tabularize");
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "LEADING_WHITE_SPACES",
                    "Align right",
                    "Tabularize");
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "ENABLE_WIDE_CHARACTER_DETECTION",
                    "Enhanced width calculation (slower)",
                    "Tabularize");
//            consumer.showCustomOption(CsvCodeStyleSettings.class,
//                    "TREAT_AMBIGUOUS_CHARACTERS_AS_WIDE",
//                    "Ambiguous as wide characters",
//                    "Tabularize");

            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "TRIM_LEADING_WHITE_SPACES",
                    "Trim leading whitespaces",
                    "Trimming (only if not tabularized)");
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "TRIM_TRAILING_WHITE_SPACES",
                    "Trim trailing whitespaces",
                    "Trimming (only if not tabularized)");
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
