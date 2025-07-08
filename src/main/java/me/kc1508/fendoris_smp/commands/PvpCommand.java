package me.kc1508.fendoris_smp.commands;

import me.kc1508.fendoris_smp.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PvpCommand implements CommandExecutor {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage;
    private final Map<UUID, Long> toggleCooldowns = new HashMap<>();
    private final Map<UUID, Long> combatCooldowns = new HashMap<>();

    public PvpCommand(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    // Method for PvpListener to update combat cooldown timestamp for a player
    public void updateCombatTimestamp(UUID playerUuid) {
        combatCooldowns.put(playerUuid, System.currentTimeMillis());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (!plugin.isPvpToggleEnabled()) {
            sendMiniMessage(player, "pvp-toggle-disabled-message", "<red>ERROR: PvP toggle system message not configured properly.</red>");
            return true;
        }

        long now = System.currentTimeMillis();

        // PRIORITY: Combat cooldown takes precedence over general cooldown
        if (plugin.isPvpCombatCooldownEnabled()) {
            int combatCooldown = plugin.getPvpCombatCooldownSeconds();
            long lastCombat = combatCooldowns.getOrDefault(uuid, 0L);
            if (now - lastCombat < combatCooldown * 1000L) {
                long millisLeft = (combatCooldown * 1000L) - (now - lastCombat);
                long secondsLeft = millisLeft / 1000L;
                if (secondsLeft < 1) {
                    sendMiniMessage(player, "pvp-combat-cooldown-message-less-than-1", "<red>ERROR: PvP combat cooldown <1 second message missing in config.</red>");
                } else {
                    sendMiniMessage(player, "pvp-combat-cooldown-message", "<red>ERROR: PvP combat cooldown message missing or invalid in config.</red>", "%seconds%", String.valueOf(secondsLeft));
                }
                return true;
            }
        }

        if (plugin.isPvpCooldownEnabled()) {
            int cooldown = plugin.getPvpCooldownSeconds();
            long lastUsed = toggleCooldowns.getOrDefault(uuid, 0L);
            if (now - lastUsed < cooldown * 1000L) {
                long millisLeft = ((cooldown * 1000L) - (now - lastUsed));
                long secondsLeft = millisLeft / 1000L;

                if (secondsLeft < 1) {
                    sendMiniMessage(player, "pvp-toggle-cooldown-message-less-than-1", "<red>ERROR: PvP toggle cooldown <1 second message missing in config.</red>");
                } else {
                    sendMiniMessage(player, "pvp-toggle-cooldown-message", "<red>ERROR: PvP toggle cooldown message missing or invalid in config.</red>", "%seconds%", String.valueOf(secondsLeft));
                }
                return true;
            }
            toggleCooldowns.put(uuid, now);
        }

        boolean currentlyEnabled = plugin.getPvpEnabledPlayers().contains(uuid);
        if (currentlyEnabled) {
            plugin.getPvpEnabledPlayers().remove(uuid);
            sendMiniMessage(player, "pvp-disabled-message", "<red>ERROR: PvP disabled message missing in config.</red>");
        } else {
            plugin.getPvpEnabledPlayers().add(uuid);
            sendMiniMessage(player, "pvp-enabled-message", "<red>ERROR: PvP enabled message missing in config.</red>");
        }

        return true;
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
