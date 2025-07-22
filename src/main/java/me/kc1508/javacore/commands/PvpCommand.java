package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PvpCommand implements CommandExecutor, TabCompleter {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage;
    private final Map<UUID, Long> toggleCooldowns = new HashMap<>();
    private final Map<UUID, Long> combatCooldowns = new HashMap<>();

    private static final String FALLBACK = "§cLanguage string invalid in config.";

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
            sendMessageRaw(sender, "system.pvp.only-player-message");
            return true;
        }

        if (args.length > 0) {
            sendMessageToPlayer(player, "system.pvp.usage-message");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (!plugin.getConfig().getBoolean("system.pvp.enabled", true)) {
            sendMessageToPlayer(player, "system.pvp.toggle-disabled-message");
            return true;
        }

        long now = System.currentTimeMillis();

        if (plugin.getConfig().getBoolean("system.pvp.combat-cooldown-enabled", true)) {
            int combatCooldown = plugin.getConfig().getInt("system.pvp.combat-cooldown-seconds", 10);
            long lastCombat = combatCooldowns.getOrDefault(uuid, 0L);
            if (now - lastCombat < combatCooldown * 1000L) {
                long millisLeft = (combatCooldown * 1000L) - (now - lastCombat);
                long secondsLeft = millisLeft / 1000L;
                if (secondsLeft < 1) {
                    sendMessageToPlayer(player, "system.pvp.combat-cooldown-message-less-than-1");
                } else {
                    sendMessageToPlayer(player, "system.pvp.combat-cooldown-message", "%seconds%", String.valueOf(secondsLeft));
                }
                return true;
            }
        }

        if (plugin.getConfig().getBoolean("system.pvp.cooldown-enabled", true)) {
            int cooldown = plugin.getConfig().getInt("system.pvp.cooldown-seconds", 30);
            long lastUsed = toggleCooldowns.getOrDefault(uuid, 0L);
            if (now - lastUsed < cooldown * 1000L) {
                long millisLeft = (cooldown * 1000L) - (now - lastUsed);
                long secondsLeft = millisLeft / 1000L;
                if (secondsLeft < 1) {
                    sendMessageToPlayer(player, "system.pvp.toggle-cooldown-message-less-than-1");
                } else {
                    sendMessageToPlayer(player, "system.pvp.toggle-cooldown-message", "%seconds%", String.valueOf(secondsLeft));
                }
                return true;
            }
            toggleCooldowns.put(uuid, now);
        }

        boolean currentlyEnabled = plugin.getPvpEnabledPlayers().contains(uuid);
        if (currentlyEnabled) {
            plugin.getPvpEnabledPlayers().remove(uuid);
            sendMessageToPlayer(player, "system.pvp.disabled-message");
            plugin.broadcastToOPsExceptSender(player.getName(), "system.pvp.operator-pvp-toggle-off", "%player%", player.getName());
        } else {
            plugin.getPvpEnabledPlayers().add(uuid);
            sendMessageToPlayer(player, "system.pvp.enabled-message");
            plugin.broadcastToOPsExceptSender(player.getName(), "system.pvp.operator-pvp-toggle-on", "%player%", player.getName());
        }
        return true;
    }

    private void sendMessageToPlayer(Player player, String key, String... extraReplacements) {
        int extraLength = extraReplacements.length;
        String[] replacements = new String[extraLength + 2];
        System.arraycopy(extraReplacements, 0, replacements, 0, extraLength);
        replacements[extraLength] = "%player%";
        replacements[extraLength + 1] = player.getName();
        sendMessageRaw(player, key, replacements);
    }

    private void sendMessageRaw(CommandSender sender, String key, String... replacements) {
        String rawMessage = plugin.getConfig().getString(key);
        if (rawMessage == null || rawMessage.isBlank()) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[System: PvP] Missing or empty config key: " + key);
            return;
        }

        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = replacements[i];
                String replacement = replacements[i + 1];
                rawMessage = rawMessage.replace(placeholder, replacement);
            }
        } else {
            plugin.getLogger().warning("[System: PvP] Replacement array length is not even!");
        }

        sender.sendMessage(miniMessage.deserialize(rawMessage));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String @NotNull [] args) {
        return Collections.emptyList(); // disables tab completion
    }
}
