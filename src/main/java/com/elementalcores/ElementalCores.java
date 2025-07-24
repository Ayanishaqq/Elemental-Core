package com.elementalcores;

import com.elementalcores.commands.CoreCommand;
import com.elementalcores.listeners.CoreListener;
import com.elementalcores.listeners.PlayerJoinListener;
import com.elementalcores.managers.CoreManager;
import com.elementalcores.managers.CraftingManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ElementalCores extends JavaPlugin {
    private static ElementalCores instance;
    private CoreManager coreManager;
    private CraftingManager craftingManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        coreManager = new CoreManager(this);
        craftingManager = new CraftingManager(this);
        
        // Register commands
        getCommand("core").setExecutor(new CoreCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new CoreListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        
        // Register crafting recipes
        craftingManager.registerRecipes();
        
        getLogger().info("ElementalCores has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ElementalCores has been disabled!");
    }

    public static ElementalCores getInstance() {
        return instance;
    }

    public CoreManager getCoreManager() {
        return coreManager;
    }

    public CraftingManager getCraftingManager() {
        return craftingManager;
    }
}
