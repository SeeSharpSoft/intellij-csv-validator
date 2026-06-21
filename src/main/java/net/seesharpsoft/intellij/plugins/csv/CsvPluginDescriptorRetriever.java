package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.BuildNumber;

import java.lang.reflect.Method;

public final class CsvPluginDescriptorRetriever {

    private static final Logger LOG = Logger.getInstance(CsvPluginDescriptorRetriever.class);
    private static final PluginId PLUGIN_ID = PluginId.getId("net.seesharpsoft.intellij.plugins.csv");

    public static IdeaPluginDescriptor getPluginDescriptor() {
        BuildNumber buildNumber = ApplicationInfo.getInstance().getBuild();
        // PluginDetailsService is available from 2026.2, which roughly corresponds to build 262
        if (buildNumber.getBaselineVersion() >= 262) {
            try {
                Class<?> serviceClass = Class.forName("com.intellij.ide.plugins.PluginDetailsService");
                Method getInstanceMethod = serviceClass.getMethod("getInstance");
                Object serviceInstance = getInstanceMethod.invoke(null);
                Method getPluginMethod = serviceClass.getMethod("getPlugin", PluginId.class);
                return (IdeaPluginDescriptor) getPluginMethod.invoke(serviceInstance, PLUGIN_ID);
            } catch (Exception e) {
                LOG.debug("Failed to retrieve plugin descriptor via PluginDetailsService", e);
                return null;
            }
        }
        
        // Fallback for older versions or if reflection fails
        try {
            Class<?> pluginManagerClass = Class.forName("com.intellij.ide.plugins.PluginManager");
            Method getInstanceMethod = pluginManagerClass.getMethod("getInstance");
            Object pluginManagerInstance = getInstanceMethod.invoke(null);
            Method findEnabledPluginMethod = pluginManagerClass.getMethod("findEnabledPlugin", PluginId.class);
            return (IdeaPluginDescriptor) findEnabledPluginMethod.invoke(pluginManagerInstance, PLUGIN_ID);
        } catch (Exception e) {
            LOG.debug("Failed to retrieve plugin descriptor via PluginManager reflection", e);
            return null;
        }
    }

    private CsvPluginDescriptorRetriever() {
    }
}
