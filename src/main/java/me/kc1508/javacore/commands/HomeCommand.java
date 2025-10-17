package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import me.kc1508.javacore.storage.StorageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HomeCommand implements CommandExecutor, Listener {

    private final FendorisPlugin plugin;
    private final StorageManager storage;
    private final MiniMessage miniMessage;
    private static final String FALLBACK = "§cLanguage string invalid in config.";

    private final Map<UUID, Long> lastHomeTimes = new HashMap<>();
    private final Map<UUID, Integer> teleportTasks = new HashMap<>();
    private final Map<UUID, Location> teleportStartLocation = new HashMap<>();

    public HomeCommand(FendorisPlugin plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.miniMessage = MiniMessage.miniMessage();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sendChat(sender, "home.only-player-message");
            return true;
        }

        if (args.length > 0) {
            sendChat(player, "home.usage-message");
            return true;
        }

        // Cooldown check (bypass for operators)
        int cooldownSec = Math.max(0, plugin.getConfig().getInt("home.cooldown-seconds", 60));
        if (cooldownSec > 0 && !player.hasPermission("fendoris.operator")) {
            UUID pu = player.getUniqueId();
            long now = System.currentTimeMillis();
            long last = lastHomeTimes.getOrDefault(pu, 0L);
            long elapsed = now - last;
            long cooldownMillis = TimeUnit.SECONDS.toMillis(cooldownSec);

            if (elapsed < cooldownMillis) {
                long remainingSec = (cooldownMillis - elapsed + 999) / 1000;
                if (remainingSec < 1) {
                    sendChat(player, "home.cooldown-message-less-than-1");
                } else {
                    sendChat(player, "home.cooldown-message", "%seconds%", String.valueOf(remainingSec));
                }
                return true;
            }
        }

        // Determine target location (home or spawn fallback)
        Location target = storage.getHome(player.getUniqueId()).orElseGet(() -> getSpawnFromConfig(player));

        // Teleport delay handling (operators bypass, -1 disables)
        int delaySeconds = plugin.getConfig().getInt("home.teleport-delay-seconds", 5);
        if (delaySeconds <= 0 || player.hasPermission("fendoris.operator")) {
            // Operator or no delay: teleport immediately; only show success (avoid staged spam)
            if (!player.isOnline()) return true;
            player.teleport(target);
            sendHotbar(player, "home.teleport-success");
            lastHomeTimes.put(player.getUniqueId(), System.currentTimeMillis());
            return true;
        }

        // Prevent duplicate countdowns
        if (teleportTasks.containsKey(player.getUniqueId())) {
            sendHotbar(player, "home.teleport-already-in-progress-message");
            return true;
        }

        // Start a new countdown similar to /spawn
        startTeleportCountdown(player, target, delaySeconds);
        return true;
    }

    private Location getSpawnFromConfig(Player player) {
        String worldName = plugin.getConfig().getString("spawn.location-world", player.getWorld().getName());
        World world = Bukkit.getWorld(worldName);
        if (world == null) world = player.getWorld();

        double x = plugin.getConfig().getDouble("spawn.location-x", 0.5);
        double y = plugin.getConfig().getDouble("spawn.location-y", 100.0);
        double z = plugin.getConfig().getDouble("spawn.location-z", 0.5);
        float yaw = (float) plugin.getConfig().getDouble("spawn.location-yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("spawn.location-pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void sendChat(CommandSender sender, String key, String... replacements) {
        String rawMessage = plugin.getConfig().getString(key);
        if (rawMessage == null || rawMessage.isBlank()) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Home] Missing config key: " + key);
            return;
        }

        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                rawMessage = rawMessage.replace(replacements[i], replacements[i + 1]);
            }
        }
        try {
            sender.sendMessage(miniMessage.deserialize(rawMessage));
        } catch (Exception e) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Home] Invalid message format for key: " + key + " -> " + e.getMessage());
        }
    }

    private void sendHotbar(Player player, String key, String... replacements) {
        String rawMessage = plugin.getConfig().getString(key);
        if (rawMessage == null || rawMessage.isBlank()) {
            player.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Home] Missing config key: " + key);
            return;
        }
        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                rawMessage = rawMessage.replace(replacements[i], replacements[i + 1]);
            }
        }
        try {
            player.sendActionBar(miniMessage.deserialize(rawMessage));
        } catch (Exception e) {
            player.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Home] Invalid message format for key: " + key + " -> " + e.getMessage());
        }
    }

    private void startTeleportCountdown(Player player, Location target, int delaySeconds) {
        UUID uuid = player.getUniqueId();
        teleportStartLocation.put(uuid, player.getLocation().clone());

        // Start message with remaining time (hotbar)
        sendHotbar(player, "home.teleport-delay-start-message", "%seconds%", String.valueOf(delaySeconds));

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            int secondsLeft = delaySeconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelTeleport(uuid);
                    return;
                }

                // Play sound each second if enabled (mirror spawn)
                if (plugin.getConfig().getBoolean("home.teleport-sound-enabled", true)) {
                    NamespacedKey key = NamespacedKey.fromString(plugin.getConfig().getString("home.teleport-sound-name", "minecraft:block.note_block.pling"));
                    if (key != null && Registry.SOUNDS.get(key) != null) {
                        player.playSound(player.getLocation(), Objects.requireNonNull(Registry.SOUNDS.get(key)),
                                (float) plugin.getConfig().getDouble("home.teleport-sound-volume", 1.0),
                                (float) plugin.getConfig().getDouble("home.teleport-sound-pitch", 1.0));
                    }
                }

                // Particles each second if enabled
                if (plugin.getConfig().getBoolean("home.teleport-particles-enabled", true)) {
                    NamespacedKey key = NamespacedKey.fromString(plugin.getConfig().getString("home.teleport-particle-name", "minecraft:portal"));
                    if (key != null && Registry.PARTICLE_TYPE.get(key) != null) {
                        player.getWorld().spawnParticle(Objects.requireNonNull(Registry.PARTICLE_TYPE.get(key)), player.getLocation(),
                                plugin.getConfig().getInt("home.teleport-particle-count", 20), 0.5, 1, 0.5, 0.01);
                    }
                }

                secondsLeft--;
                if (secondsLeft <= 0) {
                    player.teleport(target);
                    sendHotbar(player, "home.teleport-success");
                    lastHomeTimes.put(uuid, System.currentTimeMillis());
                    cancelTeleport(uuid);
                }
            }
        }, 0L, 20L);

        teleportTasks.put(uuid, taskId);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!teleportStartLocation.containsKey(uuid)) return;

        Location from = teleportStartLocation.get(uuid);
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            sendHotbar(player, "home.teleport-cancelled-message");
            cancelTeleport(uuid);
        }
    }

    private void cancelTeleport(UUID uuid) {
        if (teleportTasks.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(teleportTasks.get(uuid));
            teleportTasks.remove(uuid);
        }
        teleportStartLocation.remove(uuid);
    }
}
