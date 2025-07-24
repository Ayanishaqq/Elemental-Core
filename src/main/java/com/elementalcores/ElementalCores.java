package com.elementalcores;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class ElementalCores extends JavaPlugin implements Listener {

    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        getLogger().info("ElementalCores enabled!");

        // Set up NamespacedKeys helper for NBT
        NamespacedKeys.setPlugin(this);

        // Register recipes
        recipeManager = new RecipeManager(this);
        recipeManager.registerRecipes();

        // Register event listeners
        getServer().getPluginManager().registerEvents(this, this);

        // TODO: Register your commands here
        // getCommand("core").setExecutor(new CoreCommandExecutor(this));
    }

    @Override
    public void onDisable() {
        getLogger().info("ElementalCores disabled!");
    }

    // NBT check for crafting (example for Element Caller)
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;

        // Example: Check if crafting Element Caller
        ItemStack result = event.getRecipe().getResult();
        if (ElementalCoreItems.isElementCaller(result)) {
            ItemStack[] matrix = event.getInventory().getMatrix();
            ItemStack center = matrix[4]; // Center slot in 3x3 grid
            if (!ElementalCoreItems.isRealElementalCore(center)) {
                event.getInventory().setResult(null); // Cancel crafting if not a real core
            }
        }

        // TODO: Add similar checks for T1->T2 and T2->T3 upgrades if needed
    }
}
