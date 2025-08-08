package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SessionCommand implements CommandExecutor, TabCompleter {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage;
    private final Map<UUID, String> sessionCodes = new HashMap<>();

    private static final String FALLBACK = "§cLanguage string invalid in config.";

    public SessionCommand(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sendMessageRaw(sender, "session.only-player-message");
            return true;
        }

        if (args.length > 0) {
            sendMessageToPlayer(player, "session.usage-message");
            return true;
        }

        if (!plugin.getConfig().getBoolean("session.enabled", false)) {
            sendMessageToPlayer(player, "session.disabled-message");
            return true;
        }

        String sessionCode = sessionCodes.get(player.getUniqueId());
        plugin.getLogger().info("Session code for " + player.getName() + ": " + sessionCode);
        sendMessageToPlayer(player, "session.code-message", "%session_code%", sessionCode == null ? "" : sessionCode);
        return true;
    }

    public void regenerateSessionCode(Player player) {
        sessionCodes.put(player.getUniqueId(), generateSessionCode());
    }

    public void clearSessionCode(UUID uuid) {
        sessionCodes.remove(uuid);
    }

    private String generateSessionCode() {
        int length = plugin.getConfig().getInt("session.code-length", 6);
        length = Math.max(4, Math.min(32, length)); // Clamp to 4–32

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
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
            plugin.getLogger().warning("[System: Session] Missing or empty config key: " + key);
            return;
        }

        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = replacements[i];
                String replacement = replacements[i + 1];
                rawMessage = rawMessage.replace(placeholder, replacement);
            }
        } else {
            plugin.getLogger().warning("[System: Session] Replacement array length is not even!");
        }

        sender.sendMessage(miniMessage.deserialize(rawMessage));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String @NotNull [] args) {
        return Collections.emptyList(); // disables tab completion
    }
}
