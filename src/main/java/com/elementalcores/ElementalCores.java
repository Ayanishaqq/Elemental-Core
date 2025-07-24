package com.elementalcores;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.World;

import java.util.*;

public class ElementalCores extends JavaPlugin implements Listener {
    
    private HashMap<UUID, HashMap<String, Long>> cooldowns = new HashMap<>();
    private HashMap<UUID, String> activeCore = new HashMap<>();
    private Set<UUID> firstJoinPlayers = new HashSet<>();
    private NamespacedKey coreKey;
    private NamespacedKey tierKey;
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        coreKey = new NamespacedKey(this, "elemental_core");
        tierKey = new NamespacedKey(this, "core_tier");
        
        // Load config
        saveDefaultConfig();
        
        // Start passive effects checker
        startPassiveEffects();
        
        getLogger().info("Elemental Cores Plugin Enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Elemental Cores Plugin Disabled!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("core")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Usage: /core <give|info|reload>");
                return true;
            }
            
            if (args[0].equalsIgnoreCase("info")) {
                showCoreInfo(sender);
                return true;
            }
            
            if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("elementalcores.admin")) {
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Elemental Cores config reloaded!");
                return true;
            }
            
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("elementalcores.admin")) {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /core give <core> <player> <tier>");
                    return true;
                }
                
                String coreName = args[1].toLowerCase();
                Player target = getServer().getPlayer(args[2]);
                int tier = Integer.parseInt(args[3]);
                
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                
                giveCore(target, coreName, tier);
                sender.sendMessage(ChatColor.GREEN + "Gave " + coreName + " core (Tier " + tier + ") to " + target.getName());
                return true;
            }
        }
        return false;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPlayedBefore()) {
            firstJoinPlayers.add(player.getUniqueId());
            playFirstJoinAnimation(player);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInOffHand();
        
        if (item == null || item.getType() != Material.PAPER) return;
        if (!item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(coreKey, PersistentDataType.STRING)) return;
        
        String coreName = meta.getPersistentDataContainer().get(coreKey, PersistentDataType.STRING);
        
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            
            if (player.isSneaking()) {
                useShiftAbility(player, coreName);
            } else {
                useNormalAbility(player, coreName);
            }
        }
    }
    
    private void playFirstJoinAnimation(Player player) {
        String[] coreNames = {
            ChatColor.GOLD + "Earth",
            ChatColor.AQUA + "Water",
            ChatColor.RED + "Fire",
            ChatColor.WHITE + "Air",
            ChatColor.YELLOW + "Lightning",
            ChatColor.BLUE + "Ice",
            ChatColor.GREEN + "Nature",
            ChatColor.DARK_PURPLE + "Shadow",
            ChatColor.LIGHT_PURPLE + "Light"
        };
        
        new BukkitRunnable() {
            int ticks = 0;
            int index = 0;
            
            @Override
            public void run() {
                if (ticks >= 140) { // 7 seconds
                    this.cancel();
                    giveRandomCore(player);
                    firstJoinPlayers.remove(player.getUniqueId());
                    return;
                }
                
                player.sendTitle(coreNames[index], ChatColor.GRAY + "Elemental Core", 0, 20, 0);
                index = (index + 1) % coreNames.length;
                ticks += 10;
            }
        }.runTaskTimer(this, 0, 10);
    }
    
    private void giveRandomCore(Player player) {
        String[] cores = {"earth", "water", "fire", "air", "lightning", "ice", "nature", "shadow", "light"};
        String randomCore = cores[new Random().nextInt(cores.length)];
        giveCore(player, randomCore, 1);
    }
    
    private void giveCore(Player player, String coreName, int tier) {
        // Remove any existing cores
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.PAPER && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.getPersistentDataContainer().has(coreKey, PersistentDataType.STRING)) {
                    player.getInventory().remove(item);
                }
            }
        }
        
        ItemStack core = new ItemStack(Material.PAPER);
        ItemMeta meta = core.getItemMeta();
        
        // Set display name and lore based on core type
        switch (coreName.toLowerCase()) {
            case "earth":
                meta.setDisplayName(ChatColor.GOLD + "Earth Core " + ChatColor.GRAY + "(Tier " + tier + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Earth Fortress",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Earthquake",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Resistance III, Haste II",
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "water":
                meta.setDisplayName(ChatColor.AQUA + "Water Core " + ChatColor.GRAY + "(Tier " + tier + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Tidal Wave",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Maelstrom",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Water Breathing, Dolphin's Grace II",
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "fire":
                meta.setDisplayName(ChatColor.RED + "Fire Core " + ChatColor.GRAY + "(Tier " + tier + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Inferno Meteor",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Ring of Fire",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Fire Resistance, Strength II",
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "air":
                meta.setDisplayName(ChatColor.WHITE + "Air Core " + ChatColor.GRAY + "(Tier " + tier + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Hurricane Leap",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Cyclone",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Jump Boost III, Slow Falling",
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "lightning":
                meta.setDisplayName(ChatColor.YELLOW + "Lightning Core " + ChatColor.GRAY + "(Tier " + tier + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Thunderstorm",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Lightning Dash",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Speed III, Night Vision",
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "ice":
                meta.setDisplayName(ChatColor.BLUE + "Ice Core " + ChatColor.GRAY + "(Tier " + tier + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Glacial Prison",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Blizzard",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Resistance II, Frost Walker II",
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "nature":
                meta.setDisplayName(ChatColor.GREEN + "Nature Core " + ChatColor.GRAY + "(Tier " + tier + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Nature's Embrace",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Thornfield",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Regeneration II, Saturation",
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "shadow":
                meta.setDisplayName(ChatColor.DARK_PURPLE + "Shadow Core " + ChatColor.GRAY + "(Tier " + tier + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Shadow Clone Army",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Void Step",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Night Vision, Invisibility",
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "light":
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Light Core " + ChatColor.GRAY + "(Tier " + tier + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Solar Flare",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Sanctuary",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Glowing, Absorption II",
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
        }
        
        // Add NBT data
        meta.getPersistentDataContainer().set(coreKey, PersistentDataType.STRING, coreName.toLowerCase());
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, tier);
        
        core.setItemMeta(meta);
        player.getInventory().addItem(core);
        player.sendMessage(ChatColor.GREEN + "You received a " + meta.getDisplayName() + ChatColor.GREEN + "!");
    }
    
    private void startPassiveEffects() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    ItemStack offhand = player.getInventory().getItemInOffHand();
                    
                    if (offhand != null && offhand.getType() == Material.PAPER && offhand.hasItemMeta()) {
                        ItemMeta meta = offhand.getItemMeta();
                        if (meta.getPersistentDataContainer().has(coreKey, PersistentDataType.STRING)) {
                            String coreName = meta.getPersistentDataContainer().get(coreKey, PersistentDataType.STRING);
                            applyPassiveEffects(player, coreName);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 20); // Run every second
    }
    
    private void applyPassiveEffects(Player player, String coreName) {
        switch (coreName.toLowerCase()) {
            case "earth":
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 40, 2, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 40, 1, true, false));
                break;
            case "water":
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 40, 0, true, false));
                player.addPotionEffect(new Pot
