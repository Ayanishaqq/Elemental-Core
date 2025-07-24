package com.elementalcores;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class CoreManager {
    private final ElementalCores plugin;
    private final NamespacedKey coreTypeKey = new NamespacedKey("elementalcores", "core_type");
    private final NamespacedKey coreTierKey = new NamespacedKey("elementalcores", "core_tier");
    private final Map<UUID, Long> cooldowns = new HashMap<>(); // For abilities

    public CoreManager(ElementalCores plugin) {
        this.plugin = plugin;
    }

    public ItemStack createCore(CoreType type, int tier) {
        ItemStack core = new ItemStack(Material.PAPER);
        ItemMeta meta = core.getItemMeta();
        meta.setCustomModelData(type.getCustomModelData());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('#', type.getColorHex() + type.getName() + " Core (Tier " + tier + ")"));
        // Add lore (as per spec)
        List<String> lore = new ArrayList<>();
        lore.add("A core of " + type.getName().toLowerCase() + " power.");
        lore.add("Right Click: " + getRightClickAbilityName(type));
        lore.add("Shift+Right Click: " + getShiftRightClickAbilityName(type));
        lore.add("Passive: " + type.getPassive1().getName() + ", " + type.getPassive2().getName());
        lore.add("Tier " + tier + ": " + getTierDescription(tier));
        meta.setLore(lore);
        // NBT
        meta.getPersistentDataContainer().set(coreTypeKey, PersistentDataType.STRING, type.name().toLowerCase());
        meta.getPersistentDataContainer().set(coreTierKey, PersistentDataType.INTEGER, tier);
        core.setItemMeta(meta);
        core.setAmount(1); // Non-stackable
        return core;
    }

    public ItemStack createHeavyCore() {
        ItemStack heavy = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = heavy.getItemMeta();
        meta.setCustomModelData(1100);
        meta.setDisplayName(ChatColor.DARK_GRAY + "Heavy Core");
        heavy.setItemMeta(meta);
        return heavy;
    }

    public ItemStack createElementCaller() {
        ItemStack caller = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = caller.getItemMeta();
        meta.setCustomModelData(2001);
        meta.setDisplayName(ChatColor.AQUA + "Element Caller");
        List<String> lore = new ArrayList<>();
        lore.add("Right-click to summon the elements and receive a new core!");
        meta.setLore(lore);
        caller.setItemMeta(meta);
        return caller;
    }

    public void giveRandomCore(Player player) {
        // Remove existing core
        removeExistingCore(player);
        // Animation
        playAnimation(player);
        // Give random core after 7 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                CoreType randomType = CoreType.values()[new Random().nextInt(CoreType.values().length)];
                ItemStack core = createCore(randomType, 1); // Start at T1
                player.getInventory().addItem(core);
                player.sendMessage(ChatColor.GREEN + "You received a " + randomType.getName() + " Core!");
            }
        }.runTaskLater(plugin, 140L); // 7 seconds (20 ticks/second)
    }

    private void removeExistingCore(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isCore(item)) {
                player.getInventory().remove(item);
            }
        }
    }

    public boolean isCore(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(coreTypeKey, PersistentDataType.STRING) &&
               meta.getPersistentDataContainer().has(coreTierKey, PersistentDataType.INTEGER);
    }

    public CoreType getCoreType(ItemStack item) {
        String typeStr = item.getItemMeta().getPersistentDataContainer().get(coreTypeKey, PersistentDataType.STRING);
        return CoreType.valueOf(typeStr.toUpperCase());
    }

    public int getCoreTier(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().get(coreTierKey, PersistentDataType.INTEGER);
    }

    private void playAnimation(Player player) {
        Location loc = player.getLocation();
        new BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time >= 140) { // 7 seconds
                    cancel();
                    return;
                }
                // Rotate 9 elemental items/particles around player
                for (int i = 0; i < CoreType.values().length; i++) {
                    CoreType type = CoreType.values()[i];
                    double angle = (2 * Math.PI * i / 9) + (time * 0.1); // Rotate
                    Location particleLoc = loc.clone().add(Math.cos(angle) * 2, 1 + Math.sin(time * 0.05), Math.sin(angle) * 2);
                    player.getWorld().spawnParticle(type.getParticle(), particleLoc, 5);
                    player.getWorld().playSound(particleLoc, type.getSound(), 1.0f, 1.0f);
                    // Spawn falling block for visual (disappears quickly)
                    player.getWorld().spawnFallingBlock(particleLoc, type.getAnimationItem().createBlockData());
                }
                time += 20; // Every second
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // Cooldown check
    public boolean isOnCooldown(Player player, boolean isShift) {
        long current = System.currentTimeMillis();
        long lastUse = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        int cooldown = getCooldown(isShift, getCoreTier(player.getInventory().getItemInMainHand()));
        if (current - lastUse < cooldown * 1000) {
            player.sendMessage(ChatColor.RED + "Ability on cooldown!");
            return true;
        }
        cooldowns.put(player.getUniqueId(), current);
        return false;
    }

    private int getCooldown(boolean isShift, int tier) {
        if (isShift) {
            return switch (tier) { case 1 -> 15; case 2 -> 12; default -> 10; };
        } else {
            return switch (tier) { case 1 -> 10; case 2 -> 8; default -> 6; };
        }
    }

    private String getRightClickAbilityName(CoreType type) {
        return switch (type) {
            case EARTH -> "Earth Prison";
            case WATER -> "Tsunami Surge";
            case FIRE -> "Meteor Strike";
            case AIR -> "Tornado Launch";
            case LIGHTNING -> "Stormcall";
            case ICE -> "Glacial Spike";
            case NATURE -> "Entangling Roots";
            case SHADOW -> "Shadow Clone";
            case LIGHT -> "Solar Beam";
        };
    }

    private String getShiftRightClickAbilityName(CoreType type) {
        return switch (type) {
            case EARTH -> "Seismic Slam";
            case WATER -> "Aqua Vortex";
            case FIRE -> "Infernal Eruption";
            case AIR -> "Wind Blade";
            case LIGHTNING -> "Lightning Dash";
            case ICE -> "Blizzard";
            case NATURE -> "Nature’s Wrath";
            case SHADOW -> "Void Step";
            case LIGHT -> "Sanctuary";
        };
    }

    private String getTierDescription(int tier) {
        return switch (tier) {
            case 1 -> "Basic elemental power.";
            case 2 -> "Stronger, larger, longer.";
            default -> "Legendary elemental force.";
        };
    }
}
