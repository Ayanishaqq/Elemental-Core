package com.elementalcores;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

public class RecipeManager {
    private final ElementalCores plugin;
    private final CoreManager coreManager;

    public RecipeManager(ElementalCores plugin, CoreManager coreManager) {
        this.plugin = plugin;
        this.coreManager = coreManager;
    }

    public void registerRecipes() {
        // Element Caller recipe
        ShapedRecipe callerRecipe = new ShapedRecipe(new NamespacedKey(plugin, "element_caller"), coreManager.createElementCaller());
        callerRecipe.shape("DHD", "WEC", "DHD");
        callerRecipe.setIngredient('D', Material.DIAMOND_BLOCK);
        callerRecipe.setIngredient('H', coreManager.createHeavyCore());
        callerRecipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
        callerRecipe.setIngredient('E', Material.PAPER); // Placeholder for any core, check NBT in event if needed
        plugin.getServer().addRecipe(callerRecipe);

        // T1 to T2 upgrade (generic, actual check in event for specific core)
        ShapedRecipe t1ToT2 = new ShapedRecipe(new NamespacedKey(plugin, "upgrade_t1_t2"), new ItemStack(Material.PAPER)); // Result set in event
        t1ToT2.shape("DTD", "NEN", "DTD");
        t1ToT2.setIngredient('D', Material.DIAMOND_BLOCK);
        t1ToT2.setIngredient('T', Material.TOTEM_OF_UNDYING);
        t1ToT2.setIngredient('N', Material.NETHER_STAR);
        t1ToT2.setIngredient('E', Material.PAPER);
        plugin.getServer().addRecipe(t1ToT2);

        // T2 to T3
        ShapedRecipe t2ToT3 = new ShapedRecipe(new NamespacedKey(plugin, "upgrade_t2_t3"), new ItemStack(Material.PAPER));
        t2ToT3.shape("NHN", "NEN", "NHN");
        t2ToT3.setIngredient('N', Material.NETHERITE_BLOCK);
        t2ToT3.setIngredient('H', coreManager.createHeavyCore());
        t2ToT3.setIngredient('E', Material.NETHER_STAR);
        t2ToT3.setIngredient('E', Material.PAPER);
        plugin.getServer().addRecipe(t2ToT3);
    }
}
