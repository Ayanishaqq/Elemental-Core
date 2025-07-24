package com.elementalcores;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class NamespacedKeys {
    private static JavaPlugin plugin;

    public static void setPlugin(JavaPlugin pl) {
        plugin = pl;
    }

    public static NamespacedKey coreType() {
        return new NamespacedKey(plugin, "core_type");
    }

    public static NamespacedKey coreTier() {
        return new NamespacedKey(plugin, "core_tier");
    }
}
