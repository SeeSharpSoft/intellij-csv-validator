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
                    "TRIM_LEADING_WHITE_SPACES",
                    "Trim leading whitespaces",
                    "Trimming");
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "TRIM_TRAILING_WHITE_SPACES",
                    "Trim trailing whitespaces",
                    "Trimming");
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
        return "1 ,\"Eldon Base for stackable storage shelf, platinum\", Muhammed MacIntyre   ,3,-213.25 ,   38.94  \n" +
                "   2 ,\"   1.7 Cubic Foot Compact \"\"Cube\"\" Office Refrigerators\",Barry French,  293,457.81,208.16\n" +
                "\n" +
                "3,\"Cardinal Slant-D® Ring Binder, Heavy Gauge Vinyl   \",Barry French, 293 ,46.71 ,8.69\n" +
                "4   ,    R380 ,Clay Rozendal,483,  1198.97,195.99 \n" +
                "3.1\n" +
                "5 ,Holmes HEPA Air Purifier,Carlos Soltero,汉字宋,30.94,21.78";
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
