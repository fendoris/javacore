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

    public void updateCombatTimestamp(UUID playerUuid) {
        combatCooldowns.put(playerUuid, System.currentTimeMillis());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length > 0) {
            sender.sendMessage("§cUsage: /pvp (Toggle PvP on or off for yourself)");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (!plugin.isPvpToggleEnabled()) {
            sendConfigMessageOrError(player, "pvp-toggle-disabled-message");
            return true;
        }

        long now = System.currentTimeMillis();

        if (plugin.isPvpCombatCooldownEnabled()) {
            int combatCooldown = plugin.getPvpCombatCooldownSeconds();
            long lastCombat = combatCooldowns.getOrDefault(uuid, 0L);
            if (now - lastCombat < combatCooldown * 1000L) {
                long millisLeft = (combatCooldown * 1000L) - (now - lastCombat);
                long secondsLeft = millisLeft / 1000L;
                if (secondsLeft < 1) {
                    sendConfigMessageOrError(player, "pvp-combat-cooldown-message-less-than-1");
                } else {
                    sendConfigMessageOrError(player, "pvp-combat-cooldown-message", "%seconds%", String.valueOf(secondsLeft));
                }
                return true;
            }
        }

        if (plugin.isPvpCooldownEnabled()) {
            int cooldown = plugin.getPvpCooldownSeconds();
            long lastUsed = toggleCooldowns.getOrDefault(uuid, 0L);
            if (now - lastUsed < cooldown * 1000L) {
                long millisLeft = (cooldown * 1000L) - (now - lastUsed);
                long secondsLeft = millisLeft / 1000L;
                if (secondsLeft < 1) {
                    sendConfigMessageOrError(player, "pvp-toggle-cooldown-message-less-than-1");
                } else {
                    sendConfigMessageOrError(player, "pvp-toggle-cooldown-message", "%seconds%", String.valueOf(secondsLeft));
                }
                return true;
            }
            toggleCooldowns.put(uuid, now);
        }

        boolean currentlyEnabled = plugin.getPvpEnabledPlayers().contains(uuid);
        if (currentlyEnabled) {
            plugin.getPvpEnabledPlayers().remove(uuid);
            sendConfigMessageOrError(player, "pvp-disabled-message");
            plugin.broadcastToOPsExceptSender(player.getName(),
                    "operator-pvp-toggle-off",
                    "<gray>[<red>Player</red>: <white>%player%</white> toggled their PvP <red>off</red>]</gray>",
                    "%player%", player.getName());
        } else {
            plugin.getPvpEnabledPlayers().add(uuid);
            sendConfigMessageOrError(player, "pvp-enabled-message");
            plugin.broadcastToOPsExceptSender(player.getName(),
                    "operator-pvp-toggle-on",
                    "<gray>[<red>Player</red>: <white>%player%</white> toggled their PvP <green>on</green>]</gray>",
                    "%player%", player.getName());
        }
        return true;
    }

    private void sendConfigMessageOrError(Player player, String key, String... replacements) {
        String rawMessage = plugin.getConfig().getString(key);
        if (rawMessage == null || rawMessage.isBlank()) {
            player.sendMessage("<red>ERROR: Missing or empty config message for '" + key + "'!</red>");
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
