package me.kc1508.fendoris_smp.listeners;

import me.kc1508.fendoris_smp.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AllowedCommandListener implements Listener {

    private final FendorisPlugin plugin;
    private Set<String> allowedCommands;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public AllowedCommandListener(FendorisPlugin plugin) {
        this.plugin = plugin;
        reloadBlockedCommands();
    }

    public void reloadBlockedCommands() {
        List<String> list = plugin.getConfig().getStringList("allowed-commands");
        allowedCommands = list.stream()
                .filter(cmd -> cmd != null && !cmd.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @EventHandler
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        // Use positive logic
        if (!plugin.isCommandWhitelistEnabled()) return;

        if (event.getPlayer().hasPermission("fendoris.bypasscommandwhitelist")) return;

        if (allowedCommands.isEmpty()) {
            event.getCommands().clear();
        } else {
            event.getCommands().removeIf(cmd -> !allowedCommands.contains(cmd.toLowerCase()));
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        // Use positive logic
        if (!plugin.isCommandWhitelistEnabled()) return;

        if (event.getPlayer().hasPermission("fendoris.bypasscommandwhitelist")) return;

        String message = event.getMessage();
        if (message.length() < 2) return;

        String baseCmd = message.substring(1).split(" ")[0].toLowerCase();

        if (baseCmd.contains(":")) {
            baseCmd = baseCmd.substring(baseCmd.indexOf(":") + 1);
        }

        if (!allowedCommands.contains(baseCmd)) {
            event.setCancelled(true);

            String fallback = "<red>Unknown or disallowed command.";
            String raw = plugin.getConfig().getString("disallowed-command-message", fallback);

            event.getPlayer().sendMessage(miniMessage.deserialize(raw));
        }
    }
}
