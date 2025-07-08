package me.kc1508.fendoris_smp.listeners;

import me.kc1508.fendoris_smp.FendorisPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage;

    public PlayerDeathListener(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("death-message-enabled", false)) return;

        Component original = event.deathMessage(); // This is now a Component
        if (original != null) {
            String prefix = plugin.getConfig().getString("death-message-prefix", "<red>Death</red>: ");
            Component prefixComponent = miniMessage.deserialize(prefix);

            event.deathMessage(prefixComponent.append(Component.space()).append(original));
        }
    }
}
