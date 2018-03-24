package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.application.options.codeStyle.WrappingAndBracesPanel;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.*;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
    @Override
    public CustomCodeStyleSettings createCustomSettings(CodeStyleSettings settings) {
        return new CsvCodeStyleSettings(settings);
    }

    @Nullable
    @Override
    public String getConfigurableDisplayName() {
        return CsvLanguage.INSTANCE.getDisplayName();
    }

    @NotNull
    @Override
    public Configurable createSettingsPage(CodeStyleSettings settings, CodeStyleSettings originalSettings) {
        return new CodeStyleAbstractConfigurable(settings, originalSettings, CsvLanguage.INSTANCE.getDisplayName()) {
            @Override
            protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
                return new CsvCodeStyleMainPanel(getCurrentSettings(), settings);
            }

            @Nullable
            @Override
            public String getHelpTopic() {
                return null;
            }
        };
    }

    private static class CsvCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
        public CsvCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
            super(CsvLanguage.INSTANCE, currentSettings, settings);
        }

        @Override
        protected void initTabs(CodeStyleSettings settings) {
            addTab(new CsvCodeStyleOptionTreeWithPreviewPanel(settings));
        }

        public class CsvCodeStyleOptionTreeWithPreviewPanel extends WrappingAndBracesPanel {

            public CsvCodeStyleOptionTreeWithPreviewPanel(CodeStyleSettings settings) {
                super(settings);
            }

            @Override
            public LanguageCodeStyleSettingsProvider.SettingsType getSettingsType() {
                return LanguageCodeStyleSettingsProvider.SettingsType.LANGUAGE_SPECIFIC;
            }

            @Override
            protected String getTabTitle() {
                return "Settings";
            }

            @Override
            public Language getDefaultLanguage() {
                return CsvCodeStyleMainPanel.this.getDefaultLanguage();
            }

            private void updatePreviewHighlighter(EditorEx editor) {
                EditorColorsScheme scheme = editor.getColorsScheme();
                editor.getSettings().setCaretRowShown(false);
                EditorHighlighter highlighter = this.createHighlighter(scheme);
                if (highlighter != null) {
                    editor.setHighlighter(highlighter);
                }
            }

            @Override
            protected PsiFile createFileFromText(final Project project, final String text) {
                // the highlighter is not properly updated - do it manually
                Editor editor = this.getEditor();
                if (editor != null) {
                    updatePreviewHighlighter((EditorEx) editor);
                }

                return super.createFileFromText(project, this.getPreviewText());
            }

            @Override
            protected String getPreviewText() {
                return CsvCodeStyleSettings.REPLACE_DEFAULT_SEPARATOR_PATTERN
                        .matcher(super.getPreviewText()).replaceAll(CsvCodeStyleSettings.getCurrentSeparator(this.getSettings()));
            }

            @Override
            protected PsiFile doReformat(Project project, PsiFile psiFile) {
                CodeStyleManager.getInstance(project).reformatText(psiFile, 0, psiFile.getTextLength());
                return psiFile;
            }
        }
    }
}