package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import me.kc1508.javacore.chat.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TogglePmCommand implements CommandExecutor, TabCompleter {
    private final FendorisPlugin plugin;
    private final ChatService chat;
    private final MiniMessage mini = MiniMessage.miniMessage();

    // per-player cooldown
    private final Map<UUID, Long> lastToggle = new HashMap<>();

    public TogglePmCommand(FendorisPlugin plugin, ChatService chat) {
        this.plugin = plugin;
        this.chat = chat;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player p)) return true;

        int cd = Math.max(0, plugin.getConfig().getInt("chat.togglepm.cooldown-seconds", 0));
        if (cd > 0) {
            long now = System.currentTimeMillis();
            long next = lastToggle.getOrDefault(p.getUniqueId(), 0L) + cd * 1000L;
            if (now < next) {
                long remain = (long) Math.ceil((next - now) / 1000.0);
                String raw = plugin.getConfig().getString("chat.togglepm.cooldown-message");
                if (raw != null && !raw.isBlank()) {
                    raw = raw.replace("%seconds%", Long.toString(remain)).replace("{seconds}", Long.toString(remain)).replace("%s", "s");
                    p.sendMessage(mini.deserialize(raw));
                }
                return true;
            }
            lastToggle.put(p.getUniqueId(), now);
        }

        boolean nowDisabled = chat.togglePm(p.getUniqueId());
        String key = nowDisabled ? "chat.togglepm.enabled-message" : "chat.togglepm.disabled-message";
        String raw = plugin.getConfig().getString(key);
        if (raw != null && !raw.isBlank()) p.sendMessage(mini.deserialize(raw));
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        return java.util.Collections.emptyList();
    }
}
