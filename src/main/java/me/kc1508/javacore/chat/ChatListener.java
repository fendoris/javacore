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

public final class ChatListener implements Listener {
    private final FendorisPlugin plugin;
    private final ChatService chat;

    public ChatListener(FendorisPlugin plugin, ChatService chat) {
        this.plugin = plugin;
        this.chat = chat;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        final Player sender = event.getPlayer();
        final MiniMessage mini = plugin.getMiniMessage();

        // Per-player chat disable
        if (chat.isChatDisabled(sender.getUniqueId())) {
            event.setCancelled(true);
            String reminder = plugin.getConfig().getString(
                    "chat.togglechat.reminder",
                    "<red>You have chat disabled. Use /togglechat to enable.</red>"
            );
            sender.sendMessage(mini.deserialize(reminder));
            return;
        }

        // Cooldown (bypass for operators is handled inside ChatService)
        if (!chat.checkAndTouchCooldown(sender)) {
            double remain = chat.cooldownRemainingSeconds(sender);
            String msg = plugin.getConfig().getString(
                    "chat.cooldown.message",
                    "<red>You must wait %seconds%s before chatting again.</red>"
            );
            String secondsPart = remain < 1.0 ? "less than 1" : String.valueOf((int) Math.ceil(remain));
            msg = msg.replace("%seconds%", secondsPart)
                    .replace("{seconds}", secondsPart)
                    .replace("%s", "s");
            sender.sendMessage(mini.deserialize(msg));
            event.setCancelled(true);
            return;
        }

        // Remove recipients who disabled chat
        event.viewers().removeIf(a -> (a instanceof Player)
                && chat.isChatDisabled(((Player) a).getUniqueId()));

        // Extract plain user text, run filters, then render safely (no tag injection)
        final String rawMsg = PlainTextComponentSerializer.plainText().serialize(event.message());
        final String filtered = ChatFilters.filter(plugin, rawMsg);

        final String key = sender.hasPermission("fendoris.operator")
                ? "chat.format.operator" : "chat.format.global";
        final String template = plugin.getConfig().getString(key, "<white>%player%</white>: %message%");

        event.renderer((source, displayName, message, viewer) ->
                SafeMini.renderPlayerMessage(
                        mini,
                        template,
                        Component.text(source.getName()),
                        filtered
                )
        );

        // Mentions: ping viewers whose exact name appears in filtered text
        if (plugin.getConfig().getBoolean("chat.mention-sound-enabled", true)) {
            final String text = filtered;
            final String sound = plugin.getConfig().getString(
                    "chat.mention-sound-name",
                    "minecraft:block.note_block.pling"
            );
            final float vol = (float) plugin.getConfig().getDouble("chat.mention-sound-volume", 1.0);
            final float pitch = (float) plugin.getConfig().getDouble("chat.mention-sound-pitch", 1.0);

            // Play sounds on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (var audience : event.viewers()) {
                    if (audience instanceof Player p && !p.equals(sender)) {
                        if (text.contains(p.getName())) {
                            p.playSound(p.getLocation(), sound, vol, pitch);
                        }
                    }
                }
            });
        }
    }
}
