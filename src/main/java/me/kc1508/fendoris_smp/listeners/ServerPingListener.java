package me.kc1508.fendoris_smp.listeners;

import me.kc1508.fendoris_smp.FendorisPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.net.InetAddress;

public class ServerPingListener implements Listener {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private String rawMotd;
    private boolean pingLoggingEnabled;

    public ServerPingListener(FendorisPlugin plugin) {
        this.plugin = plugin;
        reload(); // Load initial config values
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (pingLoggingEnabled) {
            InetAddress address = event.getAddress();
            //noinspection ConstantConditions
            String ip = (address != null) ? address.getHostAddress() : "unknown";
            plugin.getLogger().info("Server List Pinged from IP: " + ip);
        }

        try {
            Component motd = miniMessage.deserialize(rawMotd);
            event.motd(motd);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid MiniMessage in 'server-list-motd': " + rawMotd);
            event.motd(Component.text("Unnamed Motd"));
        }
    }

    public void reload() {
        rawMotd = plugin.getConfig().getString("server-list-motd", "<gray>Unnamed Motd");
        pingLoggingEnabled = plugin.getConfig().getBoolean("server-list-ping-logs", false);
    }
}
