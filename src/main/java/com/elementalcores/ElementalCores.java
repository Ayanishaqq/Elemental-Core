package com.elementalcores;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;

public class ElementalCores extends JavaPlugin implements Listener {  // Implements Listener for join event

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
        getServer().getPluginManager().registerEvents(passiveManager, this);
        getServer().getPluginManager().registerEvents(recipeManager, this);
        getServer().getPluginManager().registerEvents(this, this);  // Register for PlayerJoinEvent
        getCommand("core").setExecutor(commandHandler);
        recipeManager.registerRecipes();

        // Scheduler for passives (check every second)
        getServer().getScheduler().runTaskTimer(this, passiveManager::applyPassives, 0L, 20L);
    }

    @Override
    public void onDisable() {
        getLogger().info("ElementalCores disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Check if player has no core (assume "first join" if inventory has no core)
        boolean hasCore = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && coreManager.isCore(item)) {
                hasCore = true;
                break;
            }
        }
        if (!hasCore) {
            coreManager.giveRandomCore(player);  // Give core with animation on first join
        }
    }
}
