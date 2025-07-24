package com.elementalcores;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.NamespacedKey;

public class RecipeManager implements Listener {
    private final ElementalCores plugin;
    private final CoreManager coreManager;

    public RecipeManager(ElementalCores plugin, CoreManager coreManager) {
        this.plugin = plugin;
        this.coreManager = coreManager;
    }

    public void registerRecipes() {
        // Element Caller recipe
        ShapedRecipe callerRecipe = new ShapedRecipe(new NamespacedKey(plugin, "element_caller"), coreManager.createElementCaller());
        callerRecipe.shape("DHD", "W E W", "DHD");  // Adjusted for 3x3
        callerRecipe.setIngredient('D', Material.DIAMOND_BLOCK);
        callerRecipe.setIngredient('H', new RecipeChoice.ExactChoice(coreManager.createHeavyCore()));
        callerRecipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
        callerRecipe.setIngredient('E', Material.PAPER);  // Any core (NBT checked in event)
        plugin.getServer().addRecipe(callerRecipe);

        // T1 → T2 upgrade (generic shape, NBT handled in event)
        ShapedRecipe t1ToT2 = new ShapedRecipe(new NamespacedKey(plugin, "upgrade_t1_t2"), new ItemStack(Material.PAPER)); // Placeholder result
        t1ToT2.shape("DTD", "N E N", "DTD");
        t1ToT2.setIngredient('D', Material.DIAMOND_BLOCK);
        t1ToT2.setIngredient('T', Material.TOTEM_OF_UNDYING);
        t1ToT2.setIngredient('N', Material.NETHER_STAR);
        t1ToT2.setIngredient('E', Material.PAPER);
        plugin.getServer().addRecipe(t1ToT2);

        // T2 → T3 upgrade
        ShapedRecipe t2ToT3 = new ShapedRecipe(new NamespacedKey(plugin, "upgrade_t2_t3"), new ItemStack(Material.PAPER));
        t2ToT3.shape("NHN", "N E N", "NHN");
        t2ToT3.setIngredient('N', Material.NETHERITE_BLOCK);
        t2ToT3.setIngredient('H', new RecipeChoice.ExactChoice(coreManager.createHeavyCore()));
        t2ToT3.setIngredient('E', Material.NETHER_STAR);
        t2ToT3.setIngredient('E', Material.PAPER);
        plugin.getServer().addRecipe(t2ToT3);
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();
        if (result == null) return;

        // Find the center item (the core)
        ItemStack coreItem = inv.getItem(4); // Center of 3x3 grid (0-based index 4)
        if (coreItem == null || !coreManager.isCore(coreItem)) {
            inv.setResult(null); // Invalid if no core or not a real core (NBT check)
            return;
        }

        CoreType type = coreManager.getCoreType(coreItem);
        int tier = coreManager.getCoreTier(coreItem);

        // Check which recipe is being used
        if (event.getRecipe() instanceof ShapedRecipe recipe) {
            if (recipe.getKey().getKey().equals("upgrade_t1_t2")) {
                if (tier == 1) {
                    inv.setResult(coreManager.createCore(type, 2)); // Upgrade to T2
                } else {
                    inv.setResult(null); // Wrong tier
                }
            } else if (recipe.getKey().getKey().equals("upgrade_t2_t3")) {
                if (tier == 2) {
                    inv.setResult(coreManager.createCore(type, 3)); // Upgrade to T3
                } else {
                    inv.setResult(null); // Wrong tier
                }
            } else if (recipe.getKey().getKey().equals("element_caller")) {
                // Allow if center is any core
                inv.setResult(coreManager.createElementCaller());
            }
        }
    }
}
