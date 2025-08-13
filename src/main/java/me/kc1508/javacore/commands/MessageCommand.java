package me.kc1508.javacore.commands;

import me.kc1508.javacore.FendorisPlugin;
import me.kc1508.javacore.chat.ChatFilters;
import me.kc1508.javacore.chat.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MessageCommand implements CommandExecutor, TabCompleter {
    private final FendorisPlugin plugin;
    private final ChatService chat;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public MessageCommand(FendorisPlugin plugin, ChatService chat) {
        this.plugin = plugin;
        this.chat = chat;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        if (args.length < 2) {
            String u = plugin.getConfig().getString("chat.message.usage", "<red>Usage: /message <player> <message></red>");
            p.sendMessage(mini.deserialize(u));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            String nf = plugin.getConfig().getString("chat.pm.not-found", "<red>Player not found.</red>");
            p.sendMessage(mini.deserialize(nf));
            return true;
        }
        if (target.getUniqueId().equals(p.getUniqueId())) {
            String self = plugin.getConfig().getString("chat.reply.self", "<red>You cannot message yourself.</red>");
            p.sendMessage(mini.deserialize(self));
            return true;
        }

        boolean fromIsOp = p.hasPermission("fendoris.operator");
        if (!chat.canReceivePm(target.getUniqueId(), p.getUniqueId(), fromIsOp)) {
            String blocked = plugin.getConfig().getString("chat.pm.blocked", "<red>This player is not accepting private messages.</red>");
            p.sendMessage(mini.deserialize(blocked));
            return true;
        }

        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        msg = ChatFilters.filter(plugin, msg);

        String toKey = target.hasPermission("fendoris.operator") ? "chat.msg.self-operator" : "chat.msg.self";
        String to = plugin.getConfig().getString(toKey, "<gray>[To <white>%target%</white>]</gray> %message%");
        to = to.replace("%target%", target.getName()).replace("{target}", target.getName())
                .replace("%message%", msg).replace("{message}", msg);
        p.sendMessage(mini.deserialize(to));

        String fromKey = p.hasPermission("fendoris.operator") ? "chat.msg.other-operator" : "chat.msg.other";
        String from = plugin.getConfig().getString(fromKey, "<gray>[From <white>%player%</white>]</gray> %message%");
        from = from.replace("%player%", p.getName()).replace("{player}", p.getName())
                .replace("%message%", msg).replace("{message}", msg);
        target.sendMessage(mini.deserialize(from));

        chat.noteConversation(p.getUniqueId(), target.getUniqueId());
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String @NotNull [] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(pl.getName());
            }
            return out;
        }
        return Collections.emptyList();
    }
}
