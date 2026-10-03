package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.ide.ui.search.SearchableOptionsRegistrarImpl;
import com.intellij.openapi.options.Configurable;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettingsProvider;
import java.lang.reflect.Method;

public class CsvPluginSettingsLinkTest extends CsvBasePlatformTestCase {

    public void testSettingsLinkResolvesConfigurableWithoutSearchableOptions() throws Exception {
        SearchableOptionsRegistrarImpl registrar = (SearchableOptionsRegistrarImpl) SearchableOptionsRegistrarImpl.getInstance();
        Method dropStorage = SearchableOptionsRegistrarImpl.class.getDeclaredMethod("dropStorage");
        dropStorage.setAccessible(true);
        dropStorage.invoke(registrar);
        assertFalse(registrar.isInitialized());

        Configurable configurable = new CsvEditorSettingsProvider();
        assertTrue(CsvPlugin.matchesSettingsId(configurable,
                CsvEditorSettingsProvider.CSV_EDITOR_SETTINGS_ID));
        assertFalse(registrar.isInitialized());
    }
}
