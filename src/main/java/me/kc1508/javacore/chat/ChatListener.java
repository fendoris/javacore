package me.kc1508.javacore.chat;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final FendorisPlugin plugin;
    private final ChatService chat;

    public ChatListener(FendorisPlugin plugin, ChatService chat) {
        this.plugin = plugin;
        this.chat = chat;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        final Player sender = event.getPlayer();

        if (chat.isChatDisabled(sender.getUniqueId())) {
            event.setCancelled(true);
            String reminder = plugin.getConfig().getString("chat.togglechat.reminder");
            if (reminder != null && !reminder.isBlank()) {
                sender.sendMessage(plugin.getMiniMessage().deserialize(reminder));
            }
            return;
        }

        if (!chat.checkAndTouchCooldown(sender)) {
            double remain = chat.cooldownRemainingSeconds(sender);
            String msg = plugin.getConfig().getString("chat.cooldown.message");
            if (msg != null && !msg.isBlank()) {
                String secondsPart = remain < 1.0 ? "less than 1" : String.valueOf((int) Math.ceil(remain));
                msg = msg.replace("%seconds%", secondsPart)
                        .replace("{seconds}", secondsPart)
                        .replace("%s", "s");
                sender.sendMessage(plugin.getMiniMessage().deserialize(msg));
            }
            event.setCancelled(true);
            return;
        }

        event.viewers().removeIf(a -> (a instanceof Player) && chat.isChatDisabled(((Player) a).getUniqueId()));

        final String rawMsg = PlainTextComponentSerializer.plainText().serialize(event.message());
        final String filtered = ChatFilters.filter(plugin, rawMsg);

        final boolean isOp = sender.hasPermission("fendoris.operator");
        final String key = isOp ? "chat.format.operator" : "chat.format.global";
        final String template = plugin.getConfig().getString(key);

        event.renderer((source, displayName, message, viewer) ->
                SafeMini.renderPlayerMessage(
                        plugin.getMiniMessage(),
                        template != null ? template : "%player%: %message%",
                        Component.text(source.getName()),
                        filtered
                )
        );

        if (plugin.getConfig().getBoolean("chat.mention-sound-enabled")) {
            final String sound = plugin.getConfig().getString("chat.mention-sound-name");
            final float vol = (float) plugin.getConfig().getDouble("chat.mention-sound-volume");
            final float pitch = (float) plugin.getConfig().getDouble("chat.mention-sound-pitch");
            if (sound != null && !sound.isBlank()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (var a : event.viewers()) {
                        if (a instanceof Player p && !p.equals(sender)) {
                            if (filtered.contains(p.getName())) {
                                p.playSound(p.getLocation(), sound, vol, pitch);
                            }
                        }
                    }
                });
            }
        }
    }
}
