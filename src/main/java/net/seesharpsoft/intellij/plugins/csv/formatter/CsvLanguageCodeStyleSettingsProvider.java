package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
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
        if (settingsType == SettingsType.SPACING_SETTINGS) {
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "TRIM_LEADING_WHITE_SPACES",
                    "Trim leading whitespaces",
                    "White Spaces");
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "TRIM_TRAILING_WHITE_SPACES",
                    "Trim trailing whitespaces",
                    "White Spaces");
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "TABULARIZE",
                    "Enabled",
                    "Tabularize (ignores White Space settings)");
            consumer.showCustomOption(CsvCodeStyleSettings.class,
                    "LEADING_WHITE_SPACES",
                    "Leading White Spaces",
                    "Tabularize (ignores White Space settings)");
        }
    }

    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        return " 1,\"Eldon Base for stackable storage shelf, platinum\", Muhammed MacIntyre   ,3,-213.25 ,   38.94  \n" +
                "\n" +
                "\n" +
                "   2 ,\"   1.7 Cubic Foot Compact \"\"Cube\"\" Office Refrigerators\",Barry French,  293,457.81,208.16\n" +
                "3,\"Cardinal Slant-D® Ring Binder, Heavy Gauge Vinyl   \",Barry French, 293 ,46.71 ,8.69\n" +
                "3.1\n" +
                "4   ,    R380 ,Clay Rozendal,483,  1198.97,195.99 \n" +
                "5 ,Holmes HEPA Air Purifier,Carlos Soltero,515,30.94,21.78";
    }
}
