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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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
import org.bukkit.event.EventPriority;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class ElementalCores extends JavaPlugin implements Listener {
    
    private HashMap<UUID, HashMap<String, Long>> cooldowns = new HashMap<>();
    private HashMap<UUID, String> activeCore = new HashMap<>();
    private Set<UUID> firstJoinPlayers = new HashSet<>();
    private NamespacedKey coreKey;
    private NamespacedKey tierKey;
    private RecipeManager recipeManager;
    
    // Reference to potion effect types for compatibility
    private final PotionEffectType RESISTANCE = PotionEffectType.getByName("DAMAGE_RESISTANCE");
    private final PotionEffectType HASTE = PotionEffectType.getByName("FAST_DIGGING");
    private final PotionEffectType STRENGTH = PotionEffectType.getByName("INCREASE_DAMAGE");
    private final PotionEffectType JUMP_BOOST = PotionEffectType.getByName("JUMP");
    private final PotionEffectType SLOWNESS = PotionEffectType.getByName("SLOW");
    
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
        recipeManager.registerUpgradeRecipes();
        
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
            
            if (args[0].equalsIgnoreCase("caller") && sender.hasPermission("elementalcores.admin")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /core caller <player>");
                    return true;
                }
                
                Player target = getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                
                giveElementCaller(target);
                sender.sendMessage(ChatColor.GREEN + "Gave Element Caller to " + target.getName());
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
        
        // Check for core use
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() == Material.PAPER && offhand.hasItemMeta()) {
            ItemMeta meta = offhand.getItemMeta();
            if (meta.getPersistentDataContainer().has(coreKey, PersistentDataType.STRING)) {
                String coreName = meta.getPersistentDataContainer().get(coreKey, PersistentDataType.STRING);
                
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    
                    if (player.isSneaking()) {
                        useShiftAbility(player, coreName);
                    } else {
                        useNormalAbility(player, coreName);
                    }
                }
                
                return;
            }
        }
        
        // Check for Element Caller use
        ItemStack mainhand = player.getInventory().getItemInMainHand();
        if (mainhand != null && mainhand.getType() == Material.HEART_OF_THE_SEA && mainhand.hasItemMeta()) {
            ItemMeta meta = mainhand.getItemMeta();
            if (meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.GOLD + "Element Caller")) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    useElementCaller(player, mainhand);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // Prevent dropping cores
        Item droppedItem = event.getItemDrop();
        ItemStack itemStack = droppedItem.getItemStack();
        
        if (isElementalCore(itemStack)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.RED + "You cannot drop Elemental Cores!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<ItemStack> drops = event.getDrops();
        List<ItemStack> coresToSave = new ArrayList<>();
        
        // Find and remove cores from drops
        Iterator<ItemStack> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (isElementalCore(item)) {
                coresToSave.add(item.clone());
                iterator.remove();
            }
        }
        
        // If we found any cores, restore them after respawn
        if (!coresToSave.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        for (ItemStack core : coresToSave) {
                            player.getInventory().addItem(core);
                        }
                        player.sendMessage(ChatColor.GREEN + "Your Elemental Cores have been returned to you!");
                    }
                }
            }.runTaskLater(this, 20); // Run 1 second after respawn
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
            if (item != null && item.hasItemMeta()) {
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
    
    private void giveElementCaller(Player player) {
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
        
        player.getInventory().addItem(elementCaller);
        player.sendMessage(ChatColor.GREEN + "You received an " + ChatColor.GOLD + "Element Caller" + ChatColor.GREEN + "!");
    }
    
    private void useElementCaller(Player player, ItemStack elementCaller) {
        // Check if player has a core
        ItemStack currentCore = null;
        int currentCoreTier = 1;
        String currentCoreType = null;
        
        // Check inventory for any core
        for (ItemStack item : player.getInventory().getContents()) {
            if (isElementalCore(item)) {
                currentCore = item;
                ItemMeta coreMeta = item.getItemMeta();
                currentCoreTier = coreMeta.getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
                currentCoreType = coreMeta.getPersistentDataContainer().get(coreKey, PersistentDataType.STRING);
                break;
            }
        }
        
        if (currentCore == null) {
            player.sendMessage(ChatColor.RED + "You need to have an Elemental Core to use the Element Caller!");
            return;
        }
        
        // Get all possible cores except current one
        List<String> availableCores = new ArrayList<>(Arrays.asList(
            "earth", "water", "fire", "air", "lightning", "ice", "nature", "shadow", "light"
        ));
        availableCores.remove(currentCoreType);
        
        // Choose a random new core
        String newCoreType = availableCores.get(new Random().nextInt(availableCores.size()));
        
        // Remove current core and give new one
        player.getInventory().remove(currentCore);
        giveCore(player, newCoreType, currentCoreTier);
        
        // Consume Element Caller
        elementCaller.setAmount(elementCaller.getAmount() - 1);
        
        // Effects
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5);
        
        player.sendMessage(ChatColor.GREEN + "The Element Caller transforms your core into something new!");
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
        switch (coreName.toLowerCase()) {
            case "earth":
                if (tier >= 3) {
                    player.addPotionEffect(new PotionEffect(RESISTANCE, 40, 1, true, false));
                } else {
                    player.addPotionEffect(new PotionEffect(RESISTANCE, 40, 0, true, false));
                }
                player.addPotionEffect(new PotionEffect(HASTE, 40, tier >= 2 ? 1 : 0, true, false));
                break;
                
            case "water":
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 40, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, tier >= 3 ? 1 : 0, true, false));
                break;
                
            case "fire":
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, true, false));
                player.addPotionEffect(new PotionEffect(STRENGTH, 40, tier >= 2 ? 1 : 0, true, false));
                break;
                
            case "air":
                player.addPotionEffect(new PotionEffect(JUMP_BOOST, 40, 1, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0, true, false));
                break;
                
            case "lightning":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, tier >= 3 ? 1 : 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 40, 0, true, false));
                break;
                
            case "ice":
                player.addPotionEffect(new PotionEffect(RESISTANCE, 40, tier >= 3 ? 1 : 0, true, false));
                // Frost Walker is an enchantment, not a potion effect
                break;
                
            case "nature":
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, tier >= 3 ? 1 : 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, 0, true, false));
                break;
                
            case "shadow":
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 40, 0, true, false));
                // Add invisibility in bursts, longer for higher tiers
                if (player.getWorld().getTime() % (tier == 3 ? 60 : (tier == 2 ? 80 : 100)) == 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, tier * 20, 0, true, false));
                }
                break;
                
            case "light":
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 40, tier >= 3 ? 1 : 0, true, false));
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
    
    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
    
    private void useNormalAbility(Player player, String coreName) {
        String abilityName = "";
        ItemStack offhand = player.getInventory().getItemInOffHand();
        int tier = 1;
        
        if (offhand != null && offhand.hasItemMeta()) {
            ItemMeta meta = offhand.getItemMeta();
            if (meta.getPersistentDataContainer().has(tierKey, PersistentDataType.INTEGER)) {
                tier = meta.getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
            }
        }
        
        // Get tier-specific cooldown
        int cooldown;
        switch (tier) {
            case 3:
                cooldown = 8;
                break;
            case 2:
                cooldown = 10;
                break;
            default:
                cooldown = 12;
        }
        
        switch (coreName.toLowerCase()) {
            case "earth":
                abilityName = "Earth Fortress";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                earthFortress(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "water":
                abilityName = "Tidal Wave";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                tidalWave(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "fire":
                abilityName = "Inferno Meteor";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                infernoMeteor(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "air":
                abilityName = "Hurricane Leap";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                hurricaneLeap(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "lightning":
                abilityName = "Thunderstorm";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                thunderstorm(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "ice":
                abilityName = "Glacial Prison";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                glacialPrison(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "nature":
                abilityName = "Nature's Embrace";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                naturesEmbrace(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "shadow":
                abilityName = "Shadow Clone Army";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                shadowCloneArmy(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "light":
                abilityName = "Solar Flare";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                solarFlare(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
        }
    }
    
    private void useShiftAbility(Player player, String coreName) {
        String abilityName = "";
        ItemStack offhand = player.getInventory().getItemInOffHand();
        int tier = 1;
        
        if (offhand != null && offhand.hasItemMeta()) {
            ItemMeta meta = offhand.getItemMeta();
            if (meta.getPersistentDataContainer().has(tierKey, PersistentDataType.INTEGER)) {
                tier = meta.getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
            }
        }
        
        // Get tier-specific cooldown
        int cooldown;
        switch (tier) {
            case 3:
                cooldown = 12;
                break;
            case 2:
                cooldown = 15;
                break;
            default:
                cooldown = 18;
        }
        
        switch (coreName.toLowerCase()) {
            case "earth":
                abilityName = "Earthquake";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                earthquake(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "water":
                abilityName = "Maelstrom";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                maelstrom(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "fire":
                abilityName = "Ring of Fire";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                ringOfFire(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "air":
                abilityName = "Cyclone";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                cyclone(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "lightning":
                abilityName = "Lightning Dash";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                lightningDash(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "ice":
                abilityName = "Blizzard";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                blizzard(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "nature":
                abilityName = "Thornfield";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                thornfield(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "shadow":
                abilityName = "Void Step";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
                    return;
                }
                voidStep(player, tier);
                setCooldown(player, abilityName, cooldown);
                break;
                
            case "light":
                abilityName = "Sanctuary";
                if (isOnCooldown(player, abilityName)) {
                    sendActionBar(player, ChatColor.YELLOW + abilityName + " ⏳ " + ChatColor.RED + getCooldownLeft(player, abilityName) + "s left");
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
        
        // Get tier-specific values
        int radius;
        int duration;
        double damage;
        
        switch (tier) {
            case 3:
                radius = 5;
                duration = 7;
                damage = 10.0;
                break;
            case 2:
                radius = 4;
                duration = 6;
                damage = 8.0;
                break;
            default:
                radius = 3;
                duration = 5;
                damage = 6.0;
        }
        
        // Create walls
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y <= 3; y++) {
                    if (Math.abs(x) == radius || Math.abs(z) == radius) {
                        Location blockLoc = center.clone().add(x, y, z);
                        Block block = blockLoc.getBlock();
                        if (block.getType() == Material.AIR) {
                            if (y == 0 || (Math.abs(x) == radius && Math.abs(z) == radius)) {
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
        for (Entity entity : player.getNearbyEntities(radius + 2, radius + 2, radius + 2)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                if (canPvP(player, living)) {
                    living.damage(damage, player);
                    living.setVelocity(new Vector(0, 1.5, 0));
                }
            }
        }
        
        player.getWorld().playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.FALLING_DUST, center, 100, radius, 2, radius);
        
        // Remove blocks after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : placedBlocks) {
                    block.setType(Material.AIR);
                    block.getWorld().spawnParticle(Particle.FALLING_DUST, block.getLocation(), 10, 0.5, 0.5, 0.5);
                }
            }
        }.runTaskLater(this, duration * 20); // Convert to ticks
    }
    
    private void earthquake(Player player, int tier) {
        Location center = player.getLocation();
        World world = player.getWorld();
        
        // Get tier-specific values
        int radius;
        int slowLevel;
        double damage;
        
        switch (tier) {
            case 3:
                radius = 10;
                slowLevel = 2;
                damage = 10.0;
                break;
            case 2:
                radius = 8;
                slowLevel = 1;
                damage = 8.0;
                break;
            default:
                radius = 6;
                slowLevel = 0;
                damage = 6.0;
        }
        
        // Shake effect and damage
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                if (canPvP(player, living)) {
                    living.damage(damage, player);
                    living.addPotionEffect(new PotionEffect(SLOWNESS, 100, slowLevel));
                    
                    // Shake effect
                    Location loc = living.getLocation();
                    living.teleport(loc.add(Math.random() * 0.5 - 0.25, 0.1, Math.random() * 0.5 - 0.25));
                    
                    // Stun for tier 3
                    if (tier == 3) {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    }
                }
            }
        }
        
        // Break grass, leaves, and flowers - more types for higher tiers
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.sqrt(x*x + z*z) <= radius) {
                        Block block = center.clone().add(x, y, z).getBlock();
                        Material type = block.getType();
                        
                        // Basic blocks for all tiers
                        boolean breakBlock = type == Material.SHORT_GRASS || type == Material.TALL_GRASS;
                        
                        // Add more blocks for higher tiers
                        if (tier >= 2) {
                            breakBlock |= type.name().contains("LEAVES") || type.name().contains("FLOWER");
                        }
                        
                        // Even more blocks for tier 3
                        if (tier >= 3) {
                            breakBlock |= type.name().contains("SAPLING") || type == Material.BROWN_MUSHROOM || 
                                         type == Material.RED_MUSHROOM || type.name().contains("ROOTS");
                        }
                        
                        if (breakBlock) {
                            block.breakNaturally();
                        }
                    }
                }
            }
        }
        
        world.playSound(center, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 2.0f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION, center, 20, radius/2, 1, radius/2);
    }
    
    // Water Abilities
    private void tidalWave(Player player, int tier) {
        Location start = player.getLocation();
        Vector direction = player.getLocation().getDirection().normalize();
        
        // Get tier-specific values
        int distance;
        double damage;
        double pushFactor;
        
        switch (tier) {
            case 3:
                distance = 15;
                damage = 8.0;
                pushFactor = 2.0;
                break;
            case 2:
                distance = 12;
                damage = 6.0;
                pushFactor = 1.7;
                break;
            default:
                distance = 10;
                damage = 4.0;
                pushFactor = 1.5;
        }
        
        new BukkitRunnable() {
            int currentDistance = 0;
            
            @Override
            public void run() {
                if (currentDistance >= distance) {
                    this.cancel();
                    return;
                }
                
                Location waveLoc = start.clone().add(direction.clone().multiply(currentDistance));
                
                // Create wave effect
                for (int y = 0; y <= 3; y++) {
                    for (int x = -2; x <= 2; x++) {
                        Location particleLoc = waveLoc.clone().add(
                            direction.clone().rotateAroundY(Math.PI/2).multiply(x)
                        ).add(0, y, 0);
                        
                        player.getWorld().spawnParticle(Particle.SPLASH, particleLoc, 10, 0.5, 0.5, 0.5);
                    }
                }
                
                // Damage and push entities
                for (Entity entity : waveLoc.getWorld().getNearbyEntities(waveLoc, 3, 3, 3)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity living = (LivingEntity) entity;
                        if (canPvP(player, living)) {
                            living.damage(damage, player);
                            living.setVelocity(direction.clone().multiply(pushFactor).setY(0.5));
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
                currentDistance += 2;
            }
        }.runTaskTimer(this, 0, 2);
    }
    
    private void maelstrom(Player player, int tier) {
        Location center = player.getLocation();
        List<Entity> trapped = new ArrayList<>();
        
        // Get tier-specific values
        int radius;
        int duration;
        double pullStrength;
        
        switch (tier) {
            case 3:
                radius = 8;
                duration = 4;
                pullStrength = 0.4;
                break;
            case 2:
                radius = 7;
                duration = 3;
                pullStrength = 0.3;
                break;
            default:
                radius = 5;
                duration = 2;
                pullStrength = 0.2;
        }
        
        // Pull entities
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                if (canPvP(player, (LivingEntity) entity)) {
                    trapped.add(entity);
                }
            }
        }
        
        // Give player water breathing
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, duration * 20, 0));
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration * 20) {
                    this.cancel();
                    return;
                }
                
                // Create vortex effect
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = Math.cos(angle + ticks * 0.1) * radius;
                    double z = Math.sin(angle + ticks * 0.1) * radius;
                    Location particleLoc = center.clone().add(x, ticks * 0.05, z);
                    player.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, particleLoc, 5, 0.2, 0.2, 0.2);
                }
                
                // Pull and drown trapped entities
                for (Entity entity : trapped) {
                    if (entity.isValid()) {
                        Vector pull = center.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(pullStrength);
                        entity.setVelocity(pull);
                        
                        if (entity instanceof LivingEntity) {
                            LivingEntity living = (LivingEntity) entity;
                            
                            // Apply drowning effect
                            if (living.getRemainingAir() > 0) {
                                living.setRemainingAir(living.getRemainingAir() - 10);
                            } else {
                                living.damage(1.0);
                            }
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
        
        // Get tier-specific values
        double damage;
        int fireDuration;
        double explosionPower;
        
        switch (tier) {
            case 3:
                damage = 10.0;
                fireDuration = 7;
                explosionPower = 3.0f;
                break;
            case 2:
                damage = 8.0;
                fireDuration = 6;
                explosionPower = 2.5f;
                break;
            default:
                damage = 6.0;
                fireDuration = 5;
                explosionPower = 2.0f;
        }
        
        // Launch meteor
        Fireball meteor = player.getWorld().spawn(spawn, Fireball.class);
        meteor.setDirection(new Vector(0, -1, 0));
        meteor.setYield((float)explosionPower);
        meteor.setIsIncendiary(true);
        meteor.setShooter(player);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!meteor.isValid() || meteor.getLocation().getY() <= target.getY()) {
                    this.cancel();
                    
                    // Explosion effects
                    Location impact = meteor.getLocation();
                    impact.getWorld().createExplosion(impact, (float)explosionPower, true, false, player);
                    
                    // Damage and ignite
                    for (Entity entity : impact.getWorld().getNearbyEntities(impact, 5, 5, 5)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity living = (LivingEntity) entity;
                            if (canPvP(player, living)) {
                                living.damage(damage, player);
                                living.setFireTicks(fireDuration * 20);
                            }
                        }
                    }
                    
                    // Ignite ground
                    int fireRadius = tier + 2; // 3, 4, or 5 blocks
                    for (int x = -fireRadius; x <= fireRadius; x++) {
                        for (int z = -fireRadius; z <= fireRadius; z++) {
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
        
        // Get tier-specific values
        int radius;
        int duration;
        double damage;
        
        switch (tier) {
            case 3:
                radius = 7;
                duration = 6;
                damage = 1.5;
                break;
            case 2:
                radius = 6;
                duration = 5;
                damage = 1.0;
                break;
            default:
                radius = 5;
                duration = 4;
                damage = 0.5;
        }
        
        // Create ring
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
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
                if (ticks >= duration * 20) { // Convert to ticks
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
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 3, radius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        double distance = entity.getLocation().distance(center);
                        if (distance <= radius) {
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
        // Get tier-specific values
        int height;
        double damage;
        double knockbackFactor;
        
        switch (tier) {
            case 3:
                height = 20;
                damage = 10.0;
                knockbackFactor = 2.5;
                break;
            case 2:
                height = 15;
                damage = 8.0;
                knockbackFactor = 2.0;
                break;
            default:
                height = 10;
                damage = 6.0;
                knockbackFactor = 1.5;
        }
        
        player.setVelocity(new Vector(0, Math.min(3.0, height / 7.0), 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0));
        
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
                    impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 10, 2, 0, 2);
                    
                    // Damage and knockback
                    for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity living = (LivingEntity) entity;
                            if (canPvP(player, living)) {
                                living.damage(damage, player);
                                Vector knockback = living.getLocation().toVector().subtract(impact.toVector()).normalize().multiply(knockbackFactor).setY(0.5);
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
        
        // Get tier-specific values
        int radius;
        int liftHeight;
        int duration;
        
        switch (tier) {
            case 3:
                radius = 8;
                liftHeight = 10;
                duration = 4;
                break;
            case 2:
                radius = 7;
                liftHeight = 8;
                duration = 3;
                break;
            default:
                radius = 5;
                liftHeight = 6;
                duration = 2;
        }
        
        // Lift entities
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                if (canPvP(player, (LivingEntity) entity)) {
                    lifted.add(entity);
                    entity.setVelocity(new Vector(0, 1.5, 0));
                }
            }
        }
        
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.5f);
        
        // Cyclone effect
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration * 20) {
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
                for (double y = 0; y <= liftHeight; y += 0.5) {
                    double radius = (liftHeight - y) * 0.5;
                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
                        double x = Math.cos(angle + ticks * 0.2) * radius;
                        double z = Math.sin(angle + ticks * 0.2) * radius;
                        Location particleLoc = center.clone().add(x, y, z);
                        center.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0, 0);
                    }
                }
                
                // Keep entities in air
                for (Entity entity : lifted) {
                    if (entity.isValid() && entity.getLocation().getY() < center.getY() + liftHeight) {
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
        
        // Get tier-specific values
        int maxStrikes;
        double damage;
        int stunDuration;
        int searchRadius;
        
        switch (tier) {
            case 3:
                maxStrikes = 7;
                damage = 10.0;
                stunDuration = 3;
                searchRadius = 15;
                break;
            case 2:
                maxStrikes = 5;
                damage = 8.0;
                stunDuration = 2;
                searchRadius = 12;
                break;
            default:
                maxStrikes = 3;
                damage = 6.0;
                stunDuration = 1;
                searchRadius = 10;
        }
        
        // Find targets
        for (Entity entity : player.getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
            if (entity instanceof LivingEntity && entity != player) {
                if (canPvP(player, (LivingEntity) entity)) {
                    targets.add(entity);
                }
            }
        }
        
        // Strike random targets
        Collections.shuffle(targets);
        int strikes = Math.min(maxStrikes, targets.size());
        
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
                        ((LivingEntity) target).addPotionEffect(new PotionEffect(SLOWNESS, stunDuration * 20, 10));
                        ((LivingEntity) target).addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, stunDuration * 20, 10));
                    }
                }
            }.runTaskLater(this, i * 4); // Stagger the strikes
        }
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
    }
    
    private void lightningDash(Player player, int tier) {
        // Get tier-specific values
        int distance;
        double damage;
        int stunDuration;
        
        switch (tier) {
            case 3:
                distance = 20;
                damage = 8.0;
                stunDuration = 2;
                break;
            case 2:
                distance = 15;
                damage = 6.0;
                stunDuration = 1;
                break;
            default:
                distance = 10;
                damage = 4.0;
                stunDuration = 1;
        }
        
        Location start = player.getLocation();
        Vector direction = player.getLocation().getDirection().normalize().multiply(distance);
        Location end = start.clone().add(direction);
        
        // Check for safe landing
        while (!end.getBlock().getType().isSolid() && end.getY() > 0) {
            end.subtract(0, 1, 0);
        }
        end.add(0, 1, 0);
        
        // Create lightning trail
        Vector step = direction.normalize().multiply(0.5);
        Location current = start.clone();
        int steps = (int) (distance / 0.5);
        
        for (int i = 0; i < steps; i++) {
            current.add(step);
            current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, current, 10, 0.2, 0.2, 0.2);
            
            // Damage entities along the path
            for (Entity entity : current.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity living = (LivingEntity) entity;
                    if (canPvP(player, living)) {
                        living.damage(damage, player);
                        living.addPotionEffect(new PotionEffect(SLOWNESS, stunDuration * 20, 10));
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
        // Get tier-specific values
        int radius;
        int duration;
        double damage;
        
        switch (tier) {
            case 3:
                radius = 9;
                duration = 7;
                damage = 8.0;
                break;
            case 2:
                radius = 7;
                duration = 5;
                damage = 6.0;
                break;
            default:
                radius = 5;
                duration = 3;
                damage = 4.0;
        }
        
        List<Entity> frozen = new ArrayList<>();
        
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
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
                    living.addPotionEffect(new PotionEffect(SLOWNESS, duration * 20, 255));
                    living.addPotionEffect(new PotionEffect(JUMP_BOOST, duration * 20, 128));
                }
            }
        }
        
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 2.0f, 2.0f);
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 100, 3, 3, 3);
        
        // Thaw after duration
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
                                        block.getWorld().spawnParticle(Particle.FALLING_DUST, block.getLocation(), 10, 0.5, 0.5, 0.5);
                                    }
                                }
                            }
                        }
                        
                        // Damage on thaw
                        ((LivingEntity) entity).damage(damage, player);
                    }
                }
            }
        }.runTaskLater(this, duration * 20);
    }
    
    private void blizzard(Player player, int tier) {
        // Get tier-specific values
        int radius;
        int duration;
        double damage;
        int slowLevel;
        
        switch (tier) {
            case 3:
                radius = 10;
                duration = 7;
                damage = 3.0;
                slowLevel = 4;
                break;
            case 2:
                radius = 8;
                duration = 6;
                damage = 2.0;
                slowLevel = 3;
                break;
            default:
                radius = 6;
                duration = 5;
                damage = 1.0;
                slowLevel = 2;
        }
        
        Location center = player.getLocation();
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration * 20) { // Convert to ticks
                    this.cancel();
                    return;
                }
                
                // Snow particles
                for (int i = 0; i < 50; i++) {
                    double x = (Math.random() - 0.5) * radius * 2;
                    double y = Math.random() * 5;
                    double z = (Math.random() - 0.5) * radius * 2;
                    Location particleLoc = center.clone().add(x, y, z);
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, -0.5, 0);
                }
                
                // Damage and slow enemies
                if (ticks % 20 == 0) { // Every second
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 5, radius)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity living = (LivingEntity) entity;
                            if (canPvP(player, living)) {
                                living.damage(damage, player);
                                living.addPotionEffect(new PotionEffect(SLOWNESS, 40, slowLevel));
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
        // Get tier-specific values
        int radius;
        double healing;
        
        switch (tier) {
            case 3:
                radius = 10;
                healing = 16.0;
                break;
            case 2:
                radius = 7;
                healing = 12.0;
                break;
            default:
                radius = 5;
                healing = 8.0;
        }
        
        Location center = player.getLocation();
        
        // Heal and cleanse effects
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player) {
                Player ally = (Player) entity;
                ally.setHealth(Math.min(ally.getHealth() + healing, ally.getMaxHealth()));
                
                // Remove negative effects
                for (PotionEffect effect : ally.getActivePotionEffects()) {
                    PotionEffectType type = effect.getType();
                    if (type.equals(PotionEffectType.POISON) || type.equals(PotionEffectType.WITHER) ||
                        type.equals(SLOWNESS) || type.equals(PotionEffectType.WEAKNESS) ||
                        type.equals(PotionEffectType.BLINDNESS)) {
                        ally.removePotionEffect(type);
                    }
                }
                
                ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5);
            }
        }
        
        // Heal self
        player.setHealth(Math.min(player.getHealth() + healing, player.getMaxHealth()));
        
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        center.getWorld().spawnParticle(Particle.COMPOSTER, center, 100, 5, 2, 5);
    }
    
    private void thornfield(Player player, int tier) {
        // Get tier-specific values
        int radius;
        int duration;
        double damage;
        
        switch (tier) {
            case 3:
                radius = 9;
                duration = 4;
                damage = 3.0;
                break;
            case 2:
                radius = 7;
                duration = 3;
                damage = 2.0;
                break;
            default:
                radius = 5;
                duration = 2;
                damage = 1.0;
        }
        
        Location center = player.getLocation();
        List<Entity> rooted = new ArrayList<>();
        List<Location> thornLocations = new ArrayList<>();
        
        // Create thorn field
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.sqrt(x*x + z*z) <= radius) {
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
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 3, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                if (canPvP(player, living)) {
                    rooted.add(living);
                    living.addPotionEffect(new PotionEffect(SLOWNESS, duration * 20, 255));
                    living.addPotionEffect(new PotionEffect(JUMP_BOOST, duration * 20, 128));
                }
            }
        }
        
        center.getWorld().playSound(center, Sound.BLOCK_GRASS_BREAK, 2.0f, 0.5f);
        
        // Damage over time
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration * 20) {
                    this.cancel();
                    return;
                }
                
                // Show thorns
                for (Location loc : thornLocations) {
                    loc.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 2, 0.2, 0.5, 0.2);
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
        // Get tier-specific values
        int numClones;
        int duration;
        double damage;
        
        switch (tier) {
            case 3:
                numClones = 5;
                duration = 10;
                damage = 4.0;
                break;
            case 2:
                numClones = 3;
                duration = 7;
                damage = 3.0;
                break;
            default:
                numClones = 2;
                duration = 5;
                damage = 2.0;
        }
        
        List<Entity> clones = new ArrayList<>();
        
        // Spawn clones
        for (int i = 0; i < numClones; i++) {
            double angle = (Math.PI * 2 / numClones) * i;
            double x = Math.cos(angle) * 3;
            double z = Math.sin(angle) * 3;
            Location spawnLoc = player.getLocation().add(x, 0, z);
            
            Zombie clone = player.getWorld().spawn(spawnLoc, Zombie.class);
            clone.setCustomName(ChatColor.DARK_PURPLE + "Shadow Clone");
            clone.setCustomNameVisible(true);
            clone.setBaby(false);
            clone.setHealth(20.0);
            clone.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, 1));
            clone.addPotionEffect(new PotionEffect(STRENGTH, duration * 20, (int)(damage / 2)));
            
            clones.add(clone);
            
            // Spawn effect
            spawnLoc.getWorld().spawnParticle(Particle.CLOUD, spawnLoc, 20, 0.5, 1, 0.5);
        }
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        
        // Make clones attack nearby enemies
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration * 20) { // Convert to ticks
                    this.cancel();
                    
                    // Remove clones
                    for (Entity clone : clones) {
                        if (clone.isValid()) {
                            clone.getWorld().spawnParticle(Particle.CLOUD, clone.getLocation(), 20, 0.5, 1, 0.5);
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
        // Get tier-specific values
        int distance;
        int duration;
        double damage;
        
        switch (tier) {
            case 3:
                distance = 20;
                duration = 5;
                damage = 3.0;
                break;
            case 2:
                distance = 15;
                duration = 3;
                damage = 2.0;
                break;
            default:
                distance = 10;
                duration = 2;
                damage = 1.0;
        }
        
        Location start = player.getLocation();
        Location target = player.getTargetBlock(null, distance).getLocation().add(0, 1, 0);
        
        // Create darkness cloud at start
        start.getWorld().spawnParticle(Particle.CLOUD, start, 100, 2, 2, 2);
        
        // Blind and wither enemies at start location
        for (Entity entity : start.getWorld().getNearbyEntities(start, 5, 5, 5)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                if (canPvP(player, living)) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration * 20, 0));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration * 20, (int)(damage / 2)));
                }
            }
        }
        
        // Teleport player
        player.teleport(target);
        
        // Create darkness cloud at target
        target.getWorld().spawnParticle(Particle.CLOUD, target, 100, 2, 2, 2);
        
        player.getWorld().playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        player.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
    }
    
    // Light Abilities
    private void solarFlare(Player player, int tier) {
        // Get tier-specific values
        int radius;
        int duration;
        double damage;
        int absorptionLevel;
        int absorptionDuration;
        
        switch (tier) {
            case 3:
                radius = 10;
                duration = 7;
                damage = 8.0;
                absorptionLevel = 3;
                absorptionDuration = 10;
                break;
            case 2:
                radius = 7;
                duration = 5;
                damage = 6.0;
                absorptionLevel = 2;
                absorptionDuration = 7;
                break;
            default:
                radius = 5;
                duration = 3;
                damage = 4.0;
                absorptionLevel = 1;
                absorptionDuration = 5;
        }
        
        Location center = player.getLocation();
        
        // Blind enemies
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                if (canPvP(player, living)) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration * 20, 0));
                    living.damage(damage, player);
                }
            }
        }
        
        // Give player absorption
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, absorptionDuration * 20, absorptionLevel));
        
        // Visual effect
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        
        new BukkitRunnable() {
            double radiusExpanding = 0;
            
            @Override
            public void run() {
                if (radiusExpanding >= radius) {
                    this.cancel();
                    return;
                }
                
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * radiusExpanding;
                    double z = Math.sin(angle) * radiusExpanding;
                    Location particleLoc = center.clone().add(x, 1, z);
                    center.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0);
                }
                
                radiusExpanding += 0.5;
            }
        }.runTaskTimer(this, 0, 1);
    }
    
    private void sanctuary(Player player, int tier) {
        // Get tier-specific values
        int radius;
        int duration;
        double healing;
        
        switch (tier) {
            case 3:
                radius = 10;
                duration = 7;
                healing = 1.5;
                break;
            case 2:
                radius = 7;
                duration = 5;
                healing = 1.0;
                break;
            default:
                radius = 5;
                duration = 4;
                healing = 0.5;
        }
        
        Location center = player.getLocation();
        List<Player> allies = new ArrayList<>();
        
        // Create dome visual
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= duration * 20) { // Convert to ticks
                    this.cancel();
                    return;
                }
                
                // Draw dome particles
                for (int i = 0; i < 20; i++) {
                    double u = Math.random();
                    double v = Math.random();
                    double theta = 2 * Math.PI * u;
                    double phi = Math.acos(2 * v - 1);
                    double x = radius * Math.sin(phi) * Math.cos(theta);
                    double y = radius * Math.sin(phi) * Math.sin(theta);
                    double z = radius * Math.cos(phi);
                    
                    if (y > 0) { // Only upper half of sphere
                        Location particleLoc = center.clone().add(x, y, z);
                        center.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0);
                    }
                }
                
                // Protect allies inside
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
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
        sender.sendMessage(ChatColor.GRAY + "Each core has 3 tiers with stronger abilities.");
        sender.sendMessage(ChatColor.GRAY + "Use Element Caller to reroll your core type.");
    }
    
    // Helper method to check if an item is an Elemental Core
    private boolean isElementalCore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(coreKey, PersistentDataType.STRING);
    }
    
    // Method to create an elemental core item
    public ItemStack createCoreItem(String coreName, int tier) {
        ItemStack core = new ItemStack(Material.PAPER);
        ItemMeta meta = core.getItemMeta();
        
        String tierName;
        switch (tier) {
            case 3:
                tierName = "Master";
                break;
            case 2:
                tierName = "Advanced";
                break;
            default:
                tierName = "Novice";
                tier = 1;
        }
        
        // Set display name and lore based on core type
        switch (coreName.toLowerCase()) {
            case "earth":
                meta.setDisplayName(ChatColor.GOLD + "Earth Core " + ChatColor.GRAY + "(" + tierName + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Earth Fortress",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Earthquake",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + (tier >= 3 ? "Resistance II, Haste II" : 
                                                                      (tier >= 2 ? "Resistance I, Haste II" : "Resistance I, Haste I")),
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "water":
                meta.setDisplayName(ChatColor.AQUA + "Water Core " + ChatColor.GRAY + "(" + tierName + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Tidal Wave",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Maelstrom",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + (tier >= 3 ? "Water Breathing, Dolphin's Grace II" : 
                                                                     "Water Breathing, Dolphin's Grace I"),
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "fire":
                meta.setDisplayName(ChatColor.RED + "Fire Core " + ChatColor.GRAY + "(" + tierName + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Inferno Meteor",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Ring of Fire",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + (tier >= 2 ? "Fire Resistance, Strength II" : 
                                                                     "Fire Resistance, Strength I"),
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "air":
                meta.setDisplayName(ChatColor.WHITE + "Air Core " + ChatColor.GRAY + "(" + tierName + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Hurricane Leap",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Cyclone",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Jump Boost II, Slow Falling",
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "lightning":
                meta.setDisplayName(ChatColor.YELLOW + "Lightning Core " + ChatColor.GRAY + "(" + tierName + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Thunderstorm",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Lightning Dash",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + (tier >= 3 ? "Speed II, Night Vision" : 
                                                                     "Speed I, Night Vision"),
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "ice":
                meta.setDisplayName(ChatColor.BLUE + "Ice Core " + ChatColor.GRAY + "(" + tierName + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Glacial Prison",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Blizzard",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + (tier >= 3 ? "Resistance II, Frost Walker II" : 
                                                                     "Resistance I, Frost Walker I"),
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "nature":
                meta.setDisplayName(ChatColor.GREEN + "Nature Core " + ChatColor.GRAY + "(" + tierName + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Nature's Embrace",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Thornfield",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + (tier >= 3 ? "Regeneration II, Saturation" : 
                                                                     "Regeneration I, Saturation"),
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "shadow":
                meta.setDisplayName(ChatColor.DARK_PURPLE + "Shadow Core " + ChatColor.GRAY + "(" + tierName + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Shadow Clone Army",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Void Step",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Night Vision, Invisibility" + 
                                                                     (tier >= 3 ? " (Long)" : (tier >= 2 ? " (Medium)" : " (Short)")),
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
            case "light":
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Light Core " + ChatColor.GRAY + "(" + tierName + ")");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right Click: " + ChatColor.YELLOW + "Solar Flare",
                    ChatColor.GRAY + "Shift + Right Click: " + ChatColor.YELLOW + "Sanctuary",
                    ChatColor.GRAY + "Passive: " + ChatColor.GREEN + "Glowing, Absorption" + (tier >= 3 ? " II" : " I"),
                    "",
                    ChatColor.DARK_GRAY + "Hold in offhand to use"
                ));
                break;
        }
        
        // Add NBT data
        meta.getPersistentDataContainer().set(coreKey, PersistentDataType.STRING, coreName.toLowerCase());
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, tier);
        
        // Add special effect for visual flair
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        
        core.setItemMeta(meta);
        return core;
    }
}
