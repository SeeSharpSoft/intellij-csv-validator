package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.application.options.colors.ColorAndFontOptions;
import com.intellij.ide.ui.search.SearchableOptionsRegistrarImpl;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableGroup;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ex.ConfigurableExtensionPointUtil;
import com.intellij.openapi.options.ex.ConfigurableVisitor;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvColorSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettingsProvider;
import java.lang.reflect.Method;
import java.util.List;

public class CsvPluginSettingsLinkTest extends CsvBasePlatformTestCase {

    public void testSettingsLinkResolvesConfigurableWithoutSearchableOptions() throws Exception {
        SearchableOptionsRegistrarImpl registrar = (SearchableOptionsRegistrarImpl) SearchableOptionsRegistrarImpl.getInstance();
        Method dropStorage = SearchableOptionsRegistrarImpl.class.getDeclaredMethod("dropStorage");
        dropStorage.setAccessible(true);
        dropStorage.invoke(registrar);
        assertFalse(registrar.isInitialized());

        Configurable configurable = new CsvEditorSettingsProvider();
        assertTrue(configurable instanceof SearchableConfigurable);
        assertTrue(CsvPlugin.matchesSettingsId(configurable,
                CsvEditorSettingsProvider.CSV_EDITOR_SETTINGS_ID));
        assertFalse(registrar.isInitialized());
    }

    public void testNotificationSettingsLinksResolveRegisteredConfigurables() {
        ConfigurableGroup configurableGroup = ConfigurableExtensionPointUtil.getConfigurableGroup(getProject(), true);

        assertNotNull(findConfigurable(configurableGroup, CsvEditorSettingsProvider.CSV_EDITOR_SETTINGS_ID));
        ColorAndFontOptions colorOptions = new ColorAndFontOptions();
        try {
            SearchableConfigurable colorConfigurable = colorOptions.findSubConfigurable(CsvColorSettings.class);
            assertNotNull(colorConfigurable);
            assertNotNull(findConfigurable(configurableGroup, colorConfigurable.getId()));
        } finally {
            for (Configurable configurable : colorOptions.getConfigurables()) {
                configurable.disposeUIResources();
            }
            colorOptions.disposeUIResources();
        }
        assertNotNull(findConfigurable(configurableGroup,
                CodeStyleSettings.generateConfigurableIdByLanguage(CsvLanguage.INSTANCE)));
    }

    private static Configurable findConfigurable(ConfigurableGroup configurableGroup, String settingsId) {
        return ConfigurableVisitor.find(configurable -> CsvPlugin.matchesSettingsId(configurable, settingsId),
                List.of(configurableGroup));
    }
}
