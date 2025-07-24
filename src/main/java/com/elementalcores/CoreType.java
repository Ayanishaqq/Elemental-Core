package com.elementalcores;

public enum CoreType {
    EARTH("Earth", "Resistance II", "Haste I"),
    WATER("Water", "Water Breathing", "Dolphin’s Grace"),
    FIRE("Fire", "Fire Resistance", "Strength I"),
    AIR("Air", "Jump Boost II", "Slow Falling"),
    LIGHTNING("Lightning", "Speed II", "Night Vision"),
    ICE("Ice", "Resistance to Slowness", "Frost Walker"),
    NATURE("Nature", "Regeneration II", "Saturation"),
    SHADOW("Shadow", "Night Vision", "Invisibility"),
    LIGHT("Light", "Glowing", "Absorption I");

    private final String name, passive1, passive2;
    CoreType(String name, String passive1, String passive2) {
        this.name = name; this.passive1 = passive1; this.passive2 = passive2;
    }
    public String getName() { return name; }
    public String getPassive1() { return passive1; }
    public String getPassive2() { return passive2; }
}
