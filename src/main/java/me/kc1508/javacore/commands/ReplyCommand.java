package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import me.kc1508.javacore.chat.ChatFilters;
import me.kc1508.javacore.chat.ChatService;
import me.kc1508.javacore.chat.SafeMini;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;

public class ReplyCommand implements CommandExecutor, TabCompleter {
    private final FendorisPlugin plugin;
    private final ChatService chat;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public ReplyCommand(FendorisPlugin plugin, ChatService chat) {
        this.plugin = plugin;
        this.chat = chat;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player p)) return true;

        UUID partnerId = chat.getLastPartner(p.getUniqueId());
        if (partnerId == null) {
            String none = plugin.getConfig().getString("chat.reply.none");
            if (none != null && !none.isBlank()) p.sendMessage(mini.deserialize(none));
            return true;
        }
        Player target = Bukkit.getPlayer(partnerId);
        if (target == null) {
            String nf = plugin.getConfig().getString("chat.pm.not-found");
            if (nf != null && !nf.isBlank()) p.sendMessage(mini.deserialize(nf));
            return true;
        }

        if (args.length < 1) {
            String u = plugin.getConfig().getString("chat.reply.usage");
            if (u != null && !u.isBlank()) p.sendMessage(mini.deserialize(u));
            return true;
        }

        if (!chat.canReceivePm(target.getUniqueId(), p.getUniqueId(), p.hasPermission("fendoris.operator"))) {
            String blocked = plugin.getConfig().getString("chat.pm.blocked");
            if (blocked != null && !blocked.isBlank()) p.sendMessage(mini.deserialize(blocked));
            return true;
        }

        String msg = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        msg = ChatFilters.filter(plugin, msg);

        String toKey = target.hasPermission("fendoris.operator") ? "chat.msg.self-operator" : "chat.msg.self";
        String toTpl = plugin.getConfig().getString(toKey);
        if (toTpl != null && !toTpl.isBlank()) {
            p.sendMessage(SafeMini.renderPm(mini, toTpl, Component.text(p.getName()), Component.text(target.getName()), msg));
        }

        String fromKey = p.hasPermission("fendoris.operator") ? "chat.msg.other-operator" : "chat.msg.other";
        String fromTpl = plugin.getConfig().getString(fromKey);
        if (fromTpl != null && !fromTpl.isBlank()) {
            target.sendMessage(SafeMini.renderPm(mini, fromTpl, Component.text(p.getName()), Component.text(target.getName()), msg));
        }

        chat.noteConversation(p.getUniqueId(), target.getUniqueId());
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        return java.util.Collections.emptyList();
    }
}
