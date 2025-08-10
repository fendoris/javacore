package me.kc1508.javacore.hologram;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
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
            help(sender);
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
                String text = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "New Hologram";
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
                    helpSet(sender);
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
                        String msg = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                        boolean ok = manager.setText(id, msg);
                        sender.sendMessage(mini.deserialize(ok ? "<green>Updated text for #" + id + ".</green>"
                                : "<red>Hologram #" + id + " not found.</red>"));
                        return true;
                    }
                    case "scale": {
                        try {
                            double v = Double.parseDouble(args[3]);
                            boolean ok = manager.setScale(id, v);
                            sender.sendMessage(mini.deserialize(ok ? "<green>Scale set to " + v + " for #" + id + ".</green>"
                                    : "<red>Failed to set scale for #" + id + ".</red>"));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(mini.deserialize("<red>Usage: /hologram set <id> scale <number></red>"));
                        }
                        return true;
                    }
                    case "background": {
                        String hex = args[3];
                        boolean ok = manager.setBackground(id, hex);
                        sender.sendMessage(mini.deserialize(ok ? "<green>Background set to " + hex + " for #" + id + ".</green>"
                                : "<red>Invalid color. Use #RRGGBB or #AARRGGBB.</red>"));
                        return true;
                    }
                    case "text_opacity": {
                        try {
                            int v = Integer.parseInt(args[3]);
                            boolean ok = manager.setTextOpacity(id, v);
                            sender.sendMessage(mini.deserialize(ok ? "<green>Text opacity set to " + v + " for #" + id + ".</green>"
                                    : "<red>Failed to set text opacity for #" + id + ".</red>"));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(mini.deserialize("<red>Usage: /hologram set <id> text_opacity <0-255></red>"));
                        }
                        return true;
                    }
                    case "alignment": {
                        String v = args[3];
                        boolean ok = manager.setAlignment(id, v);
                        sender.sendMessage(mini.deserialize(ok ? "<green>Alignment set to " + v + " for #" + id + ".</green>"
                                : "<red>Alignment must be left, center, or right.</red>"));
                        return true;
                    }
                    case "billboard": {
                        String v = args[3];
                        boolean ok = manager.setBillboard(id, v);
                        sender.sendMessage(mini.deserialize(ok ? "<green>Billboard set to " + v + " for #" + id + ".</green>"
                                : "<red>Billboard must be center, fixed, vertical, or horizontal.</red>"));
                        return true;
                    }
                    case "shadow": {
                        String v = args[3].toLowerCase();
                        if (!v.equals("true") && !v.equals("false")) {
                            sender.sendMessage(mini.deserialize("<red>Usage: /hologram set <id> shadow <true|false></red>"));
                            return true;
                        }
                        boolean ok = manager.setShadow(id, Boolean.parseBoolean(v));
                        sender.sendMessage(mini.deserialize(ok ? "<green>Shadow set to " + v + " for #" + id + ".</green>"
                                : "<red>Failed to set shadow for #" + id + ".</red>"));
                        return true;
                    }
                    case "see_through": {
                        String v = args[3].toLowerCase();
                        if (!v.equals("true") && !v.equals("false")) {
                            sender.sendMessage(mini.deserialize("<red>Usage: /hologram set <id> see_through <true|false></red>"));
                            return true;
                        }
                        boolean ok = manager.setSeeThrough(id, Boolean.parseBoolean(v));
                        sender.sendMessage(mini.deserialize(ok ? "<green>See-through set to " + v + " for #" + id + ".</green>"
                                : "<red>Failed to set see-through for #" + id + ".</red>"));
                        return true;
                    }
                    case "translation": {
                        if (args.length < 6) {
                            sender.sendMessage(mini.deserialize("<red>Usage: /hologram set <id> translation <x> <y> <z></red>"));
                            return true;
                        }
                        try {
                            double x = Double.parseDouble(args[3]);
                            double y = Double.parseDouble(args[4]);
                            double z = Double.parseDouble(args[5]);
                            boolean ok = manager.setTranslation(id, x, y, z);
                            sender.sendMessage(mini.deserialize(ok ? "<green>Translation set for #" + id + ".</green>"
                                    : "<red>Failed to set translation for #" + id + ".</red>"));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(mini.deserialize("<red>Usage: /hologram set <id> translation <x> <y> <z></red>"));
                        }
                        return true;
                    }
                    case "rotation": {
                        if (args.length < 6) {
                            sender.sendMessage(mini.deserialize("<red>Usage: /hologram set <id> rotation <yaw> <pitch> <roll></red>"));
                            return true;
                        }
                        try {
                            double yaw = Double.parseDouble(args[3]);
                            double pitch = Double.parseDouble(args[4]);
                            double roll = Double.parseDouble(args[5]);
                            boolean ok = manager.setRotation(id, yaw, pitch, roll);
                            sender.sendMessage(mini.deserialize(ok ? "<green>Rotation set for #" + id + ".</green>"
                                    : "<red>Failed to set rotation for #" + id + ".</red>"));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(mini.deserialize("<red>Usage: /hologram set <id> rotation <yaw> <pitch> <roll></red>"));
                        }
                        return true;
                    }
                    default: {
                        sender.sendMessage(mini.deserialize("<red>Unknown property.</red>"));
                        helpSet(sender);
                        return true;
                    }
                }
            }
            case "cleanup": {
                if (!checkOpPerm(sender)) return true;
                int removed = manager.purgeAllTagged();
                sender.sendMessage(mini.deserialize("<green>Removed <yellow>" + removed + "</yellow> hologram display(s).</green>"));
                return true;
            }
            default: {
                help(sender);
                return true;
            }
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage(mini.deserialize("<gray>/hologram create | delete <id> | list | bring <id> | set <id> ... | cleanup</gray>"));
    }
    private void helpSet(CommandSender sender) {
        sender.sendMessage(mini.deserialize("<gray>Usage:</gray>"));
        sender.sendMessage(mini.deserialize("<gray>/hologram set <id> text <message...></gray>"));
        sender.sendMessage(mini.deserialize("<gray>/hologram set <id> scale <number></gray>"));
        sender.sendMessage(mini.deserialize("<gray>/hologram set <id> background <#AARRGGBB|#RRGGBB></gray>"));
        sender.sendMessage(mini.deserialize("<gray>/hologram set <id> text_opacity <0-255></gray>"));
        sender.sendMessage(mini.deserialize("<gray>/hologram set <id> alignment <left|center|right></gray>"));
        sender.sendMessage(mini.deserialize("<gray>/hologram set <id> billboard <center|fixed|vertical|horizontal></gray>"));
        sender.sendMessage(mini.deserialize("<gray>/hologram set <id> shadow <true|false></gray>"));
        sender.sendMessage(mini.deserialize("<gray>/hologram set <id> see_through <true|false></gray>"));
        sender.sendMessage(mini.deserialize("<gray>/hologram set <id> translation <x> <y> <z></gray>"));
        sender.sendMessage(mini.deserialize("<gray>/hologram set <id> rotation <yaw> <pitch> <roll></gray>"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>();
            base.add("create"); base.add("delete"); base.add("list"); base.add("bring"); base.add("set"); base.add("cleanup");
            return base;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            List<String> props = new ArrayList<>();
            Collections.addAll(props, "text","scale","background","text_opacity","alignment","billboard","shadow","see_through","translation","rotation");
            return props;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            switch (args[2].toLowerCase()) {
                case "alignment": return List.of("left","center","right");
                case "billboard": return List.of("center","fixed","vertical","horizontal");
                case "shadow":
                case "see_through": return List.of("true","false");
            }
        }
        return Collections.emptyList();
    }
}