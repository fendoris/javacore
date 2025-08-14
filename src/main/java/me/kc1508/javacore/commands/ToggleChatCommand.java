package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import me.kc1508.javacore.chat.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ToggleChatCommand implements CommandExecutor, TabCompleter {

    private final FendorisPlugin plugin;
    private final ChatService chat;
    private final MiniMessage mini;

    public ToggleChatCommand(FendorisPlugin plugin, ChatService chat) {
        this.plugin = plugin;
        this.chat = chat;
        this.mini = plugin.getMiniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player p)) return true;

        boolean nowDisabled = chat.toggleChat(p.getUniqueId());
        String key = nowDisabled ? "chat.togglechat.enabled-message" : "chat.togglechat.disabled-message";
        String raw = plugin.getConfig().getString(key);
        if (raw != null && !raw.isBlank()) p.sendMessage(mini.deserialize(raw));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        return Collections.emptyList();
    }
}
