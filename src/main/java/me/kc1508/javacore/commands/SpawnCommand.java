package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SpawnCommand implements CommandExecutor, TabCompleter, Listener {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage;
    private static final String FALLBACK = "§cLanguage string invalid in config.";

    private final Map<UUID, Long> lastSpawnTimes = new HashMap<>();
    private final Map<UUID, Location> teleportStartLocation = new HashMap<>();
    private final Map<UUID, Integer> teleportTasks = new HashMap<>();

    public SpawnCommand(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!plugin.getConfig().getBoolean("spawn.enabled", false)) {
            sendMessage(sender, "spawn.disabled-message");
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "spawn.only-player-message");
                return true;
            }

            if (args.length > 0) {
                sendMessage(player, "spawn.usage-message");
                return true;
            }

            // Cooldown check (bypass for operators)
            int cooldownSec = plugin.getConfig().getInt("spawn.cooldown", 0);
            if (cooldownSec > 0 && !player.hasPermission("fendoris.operator")) {
                UUID pu = player.getUniqueId();
                long now = System.currentTimeMillis();
                long last = lastSpawnTimes.getOrDefault(pu, 0L);
                long elapsed = now - last;
                long cooldownMillis = TimeUnit.SECONDS.toMillis(cooldownSec);

                if (elapsed < cooldownMillis) {
                    long remainingSec = (cooldownMillis - elapsed + 999) / 1000;
                    if (remainingSec < 1) {
                        sendMessage(player, "spawn.cooldown-message-less-than-1");
                    } else {
                        sendMessage(player, "spawn.cooldown-message", "%seconds%", String.valueOf(remainingSec));
                    }
                    return true;
                }
            }

            // Teleport delay (bypass for operators)
            int delaySeconds = plugin.getConfig().getInt("spawn.teleport-delay-seconds", 0);
            if (delaySeconds <= 0 || player.hasPermission("fendoris.operator")) {
                teleportToSpawn(player);
                sendMessage(player, "spawn.teleport-success-message");
                // set cooldown only after success
                lastSpawnTimes.put(player.getUniqueId(), System.currentTimeMillis());
                return true;
            }

            // If already teleporting, do not restart
            if (teleportTasks.containsKey(player.getUniqueId())) {
                sendMessage(player, "spawn.teleport-already-in-progress-message");
                return true;
            }

            // Start a new countdown
            startTeleportCountdown(player, delaySeconds);
            return true;
        }

        if (command.getName().equalsIgnoreCase("sendtospawn")) {
            if (!sender.hasPermission("fendoris.operator")) {
                sendMessage(sender, "spawn.no-permission-message");
                return true;
            }

            if (args.length != 1) {
                sendMessage(sender, "spawn.sendtospawn-usage");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sendMessage(sender, "spawn.player-not-online");
                return true;
            }

            teleportToSpawn(target);
            sendMessage(target, "spawn.sent-to-spawn-message");
            sendMessage(sender, "spawn.operator-sent-message", "%player%", target.getName());
            return true;
        }

        return false;
    }

    private void startTeleportCountdown(Player player, int delaySeconds) {
        UUID uuid = player.getUniqueId();
        teleportStartLocation.put(uuid, player.getLocation().clone());

        sendMessage(player, "spawn.teleport-delay-start-message", "%seconds%", String.valueOf(delaySeconds));

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            int secondsLeft = delaySeconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelTeleport(uuid);
                    return;
                }

                // Sound effect
                if (plugin.getConfig().getBoolean("spawn.teleport-sound-enabled", true)) {
                    NamespacedKey key = NamespacedKey.fromString(plugin.getConfig().getString("spawn.teleport-sound-name", "minecraft:block.note_block.pling"));
                    if (key != null && Registry.SOUNDS.get(key) != null) {
                        player.playSound(player.getLocation(), Objects.requireNonNull(Registry.SOUNDS.get(key)),
                                (float) plugin.getConfig().getDouble("spawn.teleport-sound-volume", 1.0),
                                (float) plugin.getConfig().getDouble("spawn.teleport-sound-pitch", 1.0));
                    }
                }

                // Particle effect
                if (plugin.getConfig().getBoolean("spawn.teleport-particles-enabled", true)) {
                    NamespacedKey key = NamespacedKey.fromString(plugin.getConfig().getString("spawn.teleport-particle-name", "minecraft:portal"));
                    if (key != null && Registry.PARTICLE_TYPE.get(key) != null) {
                        player.getWorld().spawnParticle(Objects.requireNonNull(Registry.PARTICLE_TYPE.get(key)), player.getLocation(),
                                plugin.getConfig().getInt("spawn.teleport-particle-count", 20), 0.5, 1, 0.5, 0.01);
                    }
                }

                secondsLeft--;
                if (secondsLeft <= 0) {
                    teleportToSpawn(player);
                    sendMessage(player, "spawn.teleport-success-message");
                    lastSpawnTimes.put(uuid, System.currentTimeMillis()); // set cooldown only after success
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
        // to is never null in modern API
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            sendMessage(player, "spawn.teleport-cancelled-message");
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

    private void teleportToSpawn(Player player) {
        String worldName = plugin.getConfig().getString("spawn.location-world", player.getWorld().getName());
        World world = Bukkit.getWorld(worldName);
        if (world == null) world = player.getWorld();

        double x = plugin.getConfig().getDouble("spawn.location-x", 0.5);
        double y = plugin.getConfig().getDouble("spawn.location-y", 100.0);
        double z = plugin.getConfig().getDouble("spawn.location-z", 0.5);
        float yaw = (float) plugin.getConfig().getDouble("spawn.location-yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("spawn.location-pitch", 0.0);

        player.teleport(new Location(world, x, y, z, yaw, pitch));
    }

    private void sendMessage(CommandSender sender, String key, String... replacements) {
        String rawMessage = plugin.getConfig().getString(key);
        if (rawMessage == null || rawMessage.isBlank()) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Spawn] Missing config key: " + key);
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
            plugin.getLogger().warning("[Spawn] Invalid message format for key: " + key + " -> " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("sendtospawn") && args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(partial)) {
                    matches.add(player.getName());
                }
            }
            return matches;
        }
        return Collections.emptyList();
    }
}
