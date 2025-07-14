package me.kc1508.fendoris_smp.commands;

import me.kc1508.fendoris_smp.FendorisPlugin;
import me.kc1508.fendoris_smp.listeners.PlayerJoinQuitListener;
import me.kc1508.fendoris_smp.listeners.AllowedCommandListener;
import me.kc1508.fendoris_smp.listeners.ServerPingListener;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    private final FendorisPlugin plugin;
    private final PlayerJoinQuitListener listener;
    private final AllowedCommandListener allowedCommandListener;
    private final ServerPingListener serverPingListener;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ReloadCommand(FendorisPlugin plugin, PlayerJoinQuitListener listener, AllowedCommandListener allowedCommandListener, ServerPingListener serverPingListener) {
        this.plugin = plugin;
        this.listener = listener;
        this.allowedCommandListener = allowedCommandListener;
        this.serverPingListener = serverPingListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("fendoris.reload")) {
            sender.sendMessage("§cYou don't have permission to reload the plugin.");
            return true;
        }

        plugin.reloadConfig();
        listener.reloadConfigCache();
        allowedCommandListener.reloadBlockedCommands();
        serverPingListener.reload();

        if (plugin.getTabListManager() != null) {
            // Ensure teams exist on reload before restarting tablist
            plugin.getTabListManager().ensureTeamsExist();

            plugin.getTabListManager().stop();
            plugin.getTabListManager().reloadConfigSettings();

            if (plugin.getConfig().getBoolean("tablist-enabled", false)) {
                plugin.getTabListManager().start();
            }
        }

        sendConfigMessageOrError(sender, "reload-success-message");

        String senderName = (sender instanceof Player p) ? p.getName() : "Console";

        String broadcastMessage = plugin.getConfig().getString("operator-reload-broadcast-message");
        if (broadcastMessage == null || broadcastMessage.isBlank()) {
            sendConfigMessageOrError(sender, "operator-reload-broadcast-message");
        } else {
            plugin.broadcastToOPsExceptSender(senderName,
                    "operator-reload-broadcast-message",
                    broadcastMessage,
                    "%player%", senderName);
        }

        return true;
    }

    private void sendConfigMessageOrError(CommandSender sender, String key, String... replacements) {
        String rawMessage = plugin.getConfig().getString(key);
        if (rawMessage == null || rawMessage.isBlank()) {
            sender.sendMessage("§cERROR: Missing or empty config message for '" + key + "'!");
            return;
        }

        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = replacements[i];
                String replacement = replacements[i + 1];
                if (rawMessage.contains(placeholder)) {
                    rawMessage = rawMessage.replace(placeholder, replacement);
                }
            }
        }

        Component messageComponent = miniMessage.deserialize(rawMessage);
        if (sender instanceof Player p) {
            p.sendMessage(messageComponent);
        } else {
            sender.sendMessage(messageComponent.toString());
        }
    }
}
