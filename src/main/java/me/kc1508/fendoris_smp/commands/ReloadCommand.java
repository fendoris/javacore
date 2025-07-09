package me.kc1508.fendoris_smp.commands;

import me.kc1508.fendoris_smp.FendorisPlugin;
import me.kc1508.fendoris_smp.listeners.PlayerJoinQuitListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    private final FendorisPlugin plugin;
    private final PlayerJoinQuitListener listener;

    public ReloadCommand(FendorisPlugin plugin, PlayerJoinQuitListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("fendoris.reload")) {
            sender.sendMessage("§cYou don't have permission to reload the plugin."); // Permission message could be added to config too if desired
            return true;
        }

        plugin.reloadConfig();
        listener.reloadConfigCache();

        String successMessage = plugin.getConfig().getString("reload-success-message");
        if (successMessage == null || successMessage.isBlank()) {
            sender.sendMessage("§cERROR: reload-success-message missing from config!");
        } else {
            sender.sendMessage(successMessage);
        }

        String senderName = (sender instanceof Player p) ? p.getName() : "Console";

        String broadcastMessage = plugin.getConfig().getString("admin-reload-broadcast-message");
        if (broadcastMessage == null || broadcastMessage.isBlank()) {
            sender.sendMessage("§cERROR: admin-reload-broadcast-message missing from config!");
        } else {
            plugin.broadcastToAdminsExceptSender(senderName,
                    "admin-reload-broadcast-message",
                    broadcastMessage,
                    "%player%", senderName);
        }
        return true;
    }
}
