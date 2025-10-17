package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import me.kc1508.javacore.storage.StorageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HomeCommand implements CommandExecutor {

    private final FendorisPlugin plugin;
    private final StorageManager storage;
    private final MiniMessage miniMessage;
    private static final String FALLBACK = "§cLanguage string invalid in config.";

    private final Map<UUID, Long> lastHomeTimes = new HashMap<>();
    private final Map<UUID, Integer> teleportTasks = new HashMap<>();

    public HomeCommand(FendorisPlugin plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.miniMessage = MiniMessage.miniMessage();
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

        // Teleport delay (operators bypass or -1 disables delay)
        int delaySeconds = plugin.getConfig().getInt("home.teleport-delay-seconds", 5);
        if (player.hasPermission("fendoris.operator") || delaySeconds == -1) {
            // short staged messages for instant path
            sendHotbar(player, "home.teleport-about-to");
            Bukkit.getScheduler().runTaskLater(plugin, () -> sendHotbar(player, "home.teleport-in-progress"), 2L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                player.teleport(target);
                sendHotbar(player, "home.teleport-success");
                lastHomeTimes.put(player.getUniqueId(), System.currentTimeMillis());
            }, 4L);
            return true;
        }

        // Prevent duplicate countdowns
        if (teleportTasks.containsKey(player.getUniqueId())) {
            sendHotbar(player, "home.teleport-already-in-progress-message");
            return true;
        }

        // Start delayed teleport
        sendHotbar(player, "home.teleport-about-to");
        long delayTicks = Math.max(0L, delaySeconds) * 20L;

        // Optional pre-teleport in-progress message shortly before
        if (delayTicks >= 2L) {
            int preMsg = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (player.isOnline()) sendHotbar(player, "home.teleport-in-progress");
            }, delayTicks - 2L);
            teleportTasks.put(player.getUniqueId(), preMsg);
        }

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            try {
                if (!player.isOnline()) return;
                player.teleport(target);
                sendHotbar(player, "home.teleport-success");
                lastHomeTimes.put(player.getUniqueId(), System.currentTimeMillis());
            } finally {
                teleportTasks.remove(player.getUniqueId());
            }
        }, delayTicks);

        // Track the main task id (override with main task id)
        teleportTasks.put(player.getUniqueId(), taskId);
        
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
}
