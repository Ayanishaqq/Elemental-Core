package com.elementalcores;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    private final ElementalCores plugin;
    private final CoreManager coreManager;

    public CommandHandler(ElementalCores plugin, CoreManager coreManager) {
        this.plugin = plugin;
        this.coreManager = coreManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        String sub = args[0].toLowerCase();
        if (sub.equals("give") && args.length == 4) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "Only OPs can use this!");
                return true;
            }
            CoreType type;
            try {
                type = CoreType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid core type!");
                return true;
            }
            Player target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }
            int tier = Integer.parseInt(args[3]);
            if (tier < 1 || tier > 3) {
                sender.sendMessage(ChatColor.RED + "Tier must be 1-3!");
                return true;
            }
            coreManager.giveRandomCore(target); // Removes existing
            target.getInventory().addItem(coreManager.createCore(type, tier));
            sender.sendMessage(ChatColor.GREEN + "Gave " + type.getName() + " Core T" + tier + " to " + target.getName());
        } else if (sub.equals("reload")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "Only OPs can use this!");
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Plugin reloaded!");
        } else if (sub.equals("info")) {
            sender.sendMessage(ChatColor.YELLOW + "Elemental Cores Info:");
            for (CoreType type : CoreType.values()) {
                sender.sendMessage(ChatColor.BOLD + type.getName() + ":");
                sender.sendMessage("Right Click: " + coreManager.getRightClickAbilityName(type));
                sender.sendMessage("Shift+Right Click: " + coreManager.getShiftRightClickAbilityName(type));
                sender.sendMessage("Passive: " + (type.getPassive1() != null ? type.getPassive1().getName() : "") + ", " + (type.getPassive2() != null ? type.getPassive2().getName() : ""));
            }
        }
        return true;
    }
}
