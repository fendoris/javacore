package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import me.kc1508.javacore.storage.StorageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SetHomeCommand implements CommandExecutor {

    private final FendorisPlugin plugin;
    private final StorageManager storage;
    private final MiniMessage miniMessage;
    private static final String FALLBACK = "§cLanguage string invalid in config.";

    private final Map<UUID, Long> lastSetHomeTimes = new HashMap<>();

    public SetHomeCommand(FendorisPlugin plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sendChat(sender, "sethome.only-player-message");
            return true;
        }

        if (args.length > 0) {
            sendChat(player, "sethome.usage-message");
            return true;
        }

        int cooldownSec = Math.max(0, plugin.getConfig().getInt("sethome.cooldown-seconds", 60));
        if (cooldownSec > 0 && !player.hasPermission("fendoris.operator")) {
            UUID pu = player.getUniqueId();
            long now = System.currentTimeMillis();
            long last = lastSetHomeTimes.getOrDefault(pu, 0L);
            long elapsed = now - last;
            long cooldownMillis = TimeUnit.SECONDS.toMillis(cooldownSec);

            if (elapsed < cooldownMillis) {
                long remainingSec = (cooldownMillis - elapsed + 999) / 1000;
                if (remainingSec < 1) {
                    sendChat(player, "sethome.cooldown-message-less-than-1");
                } else {
                    sendChat(player, "sethome.cooldown-message", "%seconds%", String.valueOf(remainingSec));
                }
                return true;
            }
        }

        storage.setHome(player.getUniqueId(), player.getLocation());
        lastSetHomeTimes.put(player.getUniqueId(), System.currentTimeMillis());
        sendChat(player, "sethome.set-success");
        return true;
    }

    private void sendChat(CommandSender sender, String key, String... replacements) {
        String rawMessage = plugin.getConfig().getString(key);
        if (rawMessage == null || rawMessage.isBlank()) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[SetHome] Missing config key: " + key);
            return;
        }

        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                rawMessage = rawMessage.replace(replacements[i], replacements[i + 1]);
            }
        }

        try {
            sender.sendMessage(miniMessage.deserialize(rawMessage));
        } catch (Exception e) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[SetHome] Invalid message format for key: " + key + " -> " + e.getMessage());
        }
    }
}

