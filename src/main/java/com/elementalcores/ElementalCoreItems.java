package com.elementalcores;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ElementalCoreItems {

    // Core color hex codes
    private static final String[] CORE_TYPES = {
            "earth", "water", "fire", "air", "lightning", "ice", "nature", "shadow", "light"
    };
    private static final String[] CORE_COLORS = {
            "#8B5A2B", "#1E90FF", "#FF4500", "#B0E0E6", "#FFFF00", "#00FFFF", "#228B22", "#4B0082", "#FFFACD"
    };

    // Get a custom Elemental Core item
    public static ItemStack getElementalCore(String type, int tier) {
        int index = getCoreIndex(type);
        if (index == -1) index = 0; // fallback to earth

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        // Set display name with hex color
        String name = net.md_5.bungee.api.ChatColor.of(CORE_COLORS[index]) + capitalize(type) + " Core (Tier " + tier + ")";
        meta.setDisplayName(name);

        // Set lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + getCoreLore(type));
        lore.add("");
        lore.add(ChatColor.GOLD + "Right Click: " + ChatColor.RESET + getAbilityName(type, 1));
        lore.add(ChatColor.GRAY + getAbilityDesc(type, 1, tier));
        lore.add(ChatColor.GOLD + "Shift+Right Click: " + ChatColor.RESET + getAbilityName(type, 2));
        lore.add(ChatColor.GRAY + getAbilityDesc(type, 2, tier));
        lore.add(ChatColor.GOLD + "Passive (Offhand): " + ChatColor.RESET + getPassive(type));
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Tier " + tier + ": " + getTierDesc(tier));
        meta.setLore(lore);

        // Set NBT tags
        meta.getPersistentDataContainer().set(NamespacedKeys.coreType(), PersistentDataType.STRING, type);
        meta.getPersistentDataContainer().set(NamespacedKeys.coreTier(), PersistentDataType.INTEGER, tier);

        item.setItemMeta(meta);
        return item;
    }

    // Get the Element Caller item
    public static ItemStack getElementCaller() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(net.md_5.bungee.api.ChatColor.of("#00FFFF") + "Element Caller");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.AQUA + "Right-click to summon the elements and receive a new core!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
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

    // Check if item is a real Elemental Core (by NBT)
    public static boolean isRealElementalCore(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String type = meta.getPersistentDataContainer().get(NamespacedKeys.coreType(), PersistentDataType.STRING);
        Integer tier = meta.getPersistentDataContainer().get(NamespacedKeys.coreTier(), PersistentDataType.INTEGER);
        return type != null && tier != null && isValidCoreType(type) && tier >= 1 && tier <= 3;
    }

    // Check if item is the Element Caller
    public static boolean isElementCaller(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase("Element Caller");
    }

    // Helper: Get core index
    private static int getCoreIndex(String type) {
        for (int i = 0; i < CORE_TYPES.length; i++) {
            if (CORE_TYPES[i].equalsIgnoreCase(type)) return i;
        }
        return -1;
    }

    // Helper: Capitalize
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // Helper: Is valid core type
    private static boolean isValidCoreType(String type) {
        for (String t : CORE_TYPES) if (t.equalsIgnoreCase(type)) return true;
        return false;
    }

    // Helper: Core flavor lore
    private static String getCoreLore(String type) {
        switch (type.toLowerCase()) {
            case "earth": return "A core pulsing with the strength of stone.";
            case "water": return "A core swirling with the endless flow of water.";
            case "fire": return "A core burning with the fury of flame.";
            case "air": return "A core whispering with the winds of the sky.";
            case "lightning": return "A core crackling with unstoppable energy.";
            case "ice": return "A core frozen with the chill of winter.";
            case "nature": return "A core blooming with the power of life.";
            case "shadow": return "A core shrouded in mysterious darkness.";
            case "light": return "A core radiating with pure, blinding light.";
            default: return "A mysterious elemental core.";
        }
    }

    // Helper: Ability names
    private static String getAbilityName(String type, int which) {
        switch (type.toLowerCase()) {
            case "earth": return which == 1 ? "Earth Prison" : "Seismic Slam";
            case "water": return which == 1 ? "Tsunami Surge" : "Aqua Vortex";
            case "fire": return which == 1 ? "Meteor Strike" : "Infernal Eruption";
            case "air": return which == 1 ? "Tornado Launch" : "Wind Blade";
            case "lightning": return which == 1 ? "Stormcall" : "Lightning Dash";
            case "ice": return which == 1 ? "Glacial Spike" : "Blizzard";
            case "nature": return which == 1 ? "Entangling Roots" : "Nature’s Wrath";
            case "shadow": return which == 1 ? "Shadow Clone" : "Void Step";
            case "light": return which == 1 ? "Solar Beam" : "Sanctuary";
            default: return "Unknown";
        }
    }

    // Helper: Ability descriptions (per tier)
    private static String getAbilityDesc(String type, int which, int tier) {
        switch (type.toLowerCase()) {
            case "earth":
                if (which == 1) {
                    if (tier == 1) return "Trap nearest enemy in a 3x3x3 stone cage for 5s.";
                    if (tier == 2) return "Trap up to 2 enemies in 4x4x4 cages for 6s.";
                    return "Trap up to 3 enemies in 5x5x5 unbreakable cages for 8s, deals damage on trap.";
                } else {
                    if (tier == 1) return "Leap and slam, launching all nearby mobs/players.";
                    if (tier == 2) return "Larger shockwave, launches and slows all mobs/players in a 7-block radius.";
                    return "Massive shockwave, launches, slows, and stuns all mobs/players in a 10-block radius.";
                }
            case "water":
                if (which == 1) {
                    if (tier == 1) return "Summon a wave that pushes and damages mobs/players.";
                    if (tier == 2) return "Stronger wave, more damage and range.";
                    return "Massive wave, huge damage and knockback.";
                } else {
                    if (tier == 1) return "Pulls in mobs/players and suffocates them briefly.";
                    if (tier == 2) return "Larger vortex, longer duration.";
                    return "Huge vortex, pulls in all nearby enemies.";
                }
            case "fire":
                if (which == 1) {
                    if (tier == 1) return "Call down a meteor, causing a fiery explosion.";
                    if (tier == 2) return "Larger meteor, bigger explosion, ignites ground.";
                    return "Multiple meteors, massive explosion, ignites and knocks back all enemies.";
                } else {
                    if (tier == 1) return "Burst of flames, burning all nearby mobs/players.";
                    if (tier == 2) return "Flames last longer, burn effect is stronger.";
                    return "Flames persist, burn spreads, grants temporary fire immunity.";
                }
            case "air":
                if (which == 1) {
                    if (tier == 1) return "Summon a tornado that lifts you and mobs/players.";
                    if (tier == 2) return "Stronger tornado, more height and area.";
                    return "Massive tornado, lifts all nearby enemies.";
                } else {
                    if (tier == 1) return "Fires a blade of wind, piercing through enemies.";
                    if (tier == 2) return "Pierces more targets, more damage.";
                    return "Ultimate wind blade, huge damage and range.";
                }
            case "lightning":
                if (which == 1) {
                    if (tier == 1) return "Strike up to 5 mobs/players with lightning.";
                    if (tier == 2) return "Strike up to 8 mobs/players, stuns them.";
                    return "Strike up to 12 mobs/players, stuns and deals extra damage.";
                } else {
                    if (tier == 1) return "Teleport forward, leaving a lightning trail.";
                    if (tier == 2) return "Teleport further, trail stuns mobs/players.";
                    return "Teleport even further, trail deals heavy damage and stuns.";
                }
            case "ice":
                if (which == 1) {
                    if (tier == 1) return "Launch a spike of ice, freezing the first enemy hit.";
                    if (tier == 2) return "Larger spike, freezes longer.";
                    return "Ultimate spike, freezes and damages all in path.";
                } else {
                    if (tier == 1) return "Create a blizzard, damaging and slowing all inside.";
                    if (tier == 2) return "Bigger blizzard, more damage and slow.";
                    return "Massive blizzard, huge area and duration.";
                }
            case "nature":
                if (which == 1) {
                    if (tier == 1) return "Trap all enemies in roots, dealing poison.";
                    if (tier == 2) return "Larger radius, more poison damage.";
                    return "Ultimate roots, huge area and poison.";
                } else {
                    if (tier == 1) return "Grow thorns, damaging and knocking back all enemies.";
                    if (tier == 2) return "Thorns last longer, more knockback.";
                    return "Massive thorns, huge damage and knockback.";
                }
            case "shadow":
                if (which == 1) {
                    if (tier == 1) return "Summon clones to attack and distract.";
                    if (tier == 2) return "More clones, longer duration.";
                    return "Ultimate clones, huge distraction.";
                } else {
                    if (tier == 1) return "Teleport and leave a cloud of darkness.";
                    if (tier == 2) return "Teleport further, stronger effects.";
                    return "Ultimate teleport, massive darkness.";
                }
            case "light":
                if (which == 1) {
                    if (tier == 1) return "Fire a beam of light, damaging and blinding.";
                    if (tier == 2) return "Stronger beam, more damage and area.";
                    return "Ultimate beam, massive damage and blinding.";
                } else {
                    if (tier == 1) return "Create a dome of light, healing and protecting allies.";
                    if (tier == 2) return "Larger dome, more healing.";
                    return "Ultimate dome, huge healing and protection.";
                }
            default: return "Unknown ability.";
        }
    }

    // Helper: Passive effects
    private static String getPassive(String type) {
        switch (type.toLowerCase()) {
            case "earth": return "Resistance II, Haste I";
            case "water": return "Water Breathing, Dolphin’s Grace";
            case "fire": return "Fire Resistance, Strength I";
            case "air": return "Jump Boost II, Slow Falling";
            case "lightning": return "Speed II, Night Vision";
            case "ice": return "Resistance to Slowness, Frost Walker";
            case "nature": return "Regeneration II, Saturation";
            case "shadow": return "Night Vision, Invisibility (short bursts)";
            case "light": return "Glowing, Absorption I";
            default: return "Unknown";
        }
    }

    // Helper: Tier descriptions
    private static String getTierDesc(int tier) {
        switch (tier) {
            case 1: return "Basic elemental power.";
            case 2: return "Enhanced elemental might.";
            case 3: return "Legendary elemental force.";
            default: return "";
        }
    }
}
