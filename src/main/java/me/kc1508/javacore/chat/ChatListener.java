package me.kc1508.javacore.chat;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final FendorisPlugin plugin;
    private final ChatService chat;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public ChatListener(FendorisPlugin plugin, ChatService chat) {
        this.plugin = plugin;
        this.chat = chat;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        final Player sender = event.getPlayer();

        // Block sending entirely if sender disabled chat (per-player)
        if (chat.isChatDisabled(sender.getUniqueId())) {
            event.setCancelled(true);
            String reminder = plugin.getConfig().getString("chat.togglechat.reminder", "<red>You have chat disabled. Use /togglechat to enable.</red>");
            sender.sendMessage(mini.deserialize(reminder));
            return;
        }

        // Cooldown
        if (!chat.checkAndTouchCooldown(sender)) {
            double remain = chat.cooldownRemainingSeconds(sender);
            String msg = plugin.getConfig().getString("chat.cooldown.message", "<red>You must wait %seconds%s before chatting again.</red>");
            String secondsPart = remain < 1.0 ? "less than 1" : String.valueOf((int) Math.ceil(remain));
            msg = msg.replace("%seconds%", secondsPart).replace("{seconds}", secondsPart).replace("%s", "s");
            sender.sendMessage(mini.deserialize(msg));
            event.setCancelled(true);
            return;
        }

        // Remove recipients who disabled chat
        event.viewers().removeIf(a -> (a instanceof Player) && chat.isChatDisabled(((Player) a).getUniqueId()));

        // Build formatted message from config, with filters
        final String rawMsg = PlainTextComponentSerializer.plainText().serialize(event.message());
        final String filtered = ChatFilters.filter(plugin, rawMsg);

        final String key = sender.hasPermission("fendoris.operator") ? "chat.format.operator" : "chat.format.global";
        String fmt = plugin.getConfig().getString(key, "<white>%player%</white>: %message%");
        fmt = fmt.replace("%player%", sender.getName()).replace("{player}", sender.getName()).replace("%message%", filtered).replace("{message}", filtered);

        final Component rendered = mini.deserialize(fmt);
        event.renderer((source, displayName, message, viewer) -> rendered);

        // Mentions -> play sound if message contains exact username
        if (plugin.getConfig().getBoolean("chat.mention-sound-enabled", true)) {
            final String text = filtered;
            final String sound = plugin.getConfig().getString("chat.mention-sound-name", "minecraft:block.note_block.pling");
            final float vol = (float) plugin.getConfig().getDouble("chat.mention-sound-volume", 1.0);
            final float pitch = (float) plugin.getConfig().getDouble("chat.mention-sound-pitch", 1.0);
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (var a : event.viewers()) {
                    if (a instanceof Player p && !p.equals(sender)) {
                        if (text.contains(p.getName())) {
                            p.playSound(p.getLocation(), sound, vol, pitch);
                        }
                    }
                }
            });
        }
    }
}
