package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import me.kc1508.javacore.listeners.PlayerJoinQuitListener;
import me.kc1508.javacore.listeners.AllowedCommandListener;
import me.kc1508.javacore.listeners.ServerPingListener;
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

    private static final String FALLBACK = "§cLanguage string invalid in config.";

    public ReloadCommand(FendorisPlugin plugin, PlayerJoinQuitListener listener,
                         AllowedCommandListener allowedCommandListener,
                         ServerPingListener serverPingListener) {
        this.plugin = plugin;
        this.listener = listener;
        this.allowedCommandListener = allowedCommandListener;
        this.serverPingListener = serverPingListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {
        if (!sender.hasPermission("fendoris.operator.reload")) {
            sendMessageRaw(sender, "reload.no-permission");
            return true;
        }

        plugin.reloadConfig();
        listener.reloadConfigCache();
        allowedCommandListener.reloadBlockedCommands();
        serverPingListener.reload();

        if (plugin.getTabListManager() != null) {
            plugin.getTabListManager().ensureTeamsExist();
            plugin.getTabListManager().stop();
            plugin.getTabListManager().reloadConfigSettings();

            if (plugin.getConfig().getBoolean("tablist-enabled", false)) {
                plugin.getTabListManager().start();
            }
        }

        sendMessageRaw(sender, "reload.reload-success");

        String senderName = (sender instanceof Player p) ? p.getName() : "Console";

        String configMessage = plugin.getConfig().getString("reload.reload-broadcast");

        if (configMessage == null || configMessage.isBlank()) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Feature: ReloadCommand] Missing or empty config key: reload.reload-broadcast");
        } else {
            plugin.broadcastToOPsExceptSender(
                    senderName,
                    "reload.reload-broadcast",
                    "%player%", senderName
            );
        }

        return true;
    }

    private void sendMessageRaw(CommandSender sender, String key, String... replacements) {
        String rawMessage = plugin.getConfig().getString(key);
        if (rawMessage == null || rawMessage.isBlank()) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Feature: ReloadCommand] Missing or empty config key: " + key);
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
        } else {
            plugin.getLogger().warning("[Feature: ReloadCommand] Replacement array length is not even!");
        }

        Component messageComponent = miniMessage.deserialize(rawMessage);
        if (sender instanceof Player p) {
            p.sendMessage(messageComponent);
        } else {
            sender.sendMessage(messageComponent.toString());
        }
    }
}
