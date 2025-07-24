package com.elementalcores;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PassiveManager implements Listener {
    private final ElementalCores plugin;
    private final CoreManager coreManager;

    public PassiveManager(ElementalCores plugin, CoreManager coreManager) {
        this.plugin = plugin;
        this.coreManager = coreManager;
    }

    public void applyPassives() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (!coreManager.isCore(offhand)) continue;

            CoreType type = coreManager.getCoreType(offhand);
            int tier = coreManager.getCoreTier(offhand); // Tiers don't affect passives much, but can scale if needed
            int amplifier = tier - 1; // e.g., T3 = amplifier 2

            player.addPotionEffect(new PotionEffect(type.getPassive1(), 100, amplifier, true, false));
            player.addPotionEffect(new PotionEffect(type.getPassive2(), 100, amplifier, true, false));

            // Special cases (e.g., Ice resistance to slowness: remove slowness effect)
            if (type == CoreType.ICE && player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                player.removePotionEffect(PotionEffectType.SLOWNESS);
            }
            if (type == CoreType.SHADOW) {
                // Short bursts of invisibility
                if (Math.random() < 0.1) player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0));
            }

            // Occasional particle/sound at feet
            if (Math.random() < 0.2) {
                player.getWorld().spawnParticle(type.getParticle(), player.getLocation(), 5);
                player.getWorld().playSound(player.getLocation(), type.getSound(), 0.5f, 1.0f);
            }
        }
    }
}
