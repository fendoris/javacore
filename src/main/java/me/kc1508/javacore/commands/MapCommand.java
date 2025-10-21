package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MapCommand implements CommandExecutor, TabCompleter {

    private final FendorisPlugin plugin;
    private final MiniMessage mini;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    private static final String FALLBACK = "§cLanguage string invalid in config.";
    private static final int COOLDOWN_SECONDS = 5;

    public MapCommand(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.mini = plugin.getMiniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player p)) {
            send(sender, "map.only-player-message");
            return true;
        }

        // Operators bypass cooldown
        if (!p.hasPermission("fendoris.operator")) {
            long now = System.currentTimeMillis();
            long next = lastUse.getOrDefault(p.getUniqueId(), 0L) + COOLDOWN_SECONDS * 1000L;
            if (now < next) {
                long remain = (long) Math.ceil((next - now) / 1000.0);
                if (remain < 1) {
                    send(p, "map.cooldown-message-less-than-1");
                } else {
                    send(p, "map.cooldown-message", "%seconds%", Long.toString(remain), "{seconds}", Long.toString(remain), "%s", "s");
                }
                return true;
            }
            // Record usage first to avoid spamming via errors in config
            lastUse.put(p.getUniqueId(), now);
        }

        send(p, "map.message", "%player%", p.getName(), "{player}", p.getName());
        return true;
    }

    private void send(CommandSender target, String key, String... replacements) {
        String raw = plugin.getConfig().getString(key);
        if (raw == null || raw.isBlank()) {
            target.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Map] Missing config key: " + key);
            return;
        }

        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                raw = raw.replace(replacements[i], replacements[i + 1]);
            }
        } else {
            plugin.getLogger().warning("[Map] Replacement array length is not even!");
        }

        try {
            target.sendMessage(mini.deserialize(raw));
        } catch (Exception e) {
            target.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Map] Invalid MiniMessage at key '" + key + "': " + e.getMessage());
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        return Collections.emptyList();
    }
}
