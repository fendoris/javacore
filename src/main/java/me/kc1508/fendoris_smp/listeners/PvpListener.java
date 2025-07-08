package me.kc1508.fendoris_smp.listeners;

import me.kc1508.fendoris_smp.FendorisPlugin;
import me.kc1508.fendoris_smp.commands.PvpCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PvpListener implements Listener {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage;

    // Cooldown maps for attacker and victim PvP disabled messages
    private final Map<UUID, Long> attackerMessageCooldowns = new HashMap<>();
    private final Map<UUID, Long> victimMessageCooldowns = new HashMap<>();

    // Reference to PvpCommand to update combat timestamps
    private final PvpCommand pvpCommand;

    public PvpListener(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        // Get the PvpCommand instance from plugin commands (assuming registered in FendorisPlugin)
        this.pvpCommand = (PvpCommand) Objects.requireNonNull(plugin.getServer().getPluginCommand("pvp")).getExecutor();
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!plugin.isPvpToggleEnabled()) return;

        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) return;

        UUID victimId = victim.getUniqueId();
        UUID attackerId = attacker.getUniqueId();

        boolean victimAllows = plugin.getPvpEnabledPlayers().contains(victimId);
        boolean attackerAllows = plugin.getPvpEnabledPlayers().contains(attackerId);

        if (!victimAllows || !attackerAllows) {
            event.setCancelled(true);

            int cooldown = plugin.getPvpMessageCooldownSeconds();
            long now = System.currentTimeMillis();

            if (!attackerAllows) {
                long lastMessage = attackerMessageCooldowns.getOrDefault(attackerId, 0L);
                if (now - lastMessage >= cooldown * 1000L) {
                    sendMiniMessage(attacker, "pvp-attacker-disabled-message",
                            "<red>ERROR: PvP attacker disabled message missing in config.</red>");
                    attackerMessageCooldowns.put(attackerId, now);
                }
                return; // Priority: attacker PvP disabled message only
            }

            long lastMessage = victimMessageCooldowns.getOrDefault(victimId, 0L);
            if (now - lastMessage >= cooldown * 1000L) {
                sendMiniMessage(attacker, "pvp-victim-disabled-message",
                        "<red>ERROR: PvP victim disabled message missing in config.</red>");
                victimMessageCooldowns.put(victimId, now);
            }
            return;
        }

        // If both allow PvP, update combat cooldown timestamps for both
        if (plugin.isPvpCombatCooldownEnabled()) {
            pvpCommand.updateCombatTimestamp(attackerId);
            pvpCommand.updateCombatTimestamp(victimId);
        }
    }

    private void sendMiniMessage(Player player, String key, String defaultMessage, String... replacements) {
        String rawMessage = plugin.getConfig().getString(key, defaultMessage);
        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = replacements[i];
                String replacement = replacements[i + 1];
                if (rawMessage.contains(placeholder)) {
                    rawMessage = rawMessage.replace(placeholder, replacement);
                }
            }
        }
        player.sendMessage(miniMessage.deserialize(rawMessage));
    }
}
