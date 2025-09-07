package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.DynamicBundle;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;

import java.util.ResourceBundle;

public final class CsvPluginManager {
    private static ResourceBundle _resourceBundle;

    public static ResourceBundle getResourceBundle() {
        if (_resourceBundle == null) {
            _resourceBundle = DynamicBundle.getPluginBundle(getPluginDescriptor());
        }
        return _resourceBundle;
    }

    public static String getLocalizedText(String token) {
        return getResourceBundle().getString(token);
    }

    public static IdeaPluginDescriptor getPluginDescriptor() {
        return PluginManagerCore.getPlugin(PluginId.getId("net.seesharpsoft.intellij.plugins.csv"));
    }

    public static String getVersion() {
        return getPluginDescriptor().getVersion();
    }

    public static String getChangeNotes() {
        return getPluginDescriptor().getChangeNotes();
    }

    private CsvPluginManager() {
        // static
    }
}
