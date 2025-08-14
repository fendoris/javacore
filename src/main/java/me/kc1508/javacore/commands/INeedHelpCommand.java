package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class INeedHelpCommand implements CommandExecutor, TabCompleter {

    private final FendorisPlugin plugin;
    private final MiniMessage mini;
    private final Map<UUID, Long> lastUse = new HashMap<>();

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

        if (!plugin.getConfig().getBoolean("helpme.enabled")) {
            String raw = plugin.getConfig().getString("helpme.disabled-message");
            if (raw != null && !raw.isBlank()) sender.sendMessage(mini.deserialize(raw));
            return true;
        }

        boolean opsOnline = plugin.getServer().getOnlinePlayers().stream()
                .anyMatch(pl -> pl.hasPermission("fendoris.operator"));

        int baseCd = Math.max(0, plugin.getConfig().getInt("helpme.cooldown-seconds"));
        int noOpsCd = Math.max(0, plugin.getConfig().getInt("helpme.cooldown-no-ops-online-seconds"));
        int cd = opsOnline ? baseCd : noOpsCd;

        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastUse.getOrDefault(id, 0L);
        long next = last + cd * 1000L;

        if (now < next) {
            long remainMs = next - now;
            long remainSec = (long) Math.ceil(remainMs / 1000.0);
            String msg = plugin.getConfig().getString("helpme.cooldown-message");
            if (msg != null && !msg.isBlank()) {
                msg = msg.replace("%seconds%", Long.toString(remainSec))
                        .replace("{seconds}", Long.toString(remainSec));
                p.sendMessage(mini.deserialize(msg));
            }
            return true;
        }

        String userMsg = plugin.getConfig().getString("helpme.default-message");
        if (userMsg == null) userMsg = "";

        String broadcast = plugin.getConfig().getString("helpme.broadcast");
        if (broadcast == null || broadcast.isBlank()) {
            lastUse.put(id, now);
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

        lastUse.put(id, now);
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
