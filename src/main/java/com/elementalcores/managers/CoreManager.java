package com.elementalcores.managers;

import com.elementalcores.ElementalCores;
import com.elementalcores.core.CoreType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CoreManager {
    private final ElementalCores plugin;
    private final NamespacedKey coreTypeKey;
    private final NamespacedKey coreTierKey;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CoreManager(ElementalCores plugin) {
        this.plugin = plugin;
        this.coreTypeKey = new NamespacedKey(plugin, "core_type");
        this.coreTierKey = new NamespacedKey(plugin, "core_tier");
    }

    public ItemStack createCore(CoreType type, int tier) {
        ItemStack core = new ItemStack(Material.PAPER);
        ItemMeta meta = core.getItemMeta();
        
        // Set display name
        meta.setDisplayName(type.getColor() + "" + ChatColor.BOLD + type.getName() + " Core (Tier " + tier + ")");
        
        // Set custom model data
        meta.setCustomModelData(type.getCustomModelData());
        
        // Set lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A core pulsing with " + type.getName().toLowerCase() + " energy.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Right Click: " + ChatColor.WHITE + getRightClickAbility(type));
        lore.add(ChatColor.GRAY + getRightClickDescription(type));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Shift+Right Click: " + ChatColor.WHITE + getShiftRightClickAbility(type));
        lore.add(ChatColor.GRAY + getShiftRightClickDescription(type));
        lore.add("");
        lore.add(ChatColor.AQUA + "Passive (Offhand): " + ChatColor.WHITE + getPassiveEffects(type));
        lore.add("");
        lore.add(ChatColor.GOLD + "Tier " + tier + ": " + getTierDescription(tier));
        
        meta.setLore(lore);
        
        // Set persistent data
        meta.getPersistentDataContainer().set(coreTypeKey, PersistentDataType.STRING, type.name());
        meta.getPersistentDataContainer().set(coreTierKey, PersistentDataType.INTEGER, tier);
        
        core.setItemMeta(meta);
        return core;
    }

    public ItemStack createElementCaller() {
        ItemStack caller = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = caller.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Element Caller");
        meta.setCustomModelData(2001);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Right-click to summon the elements and receive a new core!");
        meta.setLore(lore);
        
        caller.setItemMeta(meta);
        return caller;
    }

    public ItemStack createHeavyCore() {
        ItemStack heavyCore = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = heavyCore.getItemMeta();
        
        meta.setDisplayName(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Heavy Core");
        meta.setCustomModelData(1100);
        
        heavyCore.setItemMeta(meta);
        return heavyCore;
    }

    public boolean isCore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(coreTypeKey, PersistentDataType.STRING);
    }

    public boolean isElementCaller(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasCustomModelData() && meta.getCustomModelData() == 2001;
    }

    public boolean isHeavyCore(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_INGOT) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasCustomModelData() && meta.getCustomModelData() == 1100;
    }

    public CoreType getCoreType(ItemStack item) {
        if (!isCore(item)) return null;
        String typeName = item.getItemMeta().getPersistentDataContainer().get(coreTypeKey, PersistentDataType.STRING);
        try {
            return CoreType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public int getCoreTier(ItemStack item) {
        if (!isCore(item)) return 0;
        Integer tier = item.getItemMeta().getPersistentDataContainer().get(coreTierKey, PersistentDataType.INTEGER);
        return tier != null ? tier : 1;
    }

    public void removeAllCores(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCore(item)) {
                player.getInventory().remove(item);
            }
        }
    }

    public void applyPassiveEffects(Player player, CoreType type) {
        switch (type) {
            case EARTH:
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 1, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, 0, true, false));
                break;
            case WATER:
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 100, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 100, 0, true, false));
                break;
            case FIRE:
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0, true, false));
                break;
            case AIR:
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 1, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, true, false));
                break;
            case LIGHTNING:
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 0, true, false));
                break;
            case ICE:
                // Frost Walker is handled differently
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 0, true, false));
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                break;
            case NATURE:
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 100, 0, true, false));
                break;
            case SHADOW:
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 0, true, false));
                if (player.getWorld().getTime() > 12000) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, true, false));
                }
                break;
            case LIGHT:
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 0, true, false));
                break;
        }
    }

    public boolean isOnCooldown(Player player, String ability) {
        String key = player.getUniqueId() + ":" + ability;
        if (cooldowns.containsKey(player.getUniqueId())) {
            long cooldownEnd = cooldowns.get(player.getUniqueId());
            if (System.currentTimeMillis() < cooldownEnd) {
                return true;
            }
        }
        return false;
    }

    public void setCooldown(Player player, String ability, int seconds) {
        String key = player.getUniqueId() + ":" + ability;
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }

    public int getCooldownRemaining(Player player, String ability) {
        String key = player.getUniqueId() + ":" + ability;
        if (cooldowns.containsKey(player.getUniqueId())) {
            long cooldownEnd = cooldowns.get(player.getUniqueId());
            long remaining = cooldownEnd - System.currentTimeMillis();
            if (remaining > 0) {
                return (int) Math.ceil(remaining / 1000.0);
            }
        }
        return 0;
    }

    private String getRightClickAbility(CoreType type) {
        switch (type) {
            case EARTH: return "Earth Prison";
            case WATER: return "Tsunami Surge";
            case FIRE: return "Meteor Strike";
            case AIR: return "Tornado Launch";
            case LIGHTNING: return "Stormcall";
            case ICE: return "Glacial Spike";
            case NATURE: return "Entangling Roots";
            case SHADOW: return "Shadow Clone";
            case LIGHT: return "Solar Beam";
            default: return "Unknown";
        }
    }

    private String getRightClickDescription(CoreType type) {
        switch (type) {
            case EARTH: return "Trap nearest enemy in a stone cage";
            case WATER: return "Summon a wave that pushes and damages enemies";
            case FIRE: return "Call down a meteor, causing a fiery explosion";
            case AIR: return "Summon a tornado that lifts you and enemies";
            case LIGHTNING: return "Strike enemies with lightning";
            case ICE: return "Launch a spike of ice, freezing enemies";
            case NATURE: return "Trap all enemies in roots, dealing poison";
            case SHADOW: return "Summon clones to attack and distract";
            case LIGHT: return "Fire a beam of light, damaging and blinding";
            default: return "Unknown";
        }
    }

    private String getShiftRightClickAbility(CoreType type) {
        switch (type) {
            case EARTH: return "Seismic Slam";
            case WATER: return "Aqua Vortex";
            case FIRE: return "Infernal Eruption";
            case AIR: return "Wind Blade";
            case LIGHTNING: return "Lightning Dash";
            case ICE: return "Blizzard";
            case NATURE: return "Nature's Wrath";
            case SHADOW: return "Void Step";
            case LIGHT: return "Sanctuary";
            default: return "Unknown";
        }
    }

    private String getShiftRightClickDescription(CoreType type) {
        switch (type) {
            case EARTH: return "Leap and slam, launching and stunning nearby enemies";
            case WATER: return "Pull in enemies and suffocate them briefly";
            case FIRE: return "Burst of flames, burning all nearby enemies";
            case AIR: return "Fire a blade of wind, piercing through enemies";
            case LIGHTNING: return "Teleport forward, leaving a lightning trail";
            case ICE: return "Create a blizzard, damaging and slowing all inside";
            case NATURE: return "Grow thorns, damaging and knocking back enemies";
            case SHADOW: return "Teleport and leave a cloud of darkness";
            case LIGHT: return "Create a dome of light, healing and protecting allies";
            default: return "Unknown";
        }
    }

    private String getPassiveEffects(CoreType type) {
        switch (type) {
            case EARTH: return "Resistance II, Haste I";
            case WATER: return "Water Breathing, Dolphin's Grace";
            case FIRE: return "Fire Resistance, Strength I";
            case AIR: return "Jump Boost II, Slow Falling";
            case LIGHTNING: return "Speed II, Night Vision";
            case ICE: return "Resistance to Slowness, Frost Walker";
            case NATURE: return "Regeneration II, Saturation";
            case SHADOW: return "Night Vision, Invisibility (short bursts)";
            case LIGHT: return "Glowing, Absorption I";
            default: return "Unknown";
        }
    }

    private String getTierDescription(int tier) {
        switch (tier) {
            case 1: return "Basic elemental power";
            case 2: return "Enhanced elemental mastery";
            case 3: return "Legendary elemental force";
            default: return "Unknown tier";
        }
    }
}
