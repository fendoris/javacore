package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SpawnCommand implements CommandExecutor, TabCompleter {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage;
    private static final String FALLBACK = "§cLanguage string invalid in config.";

    private final Map<UUID, Long> lastSpawnTimes = new HashMap<>();

    public SpawnCommand(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!plugin.getConfig().getBoolean("spawn.enabled", false)) {
            sendMessage(sender, "spawn.disabled-message", "<red>The Spawn system is currently disabled on this server.<reset>");
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "spawn.only-player-message", "<red>Only players can use this command.<reset>");
                return true;
            }

            if (args.length > 0) {
                sendMessage(player, "spawn.usage-message", "<red>Usage: /spawn (Teleports you to the server spawn location)<reset>");
                return true;
            }

            int cooldownSec = plugin.getConfig().getInt("spawn.cooldown", 0);
            if (cooldownSec > 0) {
                UUID pu = player.getUniqueId();
                long now = System.currentTimeMillis();
                long last = lastSpawnTimes.getOrDefault(pu, 0L);
                long elapsed = now - last;
                long cooldownMillis = TimeUnit.SECONDS.toMillis(cooldownSec);

                if (elapsed < cooldownMillis) {
                    long remainingSec = (cooldownMillis - elapsed + 999) / 1000;
                    if (remainingSec < 1) {
                        sendMessage(player, "spawn.cooldown-message-less-than-1", "<red>You must wait less than 1s to use /spawn again.<reset>");
                    } else {
                        sendMessage(player, "spawn.cooldown-message", "<red>You must wait %seconds%s to use /spawn again.<reset>", "%seconds%", String.valueOf(remainingSec));
                    }
                    return true;
                }

                lastSpawnTimes.put(pu, now);
            }

            teleportToSpawn(player);
            sendMessage(player, "spawn.teleport-success-message", "<green>You have been teleported to the server spawn.<reset>");
            return true;
        }

        if (command.getName().equalsIgnoreCase("sendtospawn")) {
            if (!sender.hasPermission("fendoris.operator")) {
                sendMessage(sender, "spawn.no-permission-message", "<red>You don't have permission to use this command.<reset>");
                return true;
            }

            if (args.length != 1) {
                sendMessage(sender, "spawn.sendtospawn-usage", "<red>Usage: /sendtospawn <player><reset>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sendMessage(sender, "spawn.player-not-online", "<red>That player is not online.<reset>");
                return true;
            }

            teleportToSpawn(target);
            sendMessage(target, "spawn.sent-to-spawn-message", "<green>You have been sent to spawn by an operator.<reset>");
            sendMessage(sender, "spawn.operator-sent-message", "<green>Sent %player% to spawn.<reset>", "%player%", target.getName());
            return true;
        }

        return false;
    }

    private void teleportToSpawn(Player player) {
        String worldName = plugin.getConfig().getString("spawn.location-world", player.getWorld().getName());
        World world = Bukkit.getWorld(worldName);
        if (world == null) world = player.getWorld();

        double x = plugin.getConfig().getDouble("spawn.location-x", 0.5);
        double y = plugin.getConfig().getDouble("spawn.location-y", 100.0);
        double z = plugin.getConfig().getDouble("spawn.location-z", 0.5);
        float yaw = (float) plugin.getConfig().getDouble("spawn.location-yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("spawn.location-pitch", 0.0);

        Location spawnLocation = new Location(world, x, y, z, yaw, pitch);
        player.teleport(spawnLocation);
    }

    private void sendMessage(CommandSender sender, String key, String defaultValue, String... replacements) {
        String rawMessage = plugin.getConfig().getString(key);
        if (rawMessage == null || rawMessage.isBlank()) {
            plugin.getConfig().set(key, defaultValue);
            plugin.saveConfig();
            rawMessage = defaultValue;
        }

        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                rawMessage = rawMessage.replace(replacements[i], replacements[i + 1]);
            }
        } else {
            plugin.getLogger().warning("[Spawn] Replacement array length is not even for key: " + key);
        }

        try {
            sender.sendMessage(miniMessage.deserialize(rawMessage));
        } catch (Exception e) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Spawn] Invalid message format for key: " + key + " -> " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        return Collections.emptyList();
    }
}
