package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import me.kc1508.javacore.chat.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class TogglePmCommand implements CommandExecutor, TabCompleter {
    private final FendorisPlugin plugin;
    private final ChatService chat;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public TogglePmCommand(FendorisPlugin plugin, ChatService chat) {
        this.plugin = plugin;
        this.chat = chat;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        boolean nowDisabled = chat.togglePm(p.getUniqueId());
        String key = nowDisabled ? "chat.togglepm.enabled-message" : "chat.togglepm.disabled-message";
        String raw = plugin.getConfig().getString(key, nowDisabled ? "<green>Private messages disabled.</green>" : "<green>Private messages enabled.</green>");
        raw = raw.replace("%player%", p.getName()).replace("{player}", p.getName());
        p.sendMessage(mini.deserialize(raw));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String @NotNull [] args) {
        return Collections.emptyList();
    }
}
