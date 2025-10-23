package me.kc1508.javacore.time;

import me.kc1508.javacore.FendorisPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitTask;
import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExtendedDayService implements Listener {

    private final FendorisPlugin plugin;
    private enum Mode { SMOOTH, BATCHED }

    private BukkitTask timeTask;
    private BukkitTask tpsCheckTask;
    private Mode currentMode = null;
    private int batchedStep = 0; // for 7,7,6 cadence
    private boolean enabled = false;

    // Track original gamerule values for worlds we changed so we can restore exactly
    private final Map<UUID, Boolean> originalDaylightCycle = new HashMap<>();

    public ExtendedDayService(FendorisPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        boolean shouldEnable = plugin.getConfig().getBoolean("system.extended-day", false);
        if (shouldEnable) {
            enable();
        } else {
            disable();
        }
    }

    public void enable() {
        if (enabled) return;

        // Ensure vanilla cycle is off (Overworld/NORMAL only) so we control time progression there
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == Environment.NORMAL) {
                applyDoDaylightCycleFalse(world);
            }
        }

        // Start TPS check every 60 seconds, and immediately decide initial mode
        tpsCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkTpsAndMaybeSwitch, 0L, 1200L);

        enabled = true;
        plugin.getLogger().info("[ExtendedDay] Enabled (60-minute full cycle; adaptive)");
    }

    public void disable() {
        if (!enabled && timeTask == null && originalDaylightCycle.isEmpty()) return;

        if (tpsCheckTask != null) { tpsCheckTask.cancel(); tpsCheckTask = null; }
        if (timeTask != null) { timeTask.cancel(); timeTask = null; }
        currentMode = null;
        batchedStep = 0;

        // Restore gamerule values we changed
        for (World world : Bukkit.getWorlds()) {
            Boolean prev = originalDaylightCycle.get(world.getUID());
            if (prev != null) {
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, prev);
            }
        }
        originalDaylightCycle.clear();

        enabled = false;
        plugin.getLogger().info("[ExtendedDay] Disabled (restored daylight cycle)");
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!enabled) return;
        if (event.getWorld().getEnvironment() == Environment.NORMAL) {
            applyDoDaylightCycleFalse(event.getWorld());
        }
    }

    private void applyDoDaylightCycleFalse(World world) {
        Boolean current = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
        if (current == null) current = Boolean.TRUE; // default true in vanilla

        // Record only once per world to preserve the pre-plugin value
        originalDaylightCycle.putIfAbsent(world.getUID(), current);

        if (Boolean.TRUE.equals(current)) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }
    }

    private void checkTpsAndMaybeSwitch() {
        double tps1m = getTps1m();
        Mode desired = (tps1m < 20.0) ? Mode.BATCHED : Mode.SMOOTH;
        if (desired != currentMode) {
            switchToMode(desired);
        }
    }

    private void switchToMode(Mode mode) {
        if (!enabled) return;
        if (timeTask != null) { timeTask.cancel(); timeTask = null; }
        currentMode = mode;
        batchedStep = 0;

        if (mode == Mode.SMOOTH) {
            // Every 3 ticks: +1 tick (Overworld only), skip if empty
            timeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (World world : Bukkit.getWorlds()) {
                    if (world.getEnvironment() != Environment.NORMAL) continue;
                    if (world.getPlayers().isEmpty()) continue;
                    long t = world.getTime();
                    long next = (t + 1) % 24000L;
                    if (next != t) world.setTime(next);
                }
            }, 0L, 3L);
            plugin.getLogger().info("[ExtendedDay] Mode: SMOOTH (3-tick cadence)");
        } else {
            // Once per second: 7,7,6 cadence (avg 6.666.. per sec)
            timeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                int add = (batchedStep == 0 || batchedStep == 1) ? 7 : 6;
                batchedStep = (batchedStep + 1) % 3;
                for (World world : Bukkit.getWorlds()) {
                    if (world.getEnvironment() != Environment.NORMAL) continue;
                    if (world.getPlayers().isEmpty()) continue;
                    long t = world.getTime();
                    long next = (t + add) % 24000L;
                    if (next != t) world.setTime(next);
                }
            }, 0L, 20L);
            plugin.getLogger().info("[ExtendedDay] Mode: BATCHED (1-second cadence)");
        }
    }

    // Attempts to get Paper's 1-minute TPS via reflection. Falls back to 20.0.
    private double getTps1m() {
        try {
            Method m = Bukkit.getServer().getClass().getMethod("getTPS");
            Object result = m.invoke(Bukkit.getServer());
            if (result instanceof double[] arr) {
                if (arr.length > 0) return arr[0];
            } else if (result instanceof Double[] arr2) {
                if (arr2.length > 0 && arr2[0] != null) return arr2[0];
            }
        } catch (Throwable ignored) {
        }
        return 20.0; // assume healthy if unavailable
    }
}
