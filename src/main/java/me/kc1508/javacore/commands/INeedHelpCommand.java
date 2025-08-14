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

public class INeedHelpCommand implements CommandExecutor, TabCompleter {

    private final FendorisPlugin plugin;
    private final MiniMessage mini;
    // Store the absolute unlock time per-player (epoch ms) to keep cooldown stable
    private final Map<UUID, Long> nextAllowedAt = new HashMap<>();

    public INeedHelpCommand(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.mini = plugin.getMiniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {
        if (!(sender instanceof Player p)) {
            String raw = plugin.getConfig().getString("helpme.only-player-message");
            if (raw != null && !raw.isBlank()) sender.sendMessage(mini.deserialize(raw));
            return true;
        }

        if (!plugin.getConfig().getBoolean("helpme.enabled", true)) {
            String raw = plugin.getConfig().getString("helpme.disabled-message");
            if (raw != null && !raw.isBlank()) sender.sendMessage(mini.deserialize(raw));
            return true;
        }

        // Pick cooldown length based on current operator presence,
        // but apply it only when we START a new cooldown.
        boolean opsOnline = plugin.getServer().getOnlinePlayers().stream()
                .anyMatch(pl -> pl.hasPermission("fendoris.operator"));

        int baseCd = Math.max(0, plugin.getConfig().getInt("helpme.cooldown-seconds", 60));
        int noOpsCd = Math.max(0, plugin.getConfig().getInt("helpme.cooldown-no-ops-online-seconds", baseCd));
        int cd = opsOnline ? baseCd : noOpsCd;

        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        long unlockAt = nextAllowedAt.getOrDefault(id, 0L);

        if (now < unlockAt) {
            long remainMs = unlockAt - now;
            long remainSec = (long) Math.ceil(remainMs / 1000.0);
            String msg = plugin.getConfig().getString("helpme.cooldown-message");
            if (msg != null && !msg.isBlank()) {
                msg = msg.replace("%seconds%", Long.toString(remainSec))
                        .replace("{seconds}", Long.toString(remainSec));
                p.sendMessage(mini.deserialize(msg));
            }
            return true;
        }

        // Always use the configured default; ignore any user-provided text.
        String userMsg = plugin.getConfig().getString("helpme.default-message", "");

        String broadcast = plugin.getConfig().getString("helpme.broadcast");
        if (broadcast == null || broadcast.isBlank()) {
            // Still start a new cooldown window to prevent spam when misconfigured.
            nextAllowedAt.put(id, now + cd * 1000L);
            return true;
        }

        broadcast = broadcast
                .replace("%player%", p.getName()).replace("{player}", p.getName())
                .replace("%message%", userMsg).replace("{message}", userMsg);

        boolean any = false;
        for (Player t : plugin.getServer().getOnlinePlayers()) {
            if (t.hasPermission("fendoris.operator")) {
                t.sendMessage(mini.deserialize(broadcast));
                any = true;
            }
        }

        String ackKey = any ? "helpme.ack-message" : "helpme.no-operators";
        String ack = plugin.getConfig().getString(ackKey);
        if (ack != null && !ack.isBlank()) p.sendMessage(mini.deserialize(ack));

        // Start a fresh cooldown window from NOW using the cd chosen at invocation time.
        nextAllowedAt.put(id, now + cd * 1000L);
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender,
                                               @NotNull Command command,
                                               @NotNull String alias,
                                               String @NotNull [] args) {
        return Collections.emptyList();
    }
}
