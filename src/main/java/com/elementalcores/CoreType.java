package com.elementalcores;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

public enum CoreType {
    EARTH("Earth", "#8B5A2B", 1001, Particle.BLOCK, Material.STONE, Sound.BLOCK_STONE_PLACE, PotionEffectType.RESISTANCE, PotionEffectType.HASTE),
    WATER("Water", "#1E90FF", 1002, Particle.SPLASH, Material.WATER_BUCKET, Sound.ENTITY_PLAYER_SPLASH, PotionEffectType.WATER_BREATHING, PotionEffectType.DOLPHINS_GRACE),
    FIRE("Fire", "#FF4500", 1003, Particle.FLAME, Material.LAVA_BUCKET, Sound.BLOCK_FIRE_AMBIENT, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.STRENGTH),
    AIR("Air", "#B0E0E6", 1004, Particle.CLOUD, Material.FEATHER, Sound.ENTITY_ENDER_DRAGON_FLAP, PotionEffectType.JUMP_BOOST, PotionEffectType.SLOW_FALLING),
    LIGHTNING("Lightning", "#FFFF00", 1005, Particle.ELECTRIC_SPARK, Material.TRIDENT, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, PotionEffectType.SPEED, PotionEffectType.NIGHT_VISION),
    ICE("Ice", "#00FFFF", 1006, Particle.SNOWFLAKE, Material.ICE, Sound.BLOCK_GLASS_PLACE, PotionEffectType.SLOWNESS, null), // Frost Walker handled manually
    NATURE("Nature", "#228B22", 1007, Particle.HAPPY_VILLAGER, Material.OAK_SAPLING, Sound.BLOCK_GRASS_PLACE, PotionEffectType.REGENERATION, PotionEffectType.SATURATION),
    SHADOW("Shadow", "#4B0082", 1008, Particle.SMOKE, Material.WITHER_ROSE, Sound.ENTITY_GHAST_SHOOT, PotionEffectType.NIGHT_VISION, PotionEffectType.INVISIBILITY),
    LIGHT("Light", "#FFFACD", 1009, Particle.END_ROD, Material.GLOWSTONE, Sound.BLOCK_BEACON_ACTIVATE, PotionEffectType.GLOWING, PotionEffectType.ABSORPTION);

    private final String name;
    private final String colorHex;
    private final int customModelData;
    private final Particle particle;
    private final Material animationItem;
    private final Sound sound;
    private final PotionEffectType passive1;
    private final PotionEffectType passive2;

    CoreType(String name, String colorHex, int customModelData, Particle particle, Material animationItem, Sound sound, PotionEffectType passive1, PotionEffectType passive2) {
        this.name = name;
        this.colorHex = colorHex;
        this.customModelData = customModelData;
        this.particle = particle;
        this.animationItem = animationItem;
        this.sound = sound;
        this.passive1 = passive1;
        this.passive2 = passive2;
    }

    // Getters (used in other classes)
    public String getName() { return name; }
    public String getColorHex() { return colorHex; }
    public int getCustomModelData() { return customModelData; }
    public Particle getParticle() { return particle; }
    public Material getAnimationItem() { return animationItem; }
    public Sound getSound() { return sound; }
    public PotionEffectType getPassive1() { return passive1; }
    public PotionEffectType getPassive2() { return passive2; }
}
