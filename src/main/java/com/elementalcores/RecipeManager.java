package com.elementalcores;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class RecipeManager {

    private final JavaPlugin plugin;

    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        // Element Caller Recipe
        ItemStack elementCaller = ElementalCoreItems.getElementCaller();

        NamespacedKey key = new NamespacedKey(plugin, "element_caller");
        ShapedRecipe recipe = new ShapedRecipe(key, elementCaller);

        // Correct 3x3 shape
        recipe.shape("ABA", "CDC", "ABA");
        recipe.setIngredient('A', Material.DIAMOND_BLOCK);
        recipe.setIngredient('B', Material.HEAVY_CORE); // 1.21+ vanilla item
        recipe.setIngredient('C', Material.WITHER_SKELETON_SKULL);
        recipe.setIngredient('D', Material.PAPER); // Placeholder for Elemental Core (NBT check in event)

        Bukkit.addRecipe(recipe);

        // Upgrade T1 -> T2 Recipe
        ItemStack t2Core = ElementalCoreItems.getExampleCore(2); // Replace with your method for T2 core
        NamespacedKey t2Key = new NamespacedKey(plugin, "core_upgrade_t2");
        ShapedRecipe t2Recipe = new ShapedRecipe(t2Key, t2Core);
        t2Recipe.shape("ABA", "CDC", "ABA");
        t2Recipe.setIngredient('A', Material.DIAMOND_BLOCK);
        t2Recipe.setIngredient('B', Material.TOTEM_OF_UNDYING);
        t2Recipe.setIngredient('C', Material.NETHER_STAR);
        t2Recipe.setIngredient('D', Material.PAPER); // Placeholder for T1 core (NBT check in event)
        Bukkit.addRecipe(t2Recipe);

        // Upgrade T2 -> T3 Recipe
        ItemStack t3Core = ElementalCoreItems.getExampleCore(3); // Replace with your method for T3 core
        NamespacedKey t3Key = new NamespacedKey(plugin, "core_upgrade_t3");
        ShapedRecipe t3Recipe = new ShapedRecipe(t3Key, t3Core);
        t3Recipe.shape("ABA", "CDC", "ABA");
        t3Recipe.setIngredient('A', Material.NETHERITE_BLOCK);
        t3Recipe.setIngredient('B', Material.HEAVY_CORE);
        t3Recipe.setIngredient('C', Material.NETHER_STAR);
        t3Recipe.setIngredient('D', Material.PAPER); // Placeholder for T2 core (NBT check in event)
        Bukkit.addRecipe(t3Recipe);
    }
}
