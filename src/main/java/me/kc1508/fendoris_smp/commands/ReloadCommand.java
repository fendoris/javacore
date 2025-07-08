package me.kc1508.fendoris_smp.commands;

import me.kc1508.fendoris_smp.FendorisPlugin;
import me.kc1508.fendoris_smp.listeners.PlayerJoinQuitListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
            sender.sendMessage("§cYou don't have permission to reload the plugin.");
            return true;
        }

        plugin.reloadConfig();
        listener.reloadConfigCache();

        sender.sendMessage("§aFendoris plugin config reloaded successfully.");
        return true;
    }
}
