package com.elementalcores;

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
        registerEarthCoreRecipe();
        registerWaterCoreRecipe();
        registerFireCoreRecipe();
        registerAirCoreRecipe();
        registerLightningCoreRecipe();
        registerIceCoreRecipe();
        registerNatureCoreRecipe();
        registerShadowCoreRecipe();
        registerLightCoreRecipe();
    }
    
    public void registerUpgradeRecipes() {
        registerTier1ToTier2Recipe();
        registerTier2ToTier3Recipe();
        registerElementCallerRecipe();
    }
    
    private void registerEarthCoreRecipe() {
        ItemStack earthCore = ((ElementalCores) plugin).createCoreItem("earth", 1);
        NamespacedKey key = new NamespacedKey(plugin, "earth_core");
        
        ShapedRecipe recipe = new ShapedRecipe(key, earthCore);
        recipe.shape("OSO", "SHS", "OSO");
        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('S', Material.STONE);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private void registerWaterCoreRecipe() {
        ItemStack waterCore = ((ElementalCores) plugin).createCoreItem("water", 1);
        NamespacedKey key = new NamespacedKey(plugin, "water_core");
        
        ShapedRecipe recipe = new ShapedRecipe(key, waterCore);
        recipe.shape("WWW", "WHW", "WWW");
        recipe.setIngredient('W', Material.WATER_BUCKET);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private void registerFireCoreRecipe() {
        ItemStack fireCore = ((ElementalCores) plugin).createCoreItem("fire", 1);
        NamespacedKey key = new NamespacedKey(plugin, "fire_core");
        
        ShapedRecipe recipe = new ShapedRecipe(key, fireCore);
        recipe.shape("LFL", "FHF", "LFL");
        recipe.setIngredient('L', Material.LAVA_BUCKET);
        recipe.setIngredient('F', Material.FIRE_CHARGE);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private void registerAirCoreRecipe() {
        ItemStack airCore = ((ElementalCores) plugin).createCoreItem("air", 1);
        NamespacedKey key = new NamespacedKey(plugin, "air_core");
        
        ShapedRecipe recipe = new ShapedRecipe(key, airCore);
        recipe.shape("FGF", "GHG", "FGF");
        recipe.setIngredient('F', Material.FEATHER);
        recipe.setIngredient('G', Material.GLASS);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private void registerLightningCoreRecipe() {
        ItemStack lightningCore = ((ElementalCores) plugin).createCoreItem("lightning", 1);
        NamespacedKey key = new NamespacedKey(plugin, "lightning_core");
        
        ShapedRecipe recipe = new ShapedRecipe(key, lightningCore);
        recipe.shape("RCR", "CHC", "RCR");
        recipe.setIngredient('R', Material.REDSTONE_BLOCK);
        recipe.setIngredient('C', Material.COPPER_BLOCK);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private void registerIceCoreRecipe() {
        ItemStack iceCore = ((ElementalCores) plugin).createCoreItem("ice", 1);
        NamespacedKey key = new NamespacedKey(plugin, "ice_core");
        
        ShapedRecipe recipe = new ShapedRecipe(key, iceCore);
        recipe.shape("III", "IHI", "III");
        recipe.setIngredient('I', Material.PACKED_ICE);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private void registerNatureCoreRecipe() {
        ItemStack natureCore = ((ElementalCores) plugin).createCoreItem("nature", 1);
        NamespacedKey key = new NamespacedKey(plugin, "nature_core");
        
        ShapedRecipe recipe = new ShapedRecipe(key, natureCore);
        recipe.shape("LOL", "OHO", "LOL");
        recipe.setIngredient('L', Material.OAK_LEAVES);
        recipe.setIngredient('O', Material.OAK_LOG);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private void registerShadowCoreRecipe() {
        ItemStack shadowCore = ((ElementalCores) plugin).createCoreItem("shadow", 1);
        NamespacedKey key = new NamespacedKey(plugin, "shadow_core");
        
        ShapedRecipe recipe = new ShapedRecipe(key, shadowCore);
        recipe.shape("EOE", "OHO", "EOE");
        recipe.setIngredient('E', Material.ENDER_PEARL);
        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private void registerLightCoreRecipe() {
        ItemStack lightCore = ((ElementalCores) plugin).createCoreItem("light", 1);
        NamespacedKey key = new NamespacedKey(plugin, "light_core");
        
        ShapedRecipe recipe = new ShapedRecipe(key, lightCore);
        recipe.shape("GDG", "DHD", "GDG");
        recipe.setIngredient('G', Material.GLOWSTONE);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        
        plugin.getServer().addRecipe(recipe);
    }
    
    private void registerTier1ToTier2Recipe() {
        // For each core type
        for (String coreType : new String[]{"earth", "water", "fire", "air", "lightning", "ice", "nature", "shadow", "light"}) {
            NamespacedKey key = new NamespacedKey(plugin, coreType + "_tier1_to_tier2");
            
            // Create a shaped recipe for upgrading this core type
            ShapedRecipe recipe = new ShapedRecipe(key, ((ElementalCores)plugin).createCoreItem(coreType, 2));
            
            recipe.shape("DTD", "NCN", "DTD");
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
            recipe.setIngredient('N', Material.NETHER_STAR);
            recipe.setIngredient('C', Material.PAPER); // Will check for correct tier1 core in CraftItemEvent
            
            plugin.getServer().addRecipe(recipe);
        }
    }
    
    private void registerTier2ToTier3Recipe() {
        // For each core type
        for (String coreType : new String[]{"earth", "water", "fire", "air", "lightning", "ice", "nature", "shadow", "light"}) {
            NamespacedKey key = new NamespacedKey(plugin, coreType + "_tier2_to_tier3");
            
            // Create a shaped recipe for upgrading this core type
            ShapedRecipe recipe = new ShapedRecipe(key, ((ElementalCores)plugin).createCoreItem(coreType, 3));
            
            recipe.shape("NHN", "NCN", "NHN");
            recipe.setIngredient('N', Material.NETHERITE_BLOCK);
            recipe.setIngredient('H', Material.HEAVY_CORE);
            recipe.setIngredient('C', Material.PAPER); // Will check for correct tier2 core in CraftItemEvent
            
            plugin.getServer().addRecipe(recipe);
        }
    }
    
    private void registerElementCallerRecipe() {
        ItemStack elementCaller = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = elementCaller.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Element Caller");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Right-click while holding this item",
            ChatColor.GRAY + "to transform your current core",
            ChatColor.GRAY + "into a random one of the same tier.",
            "",
            ChatColor.RED + "Consumes this item when used!"
        ));
        elementCaller.setItemMeta(meta);
        
        NamespacedKey key = new NamespacedKey(plugin, "element_caller");
        
        // Create the recipe
        ShapedRecipe recipe = new ShapedRecipe(key, elementCaller);
        
        recipe.shape("DHD", "WCW", "DHD");
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
        recipe.setIngredient('C', Material.PAPER); // Will check for any core in CraftItemEvent
        
        plugin.getServer().addRecipe(recipe);
    }
}
