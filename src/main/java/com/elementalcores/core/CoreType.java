package com.elementalcores.core;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;

public enum CoreType {
    EARTH("Earth", ChatColor.of("#8B5A2B"), 1001,
            new Particle[]{Particle.BLOCK, Particle.DUST, Particle.EXPLOSION},
            new Sound[]{Sound.BLOCK_STONE_PLACE, Sound.BLOCK_STONE_BREAK, Sound.ENTITY_GENERIC_EXPLODE}),
    
    WATER("Water", ChatColor.of("#1E90FF"), 1002,
            new Particle[]{Particle.SPLASH, Particle.BUBBLE, Particle.WATER_SPLASH},
            new Sound[]{Sound.ENTITY_PLAYER_SPLASH, Sound.BLOCK_WATER_AMBIENT, Sound.ENTITY_DROWNED_SHOOT}),
    
    FIRE("Fire", ChatColor.of("#FF4500"), 1003,
            new Particle[]{Particle.FLAME, Particle.LAVA, Particle.SMOKE},
            new Sound[]{Sound.BLOCK_FIRE_AMBIENT, Sound.ENTITY_BLAZE_SHOOT, Sound.ENTITY_GHAST_SHOOT}),
    
    AIR("Air", ChatColor.of("#B0E0E6"), 1004,
            new Particle[]{Particle.CLOUD, Particle.SWEEP_ATTACK, Particle.WHITE_ASH},
            new Sound[]{Sound.ENTITY_ENDER_DRAGON_FLAP, Sound.ENTITY_PARROT_FLY, Sound.ENTITY_ARROW_SHOOT}),
    
    LIGHTNING("Lightning", ChatColor.of("#FFFF00"), 1005,
            new Particle[]{Particle.ELECTRIC_SPARK, Particle.CRIT, Particle.FLASH},
            new Sound[]{Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.ENTITY_LIGHTNING_BOLT_IMPACT}),
    
    ICE("Ice", ChatColor.of("#00FFFF"), 1006,
            new Particle[]{Particle.SNOWFLAKE, Particle.ITEM_SNOWBALL, Particle.BLOCK},
            new Sound[]{Sound.BLOCK_GLASS_PLACE, Sound.BLOCK_SNOW_PLACE, Sound.BLOCK_SNOW_BREAK}),
    
    NATURE("Nature", ChatColor.of("#228B22"), 1007,
            new Particle[]{Particle.HAPPY_VILLAGER, Particle.FALLING_SPORE_BLOSSOM, Particle.BLOCK},
            new Sound[]{Sound.BLOCK_GRASS_PLACE, Sound.BLOCK_AZALEA_LEAVES_HIT, Sound.ENTITY_AXOLOTL_ATTACK}),
    
    SHADOW("Shadow", ChatColor.of("#4B0082"), 1008,
            new Particle[]{Particle.SMOKE, Particle.SOUL_FIRE_FLAME, Particle.SOUL},
            new Sound[]{Sound.ENTITY_GHAST_SHOOT, Sound.ENTITY_ENDERMAN_TELEPORT, Sound.BLOCK_SOUL_SAND_PLACE}),
    
    LIGHT("Light", ChatColor.of("#FFFACD"), 1009,
            new Particle[]{Particle.END_ROD, Particle.GLOW, Particle.FIREWORK},
            new Sound[]{Sound.BLOCK_BEACON_ACTIVATE, Sound.BLOCK_BEACON_POWER_SELECT, Sound.ENTITY_PLAYER_LEVELUP});

    private final String name;
    private final ChatColor color;
    private final int customModelData;
    private final Particle[] particles;
    private final Sound[] sounds;

    CoreType(String name, ChatColor color, int customModelData, Particle[] particles, Sound[] sounds) {
        this.name = name;
        this.color = color;
        this.customModelData = customModelData;
        this.particles = particles;
        this.sounds = sounds;
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public Particle[] getParticles() {
        return particles;
    }

    public Sound[] getSounds() {
        return sounds;
    }

    public Particle getMainParticle() {
        return particles[0];
    }

    public Sound getMainSound() {
        return sounds[0];
    }
}
