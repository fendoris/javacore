package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import me.kc1508.javacore.listeners.AllowedCommandListener;
import me.kc1508.javacore.listeners.PlayerJoinQuitListener;
import me.kc1508.javacore.listeners.ServerPingListener;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import me.kc1508.javacore.hologram.HologramManager;

public class ReloadCommand implements CommandExecutor {

    private final FendorisPlugin plugin;
    private final AllowedCommandListener allowedCommandListener;
    private final MiniMessage miniMessage;

    public ReloadCommand(FendorisPlugin plugin,
                         PlayerJoinQuitListener listener, // still passed for future use
                         AllowedCommandListener allowedCommandListener,
                         ServerPingListener serverPingListener) {
        this.plugin = plugin;
        this.allowedCommandListener = allowedCommandListener;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NotNull [] args) {

        // Only allow console or operators
        if (sender instanceof org.bukkit.entity.Player player && !player.hasPermission("fendoris.operator")) {
            sendMiniMessage(sender, "reload.no-permission", "<red>You don't have permission to reload the plugin.</red>");
            return true;
        }

        // Reload config & validate
        plugin.reloadConfig();
        new me.kc1508.javacore.config.ConfigValidator(plugin).validate();

        // Holograms: reload from config (despawn + respawn to avoid dupes)
        me.kc1508.javacore.hologram.HologramManager hm = plugin.getHologramManager();
        if (hm != null) hm.reload();

        // Reload listeners that need it
        allowedCommandListener.reloadBlockedCommands();

        // Notify sender
        sendMiniMessage(sender, "reload.reload-success", "<gold>Config reloaded.</gold>");

        // Broadcast reload message to other operators
        String broadcast = plugin.getConfig().getString("reload.reload-broadcast", "[Plugin] %player% reloaded config");
        broadcast = broadcast.replace("%player%", sender.getName());
        final String finalBroadcast = broadcast; // effectively final for lambda

        plugin.getServer().getOnlinePlayers().forEach(p -> {
            if (p.hasPermission("fendoris.operator") && !p.getName().equalsIgnoreCase(sender.getName())) {
                try {
                    p.sendMessage(miniMessage.deserialize(finalBroadcast));
                } catch (Exception ex) {
                    p.sendMessage(finalBroadcast);
                }
            }
        });

        return true;
    }

    private void sendMiniMessage(CommandSender sender, String key, String fallback) {
        String msg = plugin.getConfig().getString(key, fallback);
        try {
            sender.sendMessage(miniMessage.deserialize(msg));
        } catch (Exception e) {
            sender.sendMessage(fallback);
        }
    }
}
