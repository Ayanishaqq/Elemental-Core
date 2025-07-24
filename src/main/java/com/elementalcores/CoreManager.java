package com.elementalcores;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Random;

public class CoreManager {
    private final Random random = new Random();

    public void giveRandomCore(Player player) {
        // Remove any existing core from inventory (implement as needed)
        // Give a random core (Tier 1)
        CoreType[] types = CoreType.values();
        CoreType type = types[random.nextInt(types.length)];
        player.getInventory().addItem(ElementalCoreItems.getElementalCore(type.name().toLowerCase(), 1));
    }

    public ItemStack createCore(CoreType type, int tier) {
        return ElementalCoreItems.getElementalCore(type.name().toLowerCase(), tier);
    }

    public String getRightClickAbilityName(CoreType type) {
        // Return the right click ability name for the core
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

    public String getShiftRightClickAbilityName(CoreType type) {
        // Return the shift right click ability name for the core
        switch (type) {
            case EARTH: return "Seismic Slam";
            case WATER: return "Aqua Vortex";
            case FIRE: return "Infernal Eruption";
            case AIR: return "Wind Blade";
            case LIGHTNING: return "Lightning Dash";
            case ICE: return "Blizzard";
            case NATURE: return "Nature’s Wrath";
            case SHADOW: return "Void Step";
            case LIGHT: return "Sanctuary";
            default: return "Unknown";
        }
    }
}
