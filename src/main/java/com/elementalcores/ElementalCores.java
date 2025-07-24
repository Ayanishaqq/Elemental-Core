package com.elementalcores;

import org.bukkit.plugin.java.JavaPlugin;

public class ElementalCores extends JavaPlugin {

    private CoreManager coreManager;
    private AbilityListener abilityListener;
    private CommandHandler commandHandler;
    private RecipeManager recipeManager;
    private PassiveManager passiveManager;

    @Override
    public void onEnable() {
        getLogger().info("ElementalCores v1.21.5 enabled!");

        // Initialize managers
        coreManager = new CoreManager(this);
        abilityListener = new AbilityListener(this, coreManager);
        commandHandler = new CommandHandler(this, coreManager);
        recipeManager = new RecipeManager(this, coreManager);
        passiveManager = new PassiveManager(this, coreManager);

        // Register events, commands, recipes
        getServer().getPluginManager().registerEvents(abilityListener, this);
        getServer().getPluginManager().registerEvents(passiveManager, this); // For passives and inventory checks
        getCommand("core").setExecutor(commandHandler);
        recipeManager.registerRecipes();

        // Scheduler for passives (check every second)
        getServer().getScheduler().runTaskTimer(this, passiveManager::applyPassives, 0L, 20L);
    }

    @Override
    public void onDisable() {
        getLogger().info("ElementalCores disabled!");
    }
}
