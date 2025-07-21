package me.kc1508.javacore.listeners;

import me.kc1508.javacore.FendorisPlugin;
import me.kc1508.javacore.commands.PvpCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PvpListener implements Listener {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage;

    private final Map<UUID, Long> attackerMessageCooldowns = new HashMap<>();
    private final Map<UUID, Long> victimMessageCooldowns = new HashMap<>();

    private final PvpCommand pvpCommand;

    private static final String FALLBACK = "§cLanguage string invalid in config.";

    public PvpListener(FendorisPlugin plugin, PvpCommand pvpCommand) {
        this.plugin = plugin;
        this.pvpCommand = pvpCommand;
        this.miniMessage = MiniMessage.miniMessage();
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
                    sendMiniMessage(attacker, "system.pvp.attacker-disabled-message");
                    attackerMessageCooldowns.put(attackerId, now);
                }
                return;
            }

            long lastMessage = victimMessageCooldowns.getOrDefault(victimId, 0L);
            if (now - lastMessage >= cooldown * 1000L) {
                sendMiniMessage(attacker, "system.pvp.victim-disabled-message");
                victimMessageCooldowns.put(victimId, now);
            }
            return;
        }

        // Update combat cooldowns
        if (plugin.isPvpCombatCooldownEnabled()) {
            pvpCommand.updateCombatTimestamp(attackerId);
            pvpCommand.updateCombatTimestamp(victimId);
        }
    }

    private void sendMiniMessage(Player player, String key, String... replacements) {
        String rawMessage = plugin.getConfig().getString(key);
        if (rawMessage == null || rawMessage.isBlank()) {
            player.sendMessage(FALLBACK);
            return;
        }

        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = replacements[i];
                String replacement = replacements[i + 1];
                rawMessage = rawMessage.replace(placeholder, replacement);
            }
        }

        player.sendMessage(miniMessage.deserialize(rawMessage));
    }
}
