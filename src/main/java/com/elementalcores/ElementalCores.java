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
import org.bukkit.enchantments.Enchantment;

import java.util.*;

public class ElementalCores extends JavaPlugin implements Listener {
    
    private HashMap<UUID, HashMap<String, Long>> cooldowns = new HashMap<>();
    private HashMap<UUID, String> activeCore = new HashMap<>();
    private Set<UUID> firstJoinPlayers = new HashSet<>();
    private NamespacedKey coreKey;
    private NamespacedKey tierKey;
    private RecipeManager recipeManager;
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        coreKey = new NamespacedKey(this, "elemental_core");
        tierKey = new NamespacedKey(this, "core_tier");
        
        // Load config
        saveDefaultConfig();
        
        // Register recipes
        recipeManager = new RecipeManager(this);
        recipeManager.registerRecipes();
        
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
                int tier;
                
                try {
                    tier = Integer.parseInt(args[3]);
                    if (tier < 1 || tier > 3) {
                        sender.sendMessage(ChatColor.RED + "Tier must be between 1 and 3!");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Tier must be a number!");
                    return true;
                }
                
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                
                String[] validCores = {"earth", "water", "fire", "air", "lightning", "ice", "nature", "shadow", "light"};
                if (!Arrays.asList(validCores).contains(coreName)) {
                    sender.sendMessage(ChatColor.RED + "Invalid core type! Valid types: earth, water, fire, air, lightning, ice, nature, shadow, light");
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
        
        ItemStack core = createCoreItem(coreName, tier);
        player.getInventory().addItem(core);
        player.sendMessage(ChatColor.GREEN + "You received a " + core.getItemMeta().getDisplayName() + ChatColor.GREEN + "!");
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
                            int tier = meta.getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
                            applyPassiveEffects(player, coreName, tier);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 20); // Run every second
    }
    
    private void applyPassiveEffects(Player player, String coreName, int tier) {
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        int amplifier = (int) multiplier - 1;
        if (amplifier < 0) amplifier = 0;
        
        switch (coreName.toLowerCase()) {
            case "earth":
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 40, 2 + amplifier, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 40, 1 + amplifier, true, false));
                break;
            case "water":
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 40, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 1 + amplifier, true, false));
                break;
            case "fire":
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 1 + amplifier, true, false));
                break;
            case "air":
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 40, 2 + amplifier, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0, true, false));
                break;
            case "lightning":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2 + amplifier, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 40, 0, true, false));
                break;
            case "ice":
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 40, 1 + amplifier, true, false));
                // Frost Walker is an enchantment, not a potion effect
                break;
            case "nature":
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1 + amplifier, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, 0, true, false));
                break;
            case "shadow":
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 40, 0, true, false));
                // Add invisibility in bursts
                if (player.getWorld().getTime() % 100 == 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, true, false));
                }
                break;
            case "light":
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 40, 1 + amplifier, true, false));
                break;
        }
    }
    
    private boolean isOnCooldown(Player player, String ability) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        
        HashMap<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (!playerCooldowns.containsKey(ability)) {
            return false;
        }
        
        long cooldownEnd = playerCooldowns.get(ability);
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    private void setCooldown(Player player, String ability, int seconds) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            cooldowns.put(player.getUniqueId(), new HashMap<>());
        }
        
        cooldowns.get(player.getUniqueId()).put(ability, System.currentTimeMillis() + (seconds * 1000));
    }
    
    private int getCooldownLeft(Player player, String ability) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }
        
        HashMap<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (!playerCooldowns.containsKey(ability)) {
            return 0;
        }
        
        long cooldownEnd = playerCooldowns.get(ability);
        return (int) ((cooldownEnd - System.currentTimeMillis()) / 1000);
    }
    
    private void useNormalAbility(Player player, String coreName) {
        String abilityName = "";
        int cooldown = getConfig().getInt("cooldowns.normal", 12);
        ItemStack offhand = player.getInventory().getItemInOffHand();
        int tier = 1;
        
        if (offhand != null && offhand.hasItemMeta()) {
            ItemMeta meta = offhand.getItemMeta();
            if (meta.getPersistentDataContainer().has(tierKey, PersistentDataType.INTEGER)) {
                tier = meta.getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
            }
        }
        
        switch (coreName.toLowerCase()) {
            case "earth":
                abilityName = "Earth Fortress";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                earthFortress(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "water":
                abilityName = "Tidal Wave";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                tidalWave(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "fire":
                abilityName = "Inferno Meteor";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                infernoMeteor(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "air":
                abilityName = "Hurricane Leap";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                hurricaneLeap(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "lightning":
                abilityName = "Thunderstorm";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                thunderstorm(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "ice":
                abilityName = "Glacial Prison";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                glacialPrison(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "nature":
                abilityName = "Nature's Embrace";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                naturesEmbrace(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "shadow":
                abilityName = "Shadow Clone Army";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                shadowCloneArmy(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "light":
                abilityName = "Solar Flare";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                solarFlare(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
        }
    }
    
    private void useShiftAbility(Player player, String coreName) {
        String abilityName = "";
        int cooldown = getConfig().getInt("cooldowns.shift", 18);
        ItemStack offhand = player.getInventory().getItemInOffHand();
        int tier = 1;
        
        if (offhand != null && offhand.hasItemMeta()) {
            ItemMeta meta = offhand.getItemMeta();
            if (meta.getPersistentDataContainer().has(tierKey, PersistentDataType.INTEGER)) {
                tier = meta.getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
            }
        }
        
        switch (coreName.toLowerCase()) {
            case "earth":
                abilityName = "Earthquake";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                earthquake(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "water":
                abilityName = "Maelstrom";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                maelstrom(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "fire":
                abilityName = "Ring of Fire";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                ringOfFire(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "air":
                abilityName = "Cyclone";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                cyclone(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "lightning":
                abilityName = "Lightning Dash";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                lightningDash(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "ice":
                abilityName = "Blizzard";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                blizzard(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "nature":
                abilityName = "Thornfield";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                thornfield(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "shadow":
                abilityName = "Void Step";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                voidStep(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "light":
                abilityName = "Sanctuary";
                if (isOnCooldown(player, abilityName)) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                sanctuary(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
        }
    }
    
    // ABILITY IMPLEMENTATIONS
    
    // Earth Abilities
    private void earthFortress(Player player, int tier) {
        Location center = player.getLocation();
        List<Block> placedBlocks = new ArrayList<>();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 6.0 * multiplier;
        
        // Create walls
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 3; y++) {
                    if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                        Location blockLoc = center.clone().add(x, y, z);
                        Block block = blockLoc.getBlock();
                        if (block.getType() == Material.AIR) {
                            if (y == 0 || (Math.abs(x) == 2 && Math.abs(z) == 2)) {
                                block.setType(Material.OBSIDIAN);
                            } else {
                                block.setType(Material.STONE);
                            }
                            placedBlocks.add(block);
                        }
                    }
                }
            }
        }
        
        // Launch and damage enemies inside
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                if (canPvP(player, living)) {
                    living.damage(damage, player);
                    living.setVelocity(new Vector(0, 1.5, 0));
                }
            }
        }
        
        player.getWorld().playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, center, 100, 2, 2, 2, Material.STONE.createBlockData());
        
        // Remove blocks after 7 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : placedBlocks) {
                    block.setType(Material.AIR);
                    block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation(), 10, 0.5, 0.5, 0.5, block.getBlockData());
                }
            }
        }.runTaskLater(this, 140); // 7 seconds
    }
    
    private void earthquake(Player player, int tier) {
        Location center = player.getLocation();
        World world = player.getWorld();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 8.0 * multiplier;
        
        // Shake effect and damage
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                if (canPvP(player, living)) {
                    living.damage(damage, player);
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2));
                    
                    // Shake effect
                    Location loc = living.getLocation();
                    living.teleport(loc.add(Math.random() * 0.5 - 0.25, 0.1, Math.random() * 0.5 - 0.25));
                }
            }
        }
        
        // Break grass, leaves, and flowers
        for (int x = -10; x <= 10; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -10; z <= 10; z++) {
                    if (Math.sqrt(x*x + z*z) <= 10) {
                        Block block = center.clone().add(x, y, z).getBlock();
                        Material type = block.getType();
                        if (type == Material.GRASS || type == Material.TALL_GRASS || 
                            type.name().contains("LEAVES") || type.name().contains("FLOWER")) {
                            block.breakNaturally();
                        }
                    }
                }
            }
        }
        
        world.playSound(center, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 2.0f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION_LARGE, center, 20, 5, 1, 5);
    }
    
    // Water Abilities
    private void tidalWave(Player player, int tier) {
        Location start = player.getLocation();
        Vector direction = player.getLocation().getDirection().normalize();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 6.0 * multiplier;
        
        new BukkitRunnable() {
            int distance = 0;
            
            @Override
            public void run() {
                if (distance >= 15) {
                    this.cancel();
                    return;
                }
                
                Location waveLoc = start.clone().add(direction.clone().multiply(distance));
                
                // Create wave effect
                for (int y = 0; y <= 3; y++) {
                    for (int x = -2; x <= 2; x++) {
                        Location particleLoc = waveLoc.clone().add(
                            direction.clone().rotateAroundY(Math.PI/2).multiply(x)
                        ).add(0, y, 0);
                        
                        player.getWorld().spawnParticle(Particle.WATER_SPLASH, particleLoc, 10, 0.5, 0.5, 0.5);
                    }
                }
                
                // Damage and push entities
                for (Entity entity : waveLoc.getWorld().getNearbyEntities(waveLoc, 3, 3, 3)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity living = (LivingEntity) entity;
                        if (canPvP(player, living)) {
                            living.damage(damage, player);
                            living.setVelocity(direction.clone().multiply(1.5).setY(0.5));
                        }
                    }
                }
                
                // Extinguish fire and lava
                for (int x = -3; x <= 3; x++) {
                    for (int y = -1; y <= 3; y++) {
                        for (int z = -3; z <= 3; z++) {
                            Block block = waveLoc.clone().add(x, y, z).getBlock();
                            if (block.getType() == Material.FIRE) {
                                block.setType(Material.AIR);
                            } else if (block.getType() == Material.LAVA) {
                                block.setType(Material.OBSIDIAN);
                            }
                        }
                    }
                }
                
                waveLoc.getWorld().playSound(waveLoc, Sound.ENTITY_PLAYER_SPLASH, 1.0f, 1.0f);
                distance += 2;
            }
        }.runTaskTimer(this, 0, 2);
    }
    
    private void maelstrom(Player player, int tier) {
        Location center = player.getLocation();
        List<Entity> trapped = new ArrayList<>();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 2.0 * multiplier;
        
        // Pull entities
        for (Entity entity : player.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof LivingEntity && entity != player) {
                if (canPvP(player, (LivingEntity) entity)) {
                    trapped.add(entity);
                }
            }
        }
        
        // Give player water breathing
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 100, 0));
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 80) { // 4 seconds
                    this.cancel();
                    return;
                }
                
                // Create vortex effect
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = Math.cos(angle + ticks * 0.1) * 5;
                    double z = Math.sin(angle + ticks * 0.1) * 5;
                    Location particleLoc = center.clone().add(x, ticks * 0.1, z);
                    player.getWorld().spawnParticle(Particle.WATER_BUBBLE, particleLoc, 5, 0.2, 0.2, 0.2);
                }
                
                // Pull and damage trapped entities
                for (Entity entity : trapped) {
                    if (entity.isValid()) {
                        Vector pull = center.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.3);
                        entity.setVelocity(pull);
                        
                        if (ticks % 20 == 0) { // Damage every second
                            ((LivingEntity) entity).damage(damage, player);
                        }
                    }
                }
                
                center.getWorld().playSound(center, Sound.ENTITY_PLAYER_SWIM, 1.0f, 0.5f);
                ticks += 2;
            }
        }.runTaskTimer(this, 0, 2);
    }
    
    // Fire Abilities
    private void infernoMeteor(Player player, int tier) {
        Location target = player.getTargetBlock(null, 30).getLocation();
        Location spawn = target.clone().add(0, 20, 0);
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 10.0 * multiplier;
        
        Fireball meteor = player.getWorld().spawn(spawn, Fireball.class);
        meteor.setDirection(new Vector(0, -1, 0));
        meteor.setYield(3.0f);
        meteor.setIsIncendiary(true);
        meteor.setShooter(player);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!meteor.isValid() || meteor.getLocation().getY() <= target.getY()) {
                    this.cancel();
                    
                    // Explosion effects
                    Location impact = meteor.getLocation();
                    impact.getWorld().createExplosion(impact, 3.0f, true, false, player);
                    
                    // Damage and ignite
                    for (Entity entity : impact.getWorld().getNearbyEntities(impact, 5, 5, 5)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity living = (LivingEntity) entity;
                            if (canPvP(player, living)) {
                                living.damage(damage, player);
                                living.setFireTicks(140); // 7 seconds
                            }
                        }
                    }
                    
                    // Ignite ground
                    for (int x = -3; x <= 3; x++) {
                        for (int z = -3; z <= 3; z++) {
                            Block block = impact.clone().add(x, 1, z).getBlock();
                            if (block.getType() == Material.AIR && block.getRelative(0, -1, 0).getType().isSolid()) {
                                block.setType(Material.FIRE);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 1);
    }
    
    private void ringOfFire(Player player, int tier) {
        Location center = player.getLocation();
        List<Location> fireLocations = new ArrayList<>();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 1.0 * multiplier;
        
        // Create ring
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            double x = Math.cos(angle) * 7;
            double z = Math.sin(angle) * 7;
            Location fireLoc = center.clone().add(x, 0, z);
            
            // Find ground level
            while (fireLoc.getBlock().getType() == Material.AIR && fireLoc.getY() > center.getY() - 5) {
                fireLoc.subtract(0, 1, 0);
            }
            fireLoc.add(0, 1, 0);
            
            if (fireLoc.getBlock().getType() == Material.AIR) {
                fireLoc.getBlock().setType(Material.FIRE);
                fireLocations.add(fireLoc);
            }
        }
        
        center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 2.0f, 1.0f);
        
        // Damage entities inside ring
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 100) { // 5 seconds
                    this.cancel();
                    // Remove fire
                    for (Location loc : fireLocations) {
                        if (loc.getBlock().getType() == Material.FIRE) {
                            loc.getBlock().setType(Material.AIR);
                        }
                    }
                    return;
                }
                
                // Check for entities inside ring
                for (Entity entity : center.getWorld().getNearbyEntities(center, 7, 3, 7)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        double distance = entity.getLocation().distance(center);
                        if (distance <= 7) {
                            LivingEntity living = (LivingEntity) entity;
                            if (canPvP(player, living)) {
                                living.damage(damage, player);
                                living.setFireTicks(20);
                            }
                        }
                    }
                }
                
                ticks += 10;
            }
        }.runTaskTimer(this, 0, 10);
    }
    
    // Air Abilities
    private void hurricaneLeap(Player player, int tier) {
        player.setVelocity(new Vector(0, 3, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0));
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 8.0 * multiplier;
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 50, 1, 1, 1);
        
        // Check for landing
        new BukkitRunnable() {
            Location lastLoc = player.getLocation();
            
            @Override
            public void run() {
                if (player.isOnGround() && player.getLocation().getY() < lastLoc.getY()) {
                    this.cancel();
                    
                    // Landing impact
                    Location impact = player.getLocation();
                    impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                    impact.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, impact, 10, 2, 0, 2);
                    
                    // Damage and knockback
                    for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity living = (LivingEntity) entity;
                            if (canPvP(player, living)) {
                                living.damage(damage, player);
                                Vector knockback = living.getLocation().toVector().subtract(impact.toVector()).normalize().multiply(2).setY(0.5);
                                living.setVelocity(knockback);
                            }
                        }
                    }
                }
                lastLoc = player.getLocation();
            }
        }.runTaskTimer(this, 10, 1);
    }
    
    private void cyclone(Player player, int tier) {
        Location center = player.getLocation();
        List<Entity> lifted = new ArrayList<>();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 2.0 * multiplier; // Fall damage multiplier
        
        // Lift entities
        for (Entity entity : player.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof LivingEntity && entity != player) {
                if (canPvP(player, (LivingEntity) entity)) {
                    lifted.add(entity);
                    entity.setVelocity(new Vector(0, 2, 0));
                }
            }
        }
        
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.5f);
        
        // Cyclone effect
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 60) { // 3 seconds
                    this.cancel();
                    
                    // Drop entities
                    for (Entity entity : lifted) {
                        if (entity.isValid()) {
                            entity.setVelocity(new Vector(0, -0.5, 0));
                        }
                    }
                    return;
                }
                
                // Particle effect
                for (double y = 0; y <= 10; y += 0.5) {
                    double radius = (10 - y) * 0.5;
                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
                        double x = Math.cos(angle + ticks * 0.2) * radius;
                        double z = Math.sin(angle + ticks * 0.2) * radius;
                        Location particleLoc = center.clone().add(x, y, z);
                        center.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0, 0);
                    }
                }
                
                // Keep entities in air
                for (Entity entity : lifted) {
                    if (entity.isValid() && entity.getLocation().getY() < center.getY() + 8) {
                        entity.setVelocity(entity.getVelocity().add(new Vector(0, 0.3, 0)));
                    }
                }
                
                ticks += 2;
            }
        }.runTaskTimer(this, 0, 2);
    }
    
    // Lightning Abilities
    private void thunderstorm(Player player, int tier) {
        List<Entity> targets = new ArrayList<>();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 8.0 * multiplier;
        
        // Find targets
        for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
            if (entity instanceof LivingEntity && entity != player) {
                if (canPvP(player, (LivingEntity) entity)) {
                    targets.add(entity);
                }
            }
        }
        
        // Strike up to 7 random targets
        Collections.shuffle(targets);
        int strikes = Math.min(7, targets.size());
        
        for (int i = 0; i < strikes; i++) {
            Entity target = targets.get(i);
            
            // Strike with delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (target.isValid()) {
                        target.getWorld().strikeLightningEffect(target.getLocation());
                        ((LivingEntity) target).damage(damage, player);
                        
                        // Stun effect (slowness and weakness)
                        ((LivingEntity) target).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 10));
                        ((LivingEntity) target).addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 10));
                    }
                }
            }.runTaskLater(this, i * 4); // Stagger the strikes
        }
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
    }
    
    private void lightningDash(Player player, int tier) {
        Location start = player.getLocation();
        Vector direction = player.getLocation().getDirection().normalize();
        Location end = start.clone().add(direction.multiply(20));
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 6.0 * multiplier;
        
        // Check for safe landing
        while (!end.getBlock().getType().isSolid() && end.getY() > 0) {
            end.subtract(0, 1, 0);
        }
        end.add(0, 1, 0);
        
        // Create lightning trail
        Vector step = direction.normalize().multiply(0.5);
        Location current = start.clone();
        
        for (int i = 0; i < 40; i++) {
            current.add(step);
            current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, current, 10, 0.2, 0.2, 0.2);
            
            // Damage entities along the path
            for (Entity entity : current.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity living = (LivingEntity) entity;
                    if (canPvP(player, living)) {
                        living.damage(damage, player);
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 10));
                        current.getWorld().strikeLightningEffect(living.getLocation());
                    }
                }
            }
        }
        
        // Teleport player
        player.teleport(end);
        player.getWorld().playSound(start, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
        player.getWorld().playSound(end, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
    }
    
    // Ice Abilities
    private void glacialPrison(Player player, int tier) {
        List<Entity> frozen = new ArrayList<>();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 6.0 * multiplier;
        
        for (Entity entity : player.getNearbyEntities(7, 7, 7)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                if (canPvP(player, living)) {
                    frozen.add(living);
                    
                    // Freeze in ice
                    Location loc = living.getLocation();
                    for (int x = -1; x <= 1; x++) {
                        for (int y = 0; y <= 2; y++) {
                            for (int z = -1; z <= 1; z++) {
                                if (Math.abs(x) == 1 || Math.abs(z) == 1 || y == 0 || y == 2) {
                                    Block block = loc.clone().add(x, y, z).getBlock();
                                    if (block.getType() == Material.AIR) {
                                        block.setType(Material.ICE);
                                    }
                                }
                            }
                        }
                    }
                    
                    // Immobilize
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 255));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100, 128));
                }
            }
        }
        
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 2.0f, 2.0f);
        player.getWorld().spawnParticle(Particle.SNOW_SHOVEL, player.getLocation(), 100, 3, 3, 3);
        
        // Thaw after 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity entity : frozen) {
                    if (entity.isValid()) {
                        Location loc = entity.getLocation();
                        
                        // Remove ice
                        for (int x = -1; x <= 1; x++) {
                            for (int y = 0; y <= 2; y++) {
                                for (int z = -1; z <= 1; z++) {
                                    Block block = loc.clone().add(x, y, z).getBlock();
                                    if (block.getType() == Material.ICE) {
                                        block.setType(Material.AIR);
                                        block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation(), 10, 0.5, 0.5, 0.5, Material.ICE.createBlockData());
                                    }
                                }
                            }
                        }
                        
                        // Damage on thaw
                        ((LivingEntity) entity).damage(damage, player);
                    }
                }
            }
        }.runTaskLater(this, 100); // 5 seconds
    }
    
    private void blizzard(Player player, int tier) {
        Location center = player.getLocation();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 2.0 * multiplier;
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 140) { // 7 seconds
                    this.cancel();
                    return;
                }
                
                // Snow particles
                for (int i = 0; i < 50; i++) {
                    double x = (Math.random() - 0.5) * 20;
                    double y = Math.random() * 5;
                    double z = (Math.random() - 0.5) * 20;
                    Location particleLoc = center.clone().add(x, y, z);
                    center.getWorld().spawnParticle(Particle.SNOWBALL, particleLoc, 1, 0, -0.5, 0);
                }
                
                // Damage and slow enemies
                if (ticks % 20 == 0) { // Every second
                    for (Entity entity : center.getWorld().getNearbyEntities(center, 10, 5, 10)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity living = (LivingEntity) entity;
                            if (canPvP(player, living)) {
                                living.damage(damage, player);
                                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 3));
                            }
                        }
                    }
                }
                
                if (ticks % 10 == 0) {
                    center.getWorld().playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);
                }
                
                ticks += 2;
            }
        }.runTaskTimer(this, 0, 2);
    }
    
    // Nature Abilities
    private void naturesEmbrace(Player player, int tier) {
        Location center = player.getLocation();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double healing = 16.0 * multiplier;
        
        // Heal and cleanse effects
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Player) {
                Player ally = (Player) entity;
                ally.setHealth(Math.min(ally.getHealth() + healing, ally.getMaxHealth()));
                
                // Remove negative effects
                for (PotionEffect effect : ally.getActivePotionEffects()) {
                    PotionEffectType type = effect.getType();
                    if (type == PotionEffectType.POISON || type == PotionEffectType.WITHER ||
                        type == PotionEffectType.SLOW || type == PotionEffectType.WEAKNESS ||
                        type == PotionEffectType.BLINDNESS || type == PotionEffectType.CONFUSION) {
                        ally.removePotionEffect(type);
                    }
                }
                
                ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5);
            }
        }
        
        // Heal self
        player.setHealth(Math.min(player.getHealth() + healing, player.getMaxHealth()));
        
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        center.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, center, 100, 5, 2, 5);
    }
    
    private void thornfield(Player player, int tier) {
        Location center = player.getLocation();
        List<Entity> rooted = new ArrayList<>();
        List<Location> thornLocations = new ArrayList<>();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 2.0 * multiplier;
        
        // Create thorn field
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                if (Math.sqrt(x*x + z*z) <= 7) {
                    Location thornLoc = center.clone().add(x, 0, z);
                    
                    // Find ground
                    while (thornLoc.getBlock().getType() == Material.AIR && thornLoc.getY() > center.getY() - 5) {
                        thornLoc.subtract(0, 1, 0);
                    }
                    
                    if (Math.random() < 0.3) { // 30% chance for visual variety
                        thornLocations.add(thornLoc.add(0, 1, 0));
                    }
                }
            }
        }
        
        // Root enemies
        for (Entity entity : center.getWorld().getNearbyEntities(center, 7, 3, 7)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                if (canPvP(player, living)) {
                    rooted.add(living);
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 255));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 80, 128));
                }
            }
        }
        
        center.getWorld().playSound(center, Sound.BLOCK_GRASS_BREAK, 2.0f, 0.5f);
        
        // Damage over time
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 80) { // 4 seconds
                    this.cancel();
                    return;
                }
                
                // Show thorns
                for (Location loc : thornLocations) {
                    loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 2, 0.2, 0.5, 0.2, Material.OAK_LEAVES.createBlockData());
                }
                
                // Damage rooted enemies
                if (ticks % 20 == 0) { // Every second
                    for (Entity entity : rooted) {
                        if (entity.isValid()) {
                            ((LivingEntity) entity).damage(damage, player);
                            entity.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, entity.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
                        }
                    }
                }
                
                ticks += 5;
            }
        }.runTaskTimer(this, 0, 5);
    }
    
    // Shadow Abilities
    private void shadowCloneArmy(Player player, int tier) {
        List<Entity> clones = new ArrayList<>();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 4.0 * multiplier;
        
        // Spawn 5 clones
        for (int i = 0; i < 5; i++) {
            double angle = (Math.PI * 2 / 5) * i;
            double x = Math.cos(angle) * 3;
            double z = Math.sin(angle) * 3;
            Location spawnLoc = player.getLocation().add(x, 0, z);
            
            Zombie clone = player.getWorld().spawn(spawnLoc, Zombie.class);
            clone.setCustomName(ChatColor.DARK_PURPLE + "Shadow Clone");
            clone.setCustomNameVisible(true);
            clone.setBaby(false);
            clone.setHealth(20.0);
            clone.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
            clone.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, (int)(damage / 2)));
            
            clones.add(clone);
            
            // Spawn effect
            spawnLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, spawnLoc, 20, 0.5, 1, 0.5);
        }
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        
        // Make clones attack nearby enemies
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 200) { // 10 seconds
                    this.cancel();
                    
                    // Remove clones
                    for (Entity clone : clones) {
                        if (clone.isValid()) {
                            clone.getWorld().spawnParticle(Particle.SMOKE_LARGE, clone.getLocation(), 20, 0.5, 1, 0.5);
                            clone.remove();
                        }
                    }
                    return;
                }
                
                // Make clones target enemies
                for (Entity clone : clones) {
                    if (clone.isValid() && clone instanceof Zombie) {
                        Zombie zombie = (Zombie) clone;
                        
                        // Find nearest enemy
                        LivingEntity nearest = null;
                        double nearestDist = Double.MAX_VALUE;
                        
                        for (Entity entity : clone.getNearbyEntities(10, 10, 10)) {
                            if (entity instanceof LivingEntity && entity != player && !(entity instanceof Zombie)) {
                                LivingEntity living = (LivingEntity) entity;
                                if (canPvP(player, living)) {
                                    double dist = entity.getLocation().distance(clone.getLocation());
                                    if (dist < nearestDist) {
                                        nearest = (LivingEntity) entity;
                                        nearestDist = dist;
                                    }
                                }
                            }
                        }
                        
                        if (nearest != null) {
                            zombie.setTarget(nearest);
                        }
                    }
                }
                
                ticks += 10;
            }
        }.runTaskTimer(this, 0, 10);
    }
    
    private void voidStep(Player player, int tier) {
        Location start = player.getLocation();
        Location target = player.getTargetBlock(null, 20).getLocation().add(0, 1, 0);
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 3.0 * multiplier;
        
        // Create darkness cloud at start
        start.getWorld().spawnParticle(Particle.SMOKE_LARGE, start, 100, 2, 2, 2);
        
        // Blind and wither enemies at start location
        for (Entity entity : start.getWorld().getNearbyEntities(start, 5, 5, 5)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                if (canPvP(player, living)) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, (int)(damage / 2)));
                }
            }
        }
        
        // Teleport player
        player.teleport(target);
        
        // Create darkness cloud at target
        target.getWorld().spawnParticle(Particle.SMOKE_LARGE, target, 100, 2, 2, 2);
        
        player.getWorld().playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        player.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
    }
    
    // Light Abilities
    private void solarFlare(Player player, int tier) {
        Location center = player.getLocation();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double damage = 8.0 * multiplier;
        
        // Blind enemies
        for (Entity entity : center.getWorld().getNearbyEntities(center, 10, 10, 10)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                if (canPvP(player, living)) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                    living.damage(damage, player);
                }
            }
        }
        
        // Give player absorption
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 3));
        
        // Visual effect
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        
        new BukkitRunnable() {
            double radius = 0;
            
            @Override
            public void run() {
                if (radius >= 10) {
                    this.cancel();
                    return;
                }
                
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = center.clone().add(x, 1, z);
                    center.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0);
                }
                
                radius += 0.5;
            }
        }.runTaskTimer(this, 0, 1);
    }
    
    private void sanctuary(Player player, int tier) {
        Location center = player.getLocation();
        List<Player> allies = new ArrayList<>();
        double multiplier = getConfig().getDouble("tier-multipliers." + tier, 1.0);
        double healing = 1.0 * multiplier;
        
        // Create dome visual
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 140) { // 7 seconds
                    this.cancel();
                    return;
                }
                
                // Draw dome particles
                for (int i = 0; i < 20; i++) {
                    double u = Math.random();
                    double v = Math.random();
                    double theta = 2 * Math.PI * u;
                    double phi = Math.acos(2 * v - 1);
                    double x = 10 * Math.sin(phi) * Math.cos(theta);
                    double y = 10 * Math.sin(phi) * Math.sin(theta);
                    double z = 10 * Math.cos(phi);
                    
                    if (y > 0) { // Only upper half of sphere
                        Location particleLoc = center.clone().add(x, y, z);
                        center.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0);
                    }
                }
                
                // Protect allies inside
                for (Entity entity : center.getWorld().getNearbyEntities(center, 10, 10, 10)) {
                    if (entity instanceof Player) {
                        Player ally = (Player) entity;
                        if (!allies.contains(ally)) {
                            allies.add(ally);
                        }
                        
                        // Heal and protect
                        ally.setHealth(Math.min(ally.getHealth() + healing, ally.getMaxHealth()));
                        ally.setNoDamageTicks(40); // 2 seconds of damage immunity
                    }
                }
                
                if (ticks % 20 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.5f);
                }
                
                ticks += 5;
            }
        }.runTaskTimer(this, 0, 5);
    }
    
    private boolean canPvP(Player attacker, LivingEntity target) {
        if (!getConfig().getBoolean("allow-pvp", true) && target instanceof Player) {
            return false;
        }
        return true;
    }
    
    private void showCoreInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Elemental Cores ===");
        sender.sendMessage(ChatColor.GOLD + "Earth: " + ChatColor.GRAY + "Earth Fortress, Earthquake");
        sender.sendMessage(ChatColor.AQUA + "Water: " + ChatColor.GRAY + "Tidal Wave, Maelstrom");
        sender.sendMessage(ChatColor.RED + "Fire: " + ChatColor.GRAY + "Inferno Meteor, Ring of Fire");
        sender.sendMessage(ChatColor.WHITE + "Air: " + ChatColor.GRAY + "Hurricane Leap, Cyclone");
        sender.sendMessage(ChatColor.YELLOW + "Lightning: " + ChatColor.GRAY + "Thunderstorm, Lightning Dash");
        sender.sendMessage(ChatColor.BLUE + "Ice: " + ChatColor.GRAY + "Glacial Prison, Blizzard");
        sender.sendMessage(ChatColor.GREEN + "Nature: " + ChatColor.GRAY + "Nature's Embrace, Thornfield");
        sender.sendMessage(ChatColor.DARK_PURPLE + "Shadow: " + ChatColor.GRAY + "Shadow Clone Army, Void Step");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Light: " + ChatColor.GRAY + "Solar Flare, Sanctuary");
    }
    
    // Method to create an elemental core item
    public ItemStack createCoreItem(String coreName, int tier) {
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
        
        // Add special effect for visual flair
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        
        core.setItemMeta(meta);
        return core;
    }
}
