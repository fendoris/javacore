package me.kc1508.javacore.chat;

import me.kc1508.javacore.FendorisPlugin;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatService {
    private final FendorisPlugin plugin;

    private final Set<UUID> chatDisabled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pmDisabled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastChatAt = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastPartner = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> pmBypass = new ConcurrentHashMap<>(); // recipient -> allowed senders

    public ChatService(FendorisPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean toggleChat(UUID id) {
        if (chatDisabled.remove(id)) return false;
        chatDisabled.add(id);
        return true;
    }

    public boolean togglePm(UUID id) {
        if (pmDisabled.remove(id)) return false;
        pmDisabled.add(id);
        return true;
    }

    public boolean isChatDisabled(UUID id) {
        return chatDisabled.contains(id);
    }

    public boolean isPmDisabled(UUID id) {
        return pmDisabled.contains(id);
    }

    public boolean isCooldownBypassed(Player p) {
        return p.hasPermission("fendoris.operator");
    }

    public boolean checkAndTouchCooldown(Player p) {
        if (!plugin.getConfig().getBoolean("chat.cooldown.enabled")) return true;
        if (isCooldownBypassed(p)) return true;
        double sec = plugin.getConfig().getDouble("chat.cooldown.seconds");
        long now = System.currentTimeMillis();
        long last = lastChatAt.getOrDefault(p.getUniqueId(), 0L);
        long wait = (long) Math.ceil(sec * 1000.0);
        if (now - last < wait) return false;
        lastChatAt.put(p.getUniqueId(), now);
        return true;
    }

    public double cooldownRemainingSeconds(Player p) {
        double sec = plugin.getConfig().getDouble("chat.cooldown.seconds");
        long now = System.currentTimeMillis();
        long last = lastChatAt.getOrDefault(p.getUniqueId(), 0L);
        long wait = (long) Math.ceil(sec * 1000.0);
        long remaining = Math.max(0, wait - (now - last));
        return remaining / 1000.0;
    }

    public void noteConversation(UUID a, UUID b) {
        lastPartner.put(a, b);
        lastPartner.put(b, a);
        pmBypass.computeIfAbsent(a, k -> ConcurrentHashMap.newKeySet()).add(b);
    }

    public UUID getLastPartner(UUID id) {
        return lastPartner.get(id);
    }

    public boolean canReceivePm(UUID recipient, UUID from, boolean fromIsOperator) {
        if (fromIsOperator) return true;
        if (!pmDisabled.contains(recipient)) return true;
        Set<UUID> allowed = pmBypass.get(recipient);
        return allowed != null && allowed.contains(from);
    }
}
