package me.kc1508.fendoris_smp.listeners;

import me.kc1508.fendoris_smp.FendorisPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    private final FendorisPlugin plugin;
    private FileConfiguration config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // Cached config values
    private boolean privateJoinMessageEnabled;
    private String privateJoinMessage;
    private boolean publicJoinMessageEnabled;
    private String publicJoinMessage;
    private boolean publicQuitMessageEnabled;
    private String publicQuitMessage;
    private String publicOperatorJoinMessage;
    private String publicOperatorQuitMessage;
    private boolean opCommandLogsEnabled;
    private boolean sessionCodeEnabled;

    public PlayerJoinQuitListener(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        reloadConfigCache();
    }

    /**
     * Reloads cached config values from plugin config.yml
     */
    public void reloadConfigCache() {
        this.config = plugin.getConfig();

        this.privateJoinMessageEnabled = config.getBoolean("private-join-message-enabled", true);
        this.privateJoinMessage = config.getString("private-join-message", "");

        this.publicJoinMessageEnabled = config.getBoolean("public-join-message-enabled", true);
        this.publicJoinMessage = config.getString("public-join-message", "");

        this.publicQuitMessageEnabled = config.getBoolean("public-quit-message-enabled", true);
        this.publicQuitMessage = config.getString("public-quit-message", "");

        this.publicOperatorJoinMessage = config.getString("public-operator-join-message", "");
        this.publicOperatorQuitMessage = config.getString("public-operator-quit-message", "");

        this.opCommandLogsEnabled = config.getBoolean("operator-command-logs-enabled", false);
        this.sessionCodeEnabled = config.getBoolean("session-code-enabled", false);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        @SuppressWarnings("UnstableApiUsage")
        String version = plugin.getPluginMeta().getVersion();

        if (privateJoinMessageEnabled) {
            String privateMessageRaw = privateJoinMessage
                    .replace("%player%", player.getName())
                    .replace("%version%", version)
                    .replace("<newline>", "\n");

            if (!privateMessageRaw.isBlank()) {
                player.sendMessage(miniMessage.deserialize(privateMessageRaw));
            }
        }

        if (publicJoinMessageEnabled) {
            String raw;
            if (player.isOp() && !publicOperatorJoinMessage.isBlank()) {
                raw = publicOperatorJoinMessage.replace("%player%", player.getName());
            } else {
                raw = publicJoinMessage.replace("%player%", player.getName());
            }

            if (!raw.isBlank()) {
                event.joinMessage(miniMessage.deserialize(raw));
            }
        } else {
            event.joinMessage(Component.empty());
        }

        if (opCommandLogsEnabled) {
            if (sessionCodeEnabled && plugin.getTabListManager() != null) {
                String sessionCode = plugin.getTabListManager().getSessionCode(player);
                plugin.getLogger().info(player.getName() + " client connected [Session: " + sessionCode + "]");
            } else {
                plugin.getLogger().info(player.getName() + " client connected");
            }
        }

        if (plugin.getTabListManager() != null && plugin.getConfig().getBoolean("tablist-enabled", false)) {
            plugin.getTabListManager().updateTabList(player);
            plugin.getTabListManager().assignPlayerToTeam(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (publicQuitMessageEnabled) {
            String raw;
            if (player.isOp() && !publicOperatorQuitMessage.isBlank()) {
                raw = publicOperatorQuitMessage.replace("%player%", player.getName());
            } else {
                raw = publicQuitMessage.replace("%player%", player.getName());
            }

            if (!raw.isBlank()) {
                event.quitMessage(miniMessage.deserialize(raw));
            }
        } else {
            event.quitMessage(Component.empty());
        }

        if (opCommandLogsEnabled) {
            if (sessionCodeEnabled && plugin.getTabListManager() != null) {
                String sessionCode = plugin.getTabListManager().getSessionCode(player);
                plugin.getLogger().info(player.getName() + " disconnected [Session: " + sessionCode + "]");
            } else {
                plugin.getLogger().info(player.getName() + " disconnected");
            }
        }

        if (plugin.getTabListManager() != null) {
            plugin.getTabListManager().clearSessionCode(player.getUniqueId());
        }
    }
}
