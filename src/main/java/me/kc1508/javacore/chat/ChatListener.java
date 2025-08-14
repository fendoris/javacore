package me.kc1508.javacore.chat;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class ChatListener implements Listener {
    private final FendorisPlugin plugin;
    private final ChatService chat;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public ChatListener(FendorisPlugin plugin, ChatService chat) {
        this.plugin = plugin;
        this.chat = chat;
    }

    /**
     * Reset chat/PM toggles to defaults on join (chat enabled, PMs enabled).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinResetToggles(PlayerJoinEvent e) {
        chat.resetToggles(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        final Player sender = event.getPlayer();

        // Per-player block: sender disabled own chat
        if (chat.isChatDisabled(sender.getUniqueId())) {
            event.setCancelled(true);
            String reminder = plugin.getConfig().getString("chat.togglechat.reminder");
            if (reminder != null && !reminder.isBlank()) {
                sender.sendMessage(mini.deserialize(reminder));
            }
            return;
        }

        // Global cooldown (operators bypass)
        if (!chat.checkAndTouchCooldown(sender)) {
            double remain = chat.cooldownRemainingSeconds(sender);
            String msg = plugin.getConfig().getString("chat.cooldown.message");
            if (msg != null && !msg.isBlank()) {
                String secondsPart = remain < 1.0 ? "less than 1" : String.valueOf((int) Math.ceil(remain));
                msg = msg.replace("%seconds%", secondsPart).replace("{seconds}", secondsPart).replace("%s", "s");
                sender.sendMessage(mini.deserialize(msg));
            }
            event.setCancelled(true);
            return;
        }

        // Remove viewers who disabled chat
        event.viewers().removeIf(a -> (a instanceof Player) && chat.isChatDisabled(((Player) a).getUniqueId()));

        // Build safe, filtered message
        final String rawMsg = PlainTextComponentSerializer.plainText().serialize(event.message());
        final boolean senderIsOp = sender.hasPermission("fendoris.operator");
        final String visibleText = senderIsOp ? rawMsg : ChatFilters.filter(plugin, rawMsg, sender.getName());

        final String key = senderIsOp ? "chat.format.operator" : "chat.format.global";
        final String template = plugin.getConfig().getString(key);

        // Safe render: name as component, message UNPARSED; also supports %playername% in attributes
        event.renderer((source, displayName, message, viewer) -> SafeMini.renderPlayerMessage(mini, template, Component.text(source.getName()), source.getName(), visibleText));

        // Mentions sound
        if (plugin.getConfig().getBoolean("chat.mention-sound-enabled")) {
            final String sound = plugin.getConfig().getString("chat.mention-sound-name");
            final float vol = (float) plugin.getConfig().getDouble("chat.mention-sound-volume");
            final float pitch = (float) plugin.getConfig().getDouble("chat.mention-sound-pitch");
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (var a : event.viewers()) {
                    if (a instanceof Player p && !p.equals(sender)) {
                        if (visibleText.contains(p.getName())) {
                            assert sound != null;
                            p.playSound(p.getLocation(), sound, vol, pitch);
                        }
                    }
                }
            });
        }
    }
}
