package me.kc1508.fendoris_smp.commands;

import me.kc1508.fendoris_smp.FendorisPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SessionCommand implements CommandExecutor {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage;

    public SessionCommand(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        if (args.length > 0) {
            player.sendMessage("§cUsage: /session (Shows your current Session Code)");
            return true;
        }

        if (!plugin.getConfig().getBoolean("session-code-enabled", false)) {
            player.sendMessage("Session code feature is currently disabled.");
            return true;
        }

        String sessionCode = plugin.getTabListManager().getSessionCode(player);
        String rawMessage = plugin.getConfig().getString("session-code-message", "Your session code is %session_code%");
        rawMessage = rawMessage.replace("%session_code%", sessionCode);

        Component messageComponent = miniMessage.deserialize(rawMessage);
        player.sendMessage(messageComponent);

        return true;
    }
}
