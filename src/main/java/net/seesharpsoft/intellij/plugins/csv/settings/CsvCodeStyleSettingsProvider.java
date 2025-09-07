package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.*;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.seesharpsoft.intellij.plugins.csv.CsvPluginManager.getLocalizedText;

public class CsvCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
    @Override
    public CustomCodeStyleSettings createCustomSettings(@NotNull CodeStyleSettings settings) {
        return new CsvCodeStyleSettings(settings);
    }

    @Nullable
    @Override
    public String getConfigurableDisplayName() {
        return getLocalizedText("settings.title");
    }

    @NotNull
    @Override
    public CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings, @NotNull CodeStyleSettings originalSettings) {
        return new CodeStyleAbstractConfigurable(settings, originalSettings, CsvLanguage.INSTANCE.getDisplayName()) {
            @Override
            protected @NotNull CodeStyleAbstractPanel createPanel(@NotNull CodeStyleSettings settings) {
                return new CsvCodeStyleMainPanel(getCurrentSettings(), settings);
            }

            @Nullable
            @Override
            public String getHelpTopic() {
                return null;
            }
        };
    }

    @Override
    public Language getLanguage() {
        return CsvLanguage.INSTANCE;
    }

    private static class CsvCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
        CsvCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
            super(CsvLanguage.INSTANCE, currentSettings, settings);
        }

        @Override
        protected void initTabs(CodeStyleSettings settings) {
            addTab(new CsvSpacesPanel(settings));
            addTab(new CsvIntendPanel(settings));
            addTab(new CsvWrappingPanel(settings));
        }

        protected class CsvIntendPanel extends MyIndentOptionsWrapper {
            protected CsvIntendPanel(CodeStyleSettings settings) {
                super(settings, new IndentOptionsEditor(LanguageCodeStyleSettingsProvider.forLanguage(CsvLanguage.INSTANCE)));
            }
        }

        protected class CsvWrappingPanel extends MyWrappingAndBracesPanel {
            public CsvWrappingPanel(CodeStyleSettings settings) {
                super(settings);
            }

            @Override
            public @NotNull String getTabTitle() {
                return getLocalizedText("settings.codestyle.wrapping");
            }
        }

        protected class CsvSpacesPanel extends MyWrappingAndBracesPanel {
            public CsvSpacesPanel(CodeStyleSettings settings) {
                super(settings);
            }

            @Override
            protected @NotNull String getTabTitle() {
                return getLocalizedText("settings.codestyle.spaces");
            }

            @Override
            public LanguageCodeStyleSettingsProvider.SettingsType getSettingsType() {
                return LanguageCodeStyleSettingsProvider.SettingsType.LANGUAGE_SPECIFIC;
            }

            @Override
            protected @NotNull PsiFile doReformat(Project project, @NotNull PsiFile psiFile) {
                CodeStyleManager.getInstance(project).reformatText(psiFile, 0, psiFile.getTextLength());
                return psiFile;
            }
        }
    }
}