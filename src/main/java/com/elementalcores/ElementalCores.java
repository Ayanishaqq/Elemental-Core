package com.elementalcores;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class ElementalCores extends JavaPlugin implements Listener {

    private CoreManager coreManager;

    @Override
    public void onEnable() {
        getLogger().info("ElementalCores enabled!");

        NamespacedKeys.setPlugin(this);

        coreManager = new CoreManager();
        getCommand("core").setExecutor(new CommandHandler(this, coreManager));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new AbilityListener(), this);
        getServer().getPluginManager().registerEvents(new PassiveManager(), this);

        new RecipeManager(this).registerRecipes();
    }

    @Override
    public void onDisable() {
        getLogger().info("ElementalCores disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            playFirstJoinAnimation(player, this);
        }
    }

    public void playFirstJoinAnimation(Player player, JavaPlugin plugin) {
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
                if (ticks >= 140) { // 7 seconds at 20 ticks per second
                    this.cancel();
                    coreManager.giveRandomCore(player);
                    return;
                }
                player.sendTitle(coreNames[index], ChatColor.GRAY + "Elemental Core", 0, 20, 0);
                index = (index + 1) % coreNames.length;
                ticks += 10;
            }
        }.runTaskTimer(plugin, 0, 10); // Change title every 10 ticks (0.5s)
    }
}
