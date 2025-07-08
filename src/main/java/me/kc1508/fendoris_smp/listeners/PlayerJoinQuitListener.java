package me.kc1508.fendoris_smp.listeners;

import me.kc1508.fendoris_smp.FendorisPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    private final FendorisPlugin plugin;

    // Cached config values
    private boolean publicJoinEnabled;
    private String publicJoinMessage;

    private boolean privateJoinEnabled;
    private String privateJoinMessage;

    private boolean publicQuitEnabled;
    private String publicQuitMessage;

    public PlayerJoinQuitListener(FendorisPlugin plugin) {
        this.plugin = plugin;
        reloadConfigCache();
    }

    public void reloadConfigCache() {
        Object pubJoinEnabledObj = plugin.getConfig().get("public-join-message-enabled");
        this.publicJoinEnabled = pubJoinEnabledObj instanceof Boolean && (Boolean) pubJoinEnabledObj;

        Object pubJoinMsgObj = plugin.getConfig().get("public-join-message");
        this.publicJoinMessage = (pubJoinMsgObj instanceof String) ? (String) pubJoinMsgObj : "null";

        Object privJoinEnabledObj = plugin.getConfig().get("private-join-message-enabled");
        this.privateJoinEnabled = privJoinEnabledObj instanceof Boolean && (Boolean) privJoinEnabledObj;

        Object privJoinMsgObj = plugin.getConfig().get("private-join-message");
        if (privJoinMsgObj instanceof String) {
            String temp = ((String) privJoinMsgObj).trim();
            if (temp.isEmpty()) {
                this.privateJoinMessage = "<red>No valid config for private-join-message.";
            } else {
                this.privateJoinMessage = temp;
            }
        } else {
            this.privateJoinMessage = "<red>No valid config for private-join-message.";
        }

        Object pubQuitEnabledObj = plugin.getConfig().get("public-quit-message-enabled");
        this.publicQuitEnabled = pubQuitEnabledObj instanceof Boolean && (Boolean) pubQuitEnabledObj;

        Object pubQuitMsgObj = plugin.getConfig().get("public-quit-message");
        this.publicQuitMessage = (pubQuitMsgObj instanceof String) ? (String) pubQuitMsgObj : "null";
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Log players joining to the console
        plugin.getLogger().info("\u001B[33mPlayer-Client Connected: " + player.getName() + "\u001B[0m");

        if (publicJoinEnabled) {
            if (publicJoinMessage.equalsIgnoreCase("null")) {
                event.joinMessage(Component.empty());
            } else {
                String replaced = publicJoinMessage.replace("%player%", player.getName());
                Component message = MiniMessage.miniMessage().deserialize(replaced);
                event.joinMessage(message);
            }
        }

        if (privateJoinEnabled) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                String rawMessage = privateJoinMessage.replace("%player%", player.getName());
                Component privateMessage = MiniMessage.miniMessage().deserialize(rawMessage);
                player.sendMessage(privateMessage);
            }, 1L); // 1 tick = 50ms delay
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Log players leaving to the console
        plugin.getLogger().info("\u001B[33mPlayer-Client Disconnected: " + player.getName() + "\u001B[0m");

        if (publicQuitEnabled) {
            if (publicQuitMessage.equalsIgnoreCase("null")) {
                event.quitMessage(Component.empty()); // explicitly remove message
            } else {
                String replaced = publicQuitMessage.replace("%player%", player.getName());
                Component message = MiniMessage.miniMessage().deserialize(replaced);
                event.quitMessage(message);
            }
        }
        // else: do nothing — allow default message
    }
}