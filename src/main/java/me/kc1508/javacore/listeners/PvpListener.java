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

    private static final long MESSAGE_COOLDOWN_MS = 1500L;
    private static final String FALLBACK = "§cLanguage string invalid in config.";

    public PvpListener(FendorisPlugin plugin, PvpCommand pvpCommand) {
        this.plugin = plugin;
        this.pvpCommand = pvpCommand;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        UUID attackerUuid = attacker.getUniqueId();
        UUID victimUuid = victim.getUniqueId();

        boolean attackerHasPvP = plugin.getPvpEnabledPlayers().contains(attackerUuid);
        boolean victimHasPvP = plugin.getPvpEnabledPlayers().contains(victimUuid);

        // If attacker has PvP disabled
        if (!attackerHasPvP) {
            sendConfigMessageWithFallback(attacker, "system.pvp.attacker-disabled-message", attackerMessageCooldowns);
            event.setCancelled(true);
            return;
        }

        // If victim has PvP disabled
        if (!victimHasPvP) {
            sendConfigMessageWithFallback(attacker, "system.pvp.victim-disabled-message", victimMessageCooldowns);
            event.setCancelled(true);
            return;
        }

        // Both have PvP enabled → register combat timestamps
        pvpCommand.updateCombatTimestamp(attackerUuid);
        pvpCommand.updateCombatTimestamp(victimUuid);
    }

    private void sendConfigMessageWithFallback(Player player, String configKey, Map<UUID, Long> cooldownMap) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = cooldownMap.getOrDefault(uuid, 0L);
        if (now - last > MESSAGE_COOLDOWN_MS) {
            try {
                String msg = plugin.getConfig().getString(configKey);
                if (msg == null || msg.isBlank()) {
                    player.sendMessage(FALLBACK);
                } else {
                    player.sendMessage(miniMessage.deserialize(msg));
                }
            } catch (Exception e) {
                player.sendMessage(FALLBACK);
            }
            cooldownMap.put(uuid, now);
        }
    }
}
