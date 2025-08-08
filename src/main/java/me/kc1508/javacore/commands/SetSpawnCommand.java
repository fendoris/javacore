package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetSpawnCommand implements CommandExecutor {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage;
    private static final String FALLBACK = "§cLanguage string invalid in config.";

    public SetSpawnCommand(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "spawn.only-player-message", "<red>Only players can use this command.<reset>");
            return true;
        }

        if (!player.hasPermission("fendoris.operator")) {
            sendMessage(player, "spawn.no-permission-message", "<red>You don't have permission to use this command.<reset>");
            return true;
        }

        // save player's current location into config
        plugin.getConfig().set("spawn.location-world", player.getWorld().getName());
        plugin.getConfig().set("spawn.location-x", player.getLocation().getX());
        plugin.getConfig().set("spawn.location-y", player.getLocation().getY());
        plugin.getConfig().set("spawn.location-z", player.getLocation().getZ());
        plugin.getConfig().set("spawn.location-yaw", player.getLocation().getYaw());
        plugin.getConfig().set("spawn.location-pitch", player.getLocation().getPitch());
        plugin.saveConfig();

        sendMessage(player, "spawn.set-success", "<green>Spawn location saved to config.<reset>");
        return true;
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
        }

        try {
            sender.sendMessage(miniMessage.deserialize(rawMessage));
        } catch (Exception e) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[SetSpawn] Invalid message format for key: " + key);
        }
    }
}
