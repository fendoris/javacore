package me.kc1508.javacore.hologram;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HologramCommand implements CommandExecutor, TabCompleter {
    private final FendorisPlugin plugin;
    private final HologramManager manager;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public HologramCommand(FendorisPlugin plugin, HologramManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private boolean checkOpPerm(CommandSender sender) {
        if (sender.hasPermission("fendoris.operator.hologram") || sender.hasPermission("fendoris.operator")) {
            return true;
        }
        sender.sendMessage(mini.deserialize(plugin.getConfig().getString(
                "hologram.messages.no-permission",
                "<red>You don't have permission to use /hologram.</red>"
        )));
        return false;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage(mini.deserialize("<gray>/hologram create | delete <id> | list | bring <id> | set <id> text <message></gray>"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create": {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Must be a player.");
                    return true;
                }
                if (!checkOpPerm(sender)) return true;
                String text = args.length >= 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "New Hologram";
                int id = manager.createAtPlayer(p, text);
                sender.sendMessage(mini.deserialize(plugin.getConfig().getString(
                        "hologram.messages.created",
                        "<green>Created hologram <white>#%id%</white> at your location.</green>"
                ).replace("%id%", String.valueOf(id))));
                return true;
            }
            case "delete": {
                if (!checkOpPerm(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(mini.deserialize("<red>Usage: /hologram delete <id></red>"));
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    boolean ok = manager.delete(id);
                    sender.sendMessage(mini.deserialize(ok ? "<green>Deleted hologram #" + id + ".</green>"
                            : "<red>Hologram #" + id + " not found.</red>"));
                } catch (NumberFormatException e) {
                    sender.sendMessage(mini.deserialize("<red>ID must be a number.</red>"));
                }
                return true;
            }
            case "list": {
                if (!checkOpPerm(sender)) return true;
                List<String> lines = manager.listSummary();
                if (lines.isEmpty()) {
                    sender.sendMessage(mini.deserialize("<gray>No holograms configured.</gray>"));
                } else {
                    sender.sendMessage(mini.deserialize("<gold>Holograms:</gold>"));
                    for (String line : lines) sender.sendMessage(mini.deserialize("<gray>" + line + "</gray>"));
                }
                return true;
            }
            case "bring": {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Must be a player.");
                    return true;
                }
                if (!checkOpPerm(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(mini.deserialize("<red>Usage: /hologram bring <id></red>"));
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    boolean ok = manager.bringToPlayer(id, p, true);
                    sender.sendMessage(mini.deserialize(ok ? "<green>Brought hologram #" + id + " to your location.</green>"
                            : "<red>Hologram #" + id + " not found.</red>"));
                } catch (NumberFormatException e) {
                    sender.sendMessage(mini.deserialize("<red>ID must be a number.</red>"));
                }
                return true;
            }
            case "set": {
                if (!checkOpPerm(sender)) return true;
                if (args.length < 4) {
                    sender.sendMessage(mini.deserialize("<red>Usage: /hologram set <id> text <message></red>"));
                    return true;
                }
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(mini.deserialize("<red>ID must be a number.</red>"));
                    return true;
                }
                String prop = args[2].toLowerCase();
                switch (prop) {
                    case "text": {
                        String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                        boolean ok = manager.setText(id, msg);
                        sender.sendMessage(mini.deserialize(ok ? "<green>Updated text for #" + id + ".</green>"
                                : "<red>Hologram #" + id + " not found.</red>"));
                        return true;
                    }
                    default: {
                        sender.sendMessage(mini.deserialize("<red>Unknown property. For now, only: text</red>"));
                        return true;
                    }
                }
            }
            default: {
                sender.sendMessage(mini.deserialize("<gray>/hologram create | delete <id> | list | bring <id> | set <id> text <message></gray>"));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>();
            base.add("create"); base.add("delete"); base.add("list"); base.add("bring"); base.add("set");
            return base;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return Collections.singletonList("text");
        }
        return Collections.emptyList();
    }
}