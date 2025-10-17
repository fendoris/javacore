package me.kc1508.javacore.spawn;

import me.kc1508.javacore.FendorisPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

public class SpawnWaypointService {

    private static final String WAYPOINT_TAG = "fendoris_spawn_waypoint";

    private final FendorisPlugin plugin;
    private Integer forcedChunkX = null;
    private Integer forcedChunkZ = null;
    private String forcedWorldName = null;

    public SpawnWaypointService(FendorisPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        // Defer until worlds are available
        if (Bukkit.getWorlds().isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, this::enable);
            return;
        }

        // Always purge existing, per request
        purgeExisting();

        if (!plugin.getConfig().getBoolean("spawn-waypoint.enabled", true)) {
            return;
        }

        spawnAtConfiguredLocation();

        // Safety: verify presence shortly after startup; attempt one retry if missing
        Bukkit.getScheduler().runTaskLater(plugin, this::verifyAndRepair, 40L);
    }

    public void reload() {
        enable();
    }

    public void disable() {
        unforceChunk();
        purgeExisting();
    }

    private void purgeExisting() {
        int removed = 0;
        for (World w : Bukkit.getWorlds()) {
            for (ArmorStand as : w.getEntitiesByClass(ArmorStand.class)) {
                if (as.getScoreboardTags().contains(WAYPOINT_TAG)) {
                    as.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) plugin.getLogger().info("[SpawnWaypoint] Purged " + removed + " armor stand(s).");

        // No named waypoint registry modifications needed; we color by UUID now.
    }

    private void spawnAtConfiguredLocation() {
        // Resolve world + coords from config, fallback to default world spawn
        String worldName = plugin.getConfig().getString("spawn.location-world", null);
        World world = (worldName != null) ? Bukkit.getWorld(worldName) : null;
        if (world == null) {
            world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
        }
        if (world == null) return;

        Location defaultSpawn = world.getSpawnLocation();
        double x = plugin.getConfig().getDouble("spawn.location-x", defaultSpawn.getX());
        double y = plugin.getConfig().getDouble("spawn.location-y", defaultSpawn.getY());
        double z = plugin.getConfig().getDouble("spawn.location-z", defaultSpawn.getZ());
        Location spawnLoc = new Location(world, x, y, z);

        // Force-load the spawn chunk while plugin is enabled so the entity stays loaded
        try {
            int cx = spawnLoc.getBlockX() >> 4;
            int cz = spawnLoc.getBlockZ() >> 4;
            // Unforce previous if different
            if (forcedChunkX != null && forcedChunkZ != null && forcedWorldName != null) {
                if (!forcedWorldName.equals(world.getName()) || forcedChunkX != cx || forcedChunkZ != cz) {
                    unforceChunk();
                }
            }
            world.setChunkForceLoaded(cx, cz, true);
            forcedChunkX = cx;
            forcedChunkZ = cz;
            forcedWorldName = world.getName();
            // Keep console quiet
        } catch (Throwable t) {
            // Ignore if not supported
        }

        Entity spawned = world.spawn(spawnLoc, ArmorStand.class, as -> {
            as.addScoreboardTag(WAYPOINT_TAG);
            as.setInvulnerable(true);
            as.setGravity(false);
            as.setMarker(true);
            as.setInvisible(true);
            try { as.setCustomNameVisible(false); } catch (Throwable ignored) {}
            try { as.setCollidable(false); } catch (Throwable ignored) {}
            try { as.setRemoveWhenFarAway(false); } catch (Throwable ignored) {}
        });

        // Run console commands after a short delay to ensure entity is fully registered
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                // Ensure we target the exact entity using execute as -> @s
                String attrCmd = "execute as @e[type=armor_stand,limit=1,tag=" + WAYPOINT_TAG + 
                        "] run attribute @s minecraft:waypoint_transmit_range base set 60000000";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), attrCmd);
            } catch (Throwable t) {
                // Attribute may not exist; ignore
            }

            // Apply configured color; accept named color or raw hex.
            // For hex, use the working syntax: waypoint modify <uuid> color hex <RRGGBB|AARRGGBB>
            String colorCfg = plugin.getConfig().getString("spawn-waypoint.color", "gray");
            String uuid = spawned.getUniqueId().toString();
            try {
                String cmd;
                if (colorCfg != null) {
                    String c = colorCfg.trim();
                    // Allow with or without leading '#'
                    if (c.matches("(?i)^#[0-9a-f]{6}$") || c.matches("(?i)^#[0-9a-f]{8}$")) {
                        c = c.substring(1);
                    }
                    if (c.matches("(?i)^[0-9a-f]{6}$") || c.matches("(?i)^[0-9a-f]{8}$")) {
                        cmd = "waypoint modify " + uuid + " color hex " + c.toUpperCase();
                    } else {
                        cmd = "waypoint modify " + uuid + " color " + c;
                    }
                } else {
                    cmd = "waypoint modify " + uuid + " color gray";
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } catch (Throwable ignored) { }

            // Apply style if configured (e.g., "bowtie")
            String desiredStyle = plugin.getConfig().getString("spawn-waypoint.style", "bowtie");
            if (desiredStyle != null && !desiredStyle.isBlank()) {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "waypoint modify " + uuid + " style set " + desiredStyle);
                } catch (Throwable ignored) { }
            }
        }, 10L);

        plugin.getLogger().info("[SpawnWaypoint] Spawned at " + spawned.getWorld().getName() + " [" + spawnLoc.getBlockX() + "," + spawnLoc.getBlockY() + "," + spawnLoc.getBlockZ() + "]");
    }

    private boolean isPresent() {
        for (World w : Bukkit.getWorlds()) {
            for (ArmorStand as : w.getEntitiesByClass(ArmorStand.class)) {
                if (as.getScoreboardTags().contains(WAYPOINT_TAG)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void verifyAndRepair() {
        if (!plugin.getConfig().getBoolean("spawn-waypoint.enabled", true)) return;
        if (isPresent()) return;
        plugin.getLogger().warning("[SpawnWaypoint] Waypoint missing after startup; respawning once.");
        spawnAtConfiguredLocation();
    }

    private void unforceChunk() {
        if (forcedWorldName == null || forcedChunkX == null || forcedChunkZ == null) return;
        World w = Bukkit.getWorld(forcedWorldName);
        if (w != null) {
            try { w.setChunkForceLoaded(forcedChunkX, forcedChunkZ, false); } catch (Throwable ignored) {}
            // Keep console quiet
        }
        forcedWorldName = null;
        forcedChunkX = null;
        forcedChunkZ = null;
    }
}
