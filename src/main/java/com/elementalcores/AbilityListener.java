package com.elementalcores;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AbilityListener implements Listener {
    private final ElementalCores plugin;
    private final CoreManager coreManager;

    public AbilityListener(ElementalCores plugin, CoreManager coreManager) {
        this.plugin = plugin;
        this.coreManager = coreManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !coreManager.isCore(item)) return;

        boolean isRightClick = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        boolean isShift = player.isSneaking();
        if (!isRightClick) return;

        if (coreManager.isOnCooldown(player, isShift)) return;

        CoreType type = coreManager.getCoreType(item);
        int tier = coreManager.getCoreTier(item);
        int radius = switch (tier) { case 1 -> 5; case 2 -> 7; default -> 10; };
        int duration = switch (tier) { case 1 -> 5; case 2 -> 7; default -> 10; }; // Seconds
        int damage = switch (tier) { case 1 -> 4; case 2 -> 6; default -> 8; };
        int targets = switch (tier) { case 1 -> 3; case 2 -> 6; default -> 12; };

        if (!isShift) {
            // Right Click abilities
            switch (type) {
                case EARTH -> earthPrison(player, radius, duration);
                case WATER -> tsunamiSurge(player, radius, damage);
                case FIRE -> meteorStrike(player, radius, damage, duration);  // Fixed: Added duration param
                case AIR -> tornadoLaunch(player, radius);
                case LIGHTNING -> stormcall(player, targets);
                case ICE -> glacialSpike(player, duration, damage);
                case NATURE -> entanglingRoots(player, radius, duration);
                case SHADOW -> shadowClone(player, tier, duration);
                case LIGHT -> solarBeam(player, damage, radius);
            }
        } else {
            // Shift+Right Click
            switch (type) {
                case EARTH -> seismicSlam(player, radius, duration, damage);  // Fixed: Added damage param
                case WATER -> aquaVortex(player, radius, duration, damage);
                case FIRE -> infernalEruption(player, radius, duration, damage);
                case AIR -> windBlade(player, damage, targets);
                case LIGHTNING -> lightningDash(player, damage, tier);
                case ICE -> blizzard(player, radius, duration, damage);
                case NATURE -> naturesWrath(player, radius, damage);
                case SHADOW -> voidStep(player, tier);
                case LIGHT -> sanctuary(player, radius, duration);
            }
        }

        // Play particle and sound for the core type
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(type.getParticle(), loc, 50);
        player.getWorld().playSound(loc, type.getSound(), 1.0f, 1.0f);
    }

    // Earth Abilities
    private void earthPrison(Player player, int radius, int duration) {
        Entity target = getNearestEnemy(player, radius);
        if (target == null) return;
        Location loc = target.getLocation();
        // Build stone cage (size scales with tier: small for T1, larger for T3)
        int cageSize = (int) (radius / 2.5); // e.g., 2 for T1, 4 for T3
        for (int x = -cageSize; x <= cageSize; x++) for (int y = 0; y <= cageSize * 2; y++) for (int z = -cageSize; z <= cageSize; z++) {
            if (Math.abs(x) != cageSize && Math.abs(z) != cageSize && y != 0 && y != cageSize * 2) continue; // Hollow cage
            Location blockLoc = loc.clone().add(x, y, z);
            if (blockLoc.getBlock().getType() == Material.AIR) blockLoc.getBlock().setType(Material.STONE);
        }
        // Remove after duration
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (int x = -cageSize; x <= cageSize; x++) for (int y = 0; y <= cageSize * 2; y++) for (int z = -cageSize; z <= cageSize; z++) {
                loc.clone().add(x, y, z).getBlock().setType(Material.AIR);
            }
        }, duration * 20L);
        // Particles: block stone, dust
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 50, Material.STONE.createBlockData());
        loc.getWorld().spawnParticle(Particle.DUST, loc, 50, new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 90, 43), 1));
    }

    private void seismicSlam(Player player, int radius, int duration, int damage) {  // Fixed: Added damage param
        player.setVelocity(player.getLocation().getDirection().multiply(2).setY(1)); // Leap forward
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location loc = player.getLocation();
            loc.getWorld().createExplosion(loc, 0, false, false); // Visual explosion
            for (Entity e : getNearbyEntities(loc, radius)) {
                if (e != player && e instanceof LivingEntity) {
                    e.setVelocity(new Vector(0, 1.5, 0)); // Launch up
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration * 20, 2)); // Stun
                    ((LivingEntity) e).damage(damage / 2.0);
                }
            }
            // Particles: explosion, dust
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 10);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 50, new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 90, 43), 1));
        }, 15L); // Short delay for slam
    }

    // Water Abilities
    private void tsunamiSurge(Player player, int radius, int damage) {
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().multiply(radius);
        for (int i = 0; i < radius; i++) {
            Location waveLoc = start.clone().add(dir.clone().normalize().multiply(i));
            waveLoc.getWorld().spawnParticle(Particle.SPLASH, waveLoc, 20);  // Fixed particle name
            for (Entity e : getNearbyEntities(waveLoc, 2)) {
                if (e != player && e instanceof LivingEntity) {
                    e.setVelocity(dir.clone().normalize().multiply(1.5)); // Push
                    ((LivingEntity) e).damage(damage);
                }
            }
        }
        // Sounds: splash, bubble
        start.getWorld().playSound(start, Sound.ENTITY_PLAYER_SPLASH, 1, 1);
        start.getWorld().playSound(start, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1, 1);
    }

    private void aquaVortex(Player player, int radius, int duration, int damage) {
        Location loc = player.getLocation();
        for (Entity e : getNearbyEntities(loc, radius)) {
            if (e != player && e instanceof LivingEntity) {
                Vector pull = loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.5);
                e.setVelocity(pull);
                ((LivingEntity) e).damage(damage / 2.0);
                ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, duration * 20, 0, false, false)); // Suffocate simulation (no air)
            }
        }
        // Particles: bubble, splash (fixed)
        loc.getWorld().spawnParticle(Particle.BUBBLE, loc, 50);
        loc.getWorld().spawnParticle(Particle.SPLASH, loc, 50);
    }

    // Fire Abilities
    private void meteorStrike(Player player, int radius, int damage, int duration) {  // Fixed: Added duration param
        RayTraceResult ray = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 50, e -> e instanceof LivingEntity && e != player);
        Location targetLoc = (ray != null && ray.getHitEntity() != null) ? ray.getHitEntity().getLocation() : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(20));
        FallingBlock meteor = targetLoc.getWorld().spawnFallingBlock(targetLoc.add(0, 10, 0), Material.FIRE.createBlockData());
        meteor.setVelocity(new Vector(0, -2, 0));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!meteor.isDead()) {
                meteor.getWorld().createExplosion(meteor.getLocation(), radius / 2f, true, false);
                for (Entity e : getNearbyEntities(meteor.getLocation(), radius)) {
                    if (e instanceof LivingEntity) {
                        ((LivingEntity) e).damage(damage);
                        e.setFireTicks(duration * 20);
                    }
                }
                meteor.remove();
            }
        }, 40L); // Time to fall
        // Particles: flame, lava
        targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 50);
        targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 10);
    }

    private void infernalEruption(Player player, int radius, int duration, int damage) {
        Location loc = player.getLocation();
        for (Entity e : getNearbyEntities(loc, radius)) {
            if (e != player && e instanceof LivingEntity) {
                ((LivingEntity) e).damage(damage);
                e.setFireTicks(duration * 20);
            }
        }
        // Create temporary fire blocks
        Random random = new Random();
        for (int i = 0; i < radius; i++) {
            Location fireLoc = loc.clone().add(new Vector(random.nextDouble() * radius * 2 - radius, 0, random.nextDouble() * radius * 2 - radius));  // Fixed random vector
            if (fireLoc.getBlock().getType() == Material.AIR) fireLoc.getBlock().setType(Material.FIRE);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> fireLoc.getBlock().setType(Material.AIR), duration * 20L);
        }
        // Particles: smoke, ash
        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 50);
        loc.getWorld().spawnParticle(Particle.ASH, loc, 50);
    }

    // Air Abilities
    private void tornadoLaunch(Player player, int radius) {
        Location loc = player.getLocation();
        for (Entity e : getNearbyEntities(loc, radius)) {
            e.setVelocity(new Vector(0, radius / 2.0, 0)); // Lift up
        }
        player.setVelocity(new Vector(0, radius / 2.0, 0)); // Lift self too
        // Particles: cloud, white_ash
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 50);
        loc.getWorld().spawnParticle(Particle.WHITE_ASH, loc, 50);
    }

    private void windBlade(Player player, int damage, int targets) {
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection();
        int hitCount = 0;
        for (int i = 1; i <= 20; i++) { // Range 20 blocks
            Location bladeLoc = start.clone().add(dir.multiply(i));
            bladeLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, bladeLoc, 5);
            for (Entity e : getNearbyEntities(bladeLoc, 1)) {
                if (e != player && e instanceof LivingEntity && hitCount < targets) {
                    ((LivingEntity) e).damage(damage);
                    hitCount++;
                }
            }
            if (hitCount >= targets) break;
        }
        // Sounds: arrow shoot
        start.getWorld().playSound(start, Sound.ENTITY_ARROW_SHOOT, 1, 1);
    }

    // Lightning Abilities
    private void stormcall(Player player, int targets) {
        List<Entity> enemies = getNearbyEntities(player.getLocation(), 20); // Wide search
        int struck = 0;
        for (Entity e : enemies) {
            if (e != player && e instanceof LivingEntity && struck < targets) {
                e.getWorld().strikeLightning(e.getLocation());
                ((LivingEntity) e).damage(5); // Lightning base damage + extra
                struck++;
            }
        }
        // Particles: electric_spark, flash
        player.getLocation().getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 50);
        player.getLocation().getWorld().spawnParticle(Particle.FLASH, player.getLocation(), 10);
    }

    private void lightningDash(Player player, int damage, int tier) {
        int distance = switch (tier) { case 1 -> 10; case 2 -> 15; default -> 20; };
        Location target = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(distance));
        player.teleport(target);
        // Leave trail
        Location trail = player.getLocation();
        for (int i = 0; i < distance; i++) {
            trail = trail.add(player.getEyeLocation().getDirection());
            trail.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, trail, 10);
            for (Entity e : getNearbyEntities(trail, 2)) {
                if (e != player && e instanceof LivingEntity) {
                    ((LivingEntity) e).damage(damage);
                }
            }
        }
    }

    // Ice Abilities
    private void glacialSpike(Player player, int duration, int damage) {
        RayTraceResult ray = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 30, e -> e instanceof LivingEntity && e != player);
        if (ray == null || ray.getHitEntity() == null) return;
        Entity target = ray.getHitEntity();
        ((LivingEntity) target).damage(damage);
        ((LivingEntity) target).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration * 20, 4)); // Freeze
        // Spawn ice spike visual
        Location loc = target.getLocation();
        for (int y = 0; y < 3; y++) loc.clone().add(0, y, 0).getBlock().setType(Material.ICE);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (int y = 0; y < 3; y++) loc.clone().add(0, y, 0).getBlock().setType(Material.AIR);
        }, duration * 20L);
        // Particles: snowflake, block ice
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 50);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 20, Material.ICE.createBlockData());
    }

    private void blizzard(Player player, int radius, int duration, int damage) {
        Location loc = player.getLocation();
        new org.bukkit.scheduler.BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time >= duration * 20) cancel();
                for (Entity e : getNearbyEntities(loc, radius)) {
                    if (e != player && e instanceof LivingEntity) {
                        ((LivingEntity) e).damage(damage / 5.0); // Tick damage
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                    }
                }
                loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 100, radius, radius, radius, 0);
                time += 20;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // Nature Abilities
    private void entanglingRoots(Player player, int radius, int duration) {
        Location loc = player.getLocation();
        for (Entity e : getNearbyEntities(loc, radius)) {
            if (e != player && e instanceof LivingEntity) {
                ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.POISON, duration * 20, 1));
                ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration * 20, 3)); // Trap
            }
        }
        // Visual roots (vines)
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            Location rootLoc = loc.clone().add(new Vector(random.nextDouble() * radius * 2 - radius, 0, random.nextDouble() * radius * 2 - radius));  // Fixed random vector
            rootLoc.getBlock().setType(Material.VINE);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> rootLoc.getBlock().setType(Material.AIR), duration * 20L);
        }
        // Particles: happy_villager, falling_spore_blossom
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 50);
        loc.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, loc, 50);
    }

    private void naturesWrath(Player player, int radius, int damage) {
        Location loc = player.getLocation();
        for (Entity e : getNearbyEntities(loc, radius)) {
            if (e != player && e instanceof LivingEntity) {
                ((LivingEntity) e).damage(damage);
                e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2)); // Knockback
            }
        }
        // Spawn thorns (cactus or something visual)
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            Location thornLoc = loc.clone().add(new Vector(random.nextDouble() * (radius / 2) * 2 - (radius / 2), 0, random.nextDouble() * (radius / 2) * 2 - (radius / 2)));  // Fixed random vector
            thornLoc.getBlock().setType(Material.CACTUS);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> thornLoc.getBlock().setType(Material.AIR), 100L);
        }
        // Particles: block grass, block leaves
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 50, Material.GRASS_BLOCK.createBlockData());
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 50, Material.OAK_LEAVES.createBlockData());
    }

    // Shadow Abilities
    private void shadowClone(Player player, int tier, int duration) {
        int clones = switch (tier) { case 1 -> 1; case 2 -> 2; default -> 3; };
        Random random = new Random();
        for (int i = 0; i < clones; i++) {
            Location cloneLoc = player.getLocation().add(new Vector(random.nextDouble() * 4 - 2, 0, random.nextDouble() * 4 - 2));  // Fixed random vector
            Zombie clone = (Zombie) player.getWorld().spawnEntity(cloneLoc, EntityType.ZOMBIE);
            clone.setCustomName("Shadow Clone");
            clone.setAI(false); // Distract only
            plugin.getServer().getScheduler().runTaskLater(plugin, clone::remove, duration * 20L);
        }
        // Particles: smoke, soul
        player.getLocation().getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 50);
        player.getLocation().getWorld().spawnParticle(Particle.SOUL, player.getLocation(), 50);
    }

    private void voidStep(Player player, int tier) {
        int distance = switch (tier) { case 1 -> 10; case 2 -> 15; default -> 20; };
        Location target = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(distance));
        player.teleport(target);
        // Leave darkness cloud
        Location cloudLoc = player.getLocation();
        cloudLoc.getWorld().spawnParticle(Particle.ASH, cloudLoc, 100, 3, 3, 3, 0);
        for (Entity e : getNearbyEntities(cloudLoc, 3)) {
            if (e != player && e instanceof LivingEntity) {
                ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
            }
        }
        // Sounds: enderman teleport
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    // Light Abilities
    private void solarBeam(Player player, int damage, int radius) {
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection();
        for (int i = 1; i <= 30; i++) {
            Location beamLoc = start.clone().add(dir.multiply(i));
            beamLoc.getWorld().spawnParticle(Particle.END_ROD, beamLoc, 5);
            for (Entity e : getNearbyEntities(beamLoc, 1)) {
                if (e != player && e instanceof LivingEntity) {
                    ((LivingEntity) e).damage(damage);
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
                }
            }
        }
        // Particles: glow, firework (fixed name)
        start.getWorld().spawnParticle(Particle.GLOW, start, 50);
        start.getWorld().spawnParticle(Particle.FIREWORK, start, 50);
    }

    private void sanctuary(Player player, int radius, int duration) {
        Location loc = player.getLocation();
        // Heal and protect allies (players/mobs near player - assuming "allies" are non-hostile or same team; simplify to nearby)
        new org.bukkit.scheduler.BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time >= duration * 20) cancel();
                for (Entity e : getNearbyEntities(loc, radius)) {
                    if (e instanceof LivingEntity) {
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1));
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 40, 1));
                    }
                }
                // Visual dome (particles)
                for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 20) {
                    Location particleLoc = loc.clone().add(radius * Math.cos(theta), time / 20.0 % radius, radius * Math.sin(theta));
                    particleLoc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1);
                }
                time += 20;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // Helper methods (same as before)
    private Entity getNearestEnemy(Player player, int radius) {
        Entity nearest = null;
        double dist = Double.MAX_VALUE;
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity && e != player) {
                double d = e.getLocation().distance(player.getLocation());
                if (d < dist) {
                    dist = d;
                    nearest = e;
                }
            }
        }
        return nearest;
    }

    private List<Entity> getNearbyEntities(Location loc, double radius) {
        List<Entity> entities = new ArrayList<>();
        loc.getWorld().getNearbyEntities(loc, radius, radius, radius, e -> e instanceof LivingEntity).forEach(entities::add);
        return entities;
    }
}
