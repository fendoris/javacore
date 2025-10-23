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
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.GameMode;
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
    private final Map<UUID, BukkitTask> pendingNightSkips = new HashMap<>();
    private static final long SLEEP_SKIP_DELAY_TICKS = 100L; // ~5 seconds, mimics vanilla feel

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
        Mode desired;
        if (tps1m < 19.5) {
            desired = Mode.BATCHED;
        } else if (tps1m > 19.5) {
            desired = Mode.SMOOTH;
        } else {
            // exactly 19.5 — hold current; if none yet, prefer SMOOTH
            desired = (currentMode != null) ? currentMode : Mode.SMOOTH;
        }
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

    // ---- Sleep handling to skip night while DO_DAYLIGHT_CYCLE is false ----
    @EventHandler(ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!enabled) return;
        World world = event.getPlayer().getWorld();
        if (world.getEnvironment() != Environment.NORMAL) return;
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;

        // Re-evaluate after the sleeping state is applied this tick
        Bukkit.getScheduler().runTask(plugin, () -> scheduleNightSkipIfReady(world));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBedLeave(PlayerBedLeaveEvent event) {
        if (!enabled) return;
        World world = event.getPlayer().getWorld();
        if (world.getEnvironment() != Environment.NORMAL) return;
        cancelPendingNightSkip(world);
    }

    @EventHandler(ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled) return;
        World world = event.getPlayer().getWorld();
        if (world.getEnvironment() != Environment.NORMAL) return;
        // Player leaving can break the threshold; cancel if we had scheduled
        cancelPendingNightSkip(world);
    }

    private void scheduleNightSkipIfReady(World world) {
        if (!isNightOrThundering(world)) return;

        SleepCounts counts = computeSleepCounts(world);
        if (counts.eligible == 0) return;

        int required = requiredSleepers(world, counts.eligible);
        if (counts.sleeping < required) {
            cancelPendingNightSkip(world);
            return;
        }

        // If already pending, do nothing; otherwise schedule with vanilla-like delay
        pendingNightSkips.computeIfAbsent(world.getUID(), k ->
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                pendingNightSkips.remove(world.getUID());
                // Re-check conditions at execution time
                if (!isNightOrThundering(world)) return;
                SleepCounts now = computeSleepCounts(world);
                if (now.eligible == 0) return;
                if (now.sleeping < requiredSleepers(world, now.eligible)) return;

                long next = ((world.getFullTime() / 24000L) + 1L) * 24000L + 1000L; // morning
                world.setFullTime(next);
                world.setStorm(false);
                world.setThundering(false);
            }, SLEEP_SKIP_DELAY_TICKS)
        );
    }

    private void cancelPendingNightSkip(World world) {
        BukkitTask pending = pendingNightSkips.remove(world.getUID());
        if (pending != null) pending.cancel();
    }

    private boolean isNightOrThundering(World world) {
        long t = world.getTime() % 24000L;
        boolean isNight = t >= 12541L && t <= 23458L; // vanilla-ish window
        return isNight || world.isThundering();
    }

    private int requiredSleepers(World world, int eligible) {
        Integer perc = world.getGameRuleValue(GameRule.PLAYERS_SLEEPING_PERCENTAGE);
        int percentage = (perc != null) ? Math.max(0, Math.min(100, perc)) : 100;
        int required = (int) Math.ceil((percentage / 100.0) * eligible);
        if (required <= 0) required = 1;
        return required;
    }

    private record SleepCounts(int eligible, int sleeping) {}

    private SleepCounts computeSleepCounts(World world) {
        int eligible = 0;
        int sleeping = 0;
        for (var p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            if (p.isSleepingIgnored()) continue;
            eligible++;
            if (p.isSleeping()) sleeping++;
        }
        return new SleepCounts(eligible, sleeping);
    }
}
