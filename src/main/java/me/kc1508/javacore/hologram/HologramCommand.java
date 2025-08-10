package me.kc1508.javacore.hologram;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HologramCommand implements CommandExecutor, TabCompleter {
    private static final String FALLBACK = "§cLanguage string invalid in config.";

    private final FendorisPlugin plugin;
    private final HologramManager manager;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public HologramCommand(FendorisPlugin plugin, HologramManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private void sendMsg(CommandSender sender, String key, String... kv) {
        String raw = plugin.getConfig().getString(key);
        if (raw == null || raw.isBlank()) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Hologram] Missing config key: " + key);
            return;
        }
        // simple replacements: "%key%" -> value
        if (kv != null && kv.length % 2 == 0) {
            for (int i = 0; i < kv.length; i += 2) {
                raw = raw.replace(kv[i], kv[i + 1]);
            }
        }
        try {
            sender.sendMessage(mini.deserialize(raw));
        } catch (Exception ex) {
            sender.sendMessage(FALLBACK);
            plugin.getLogger().warning("[Hologram] Invalid MiniMessage for key: " + key + " -> " + ex.getMessage());
        }
    }

    private boolean hasOpPerm(CommandSender sender) {
        return sender.hasPermission("fendoris.operator.hologram") || sender.hasPermission("fendoris.operator");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create": {
                if (!(sender instanceof Player p)) {
                    sendMsg(sender, "hologram.messages.only-player");
                    return true;
                }
                if (!hasOpPerm(sender)) {
                    sendMsg(sender, "hologram.messages.no-permission");
                    return true;
                }
                String text = (args.length >= 2) ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "New Hologram";
                int id = manager.createAtPlayer(p, text);
                sendMsg(sender, "hologram.messages.created", "%id%", String.valueOf(id));
                return true;
            }
            case "delete": {
                if (!hasOpPerm(sender)) {
                    sendMsg(sender, "hologram.messages.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    sendMsg(sender, "hologram.messages.usage.delete");
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    boolean ok = manager.delete(id);
                    if (ok) sendMsg(sender, "hologram.messages.deleted", "%id%", String.valueOf(id));
                    else sendMsg(sender, "hologram.messages.not-found", "%id%", String.valueOf(id));
                } catch (NumberFormatException e) {
                    sendMsg(sender, "hologram.messages.id-not-number");
                }
                return true;
            }
            case "list": {
                if (!hasOpPerm(sender)) {
                    sendMsg(sender, "hologram.messages.no-permission");
                    return true;
                }
                List<String> lines = manager.listSummary();
                if (lines.isEmpty()) {
                    sendMsg(sender, "hologram.messages.list-empty");
                } else {
                    sendMsg(sender, "hologram.messages.list-header");
                    for (String line : lines) {
                        sender.sendMessage(mini.deserialize("<gray>" + line + "</gray>"));
                    }
                }
                return true;
            }
            case "bring": {
                if (!(sender instanceof Player p)) {
                    sendMsg(sender, "hologram.messages.only-player");
                    return true;
                }
                if (!hasOpPerm(sender)) {
                    sendMsg(sender, "hologram.messages.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    sendMsg(sender, "hologram.messages.usage.bring");
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    boolean ok = manager.bringToPlayer(id, p, true);
                    if (ok) sendMsg(sender, "hologram.messages.brought", "%id%", String.valueOf(id));
                    else sendMsg(sender, "hologram.messages.not-found", "%id%", String.valueOf(id));
                } catch (NumberFormatException e) {
                    sendMsg(sender, "hologram.messages.id-not-number");
                }
                return true;
            }
            case "set": {
                if (!hasOpPerm(sender)) {
                    sendMsg(sender, "hologram.messages.no-permission");
                    return true;
                }
                if (args.length < 4) {
                    helpSet(sender);
                    return true;
                }
                final int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sendMsg(sender, "hologram.messages.id-not-number");
                    return true;
                }
                String prop = args[2].toLowerCase(Locale.ROOT);
                switch (prop) {
                    case "text": {
                        String msgText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                        boolean ok = manager.setText(id, msgText);
                        if (ok) sendMsg(sender, "hologram.messages.set.text.ok", "%id%", String.valueOf(id));
                        else sendMsg(sender, "hologram.messages.not-found", "%id%", String.valueOf(id));
                        return true;
                    }
                    case "scale": {
                        try {
                            double v = Double.parseDouble(args[3]);
                            boolean ok = manager.setScale(id, v);
                            if (ok)
                                sendMsg(sender, "hologram.messages.set.scale.ok", "%value%", String.valueOf(v), "%id%", String.valueOf(id));
                            else sendMsg(sender, "hologram.messages.set.scale.fail", "%id%", String.valueOf(id));
                        } catch (NumberFormatException e) {
                            sendMsg(sender, "hologram.messages.usage.scale");
                        }
                        return true;
                    }
                    case "background": {
                        String hex = args[3];
                        boolean ok = manager.setBackground(id, hex);
                        if (ok)
                            sendMsg(sender, "hologram.messages.set.background.ok", "%value%", hex, "%id%", String.valueOf(id));
                        else sendMsg(sender, "hologram.messages.set.background.invalid");
                        return true;
                    }
                    case "text_opacity": {
                        try {
                            int v = Integer.parseInt(args[3]);
                            boolean ok = manager.setTextOpacity(id, v);
                            if (ok)
                                sendMsg(sender, "hologram.messages.set.text_opacity.ok", "%value%", String.valueOf(v), "%id%", String.valueOf(id));
                            else sendMsg(sender, "hologram.messages.set.text_opacity.fail", "%id%", String.valueOf(id));
                        } catch (NumberFormatException e) {
                            sendMsg(sender, "hologram.messages.usage.text_opacity");
                        }
                        return true;
                    }
                    case "alignment": {
                        String v = args[3];
                        boolean ok = manager.setAlignment(id, v);
                        if (ok)
                            sendMsg(sender, "hologram.messages.set.alignment.ok", "%value%", v, "%id%", String.valueOf(id));
                        else sendMsg(sender, "hologram.messages.set.alignment.invalid");
                        return true;
                    }
                    case "billboard": {
                        String v = args[3];
                        boolean ok = manager.setBillboard(id, v);
                        if (ok)
                            sendMsg(sender, "hologram.messages.set.billboard.ok", "%value%", v, "%id%", String.valueOf(id));
                        else sendMsg(sender, "hologram.messages.set.billboard.invalid");
                        return true;
                    }
                    case "shadow": {
                        String v = args[3].toLowerCase(Locale.ROOT);
                        if (!v.equals("true") && !v.equals("false")) {
                            sendMsg(sender, "hologram.messages.usage.shadow");
                            return true;
                        }
                        boolean ok = manager.setShadow(id, Boolean.parseBoolean(v));
                        if (ok)
                            sendMsg(sender, "hologram.messages.set.shadow.ok", "%value%", v, "%id%", String.valueOf(id));
                        else sendMsg(sender, "hologram.messages.set.shadow.fail", "%id%", String.valueOf(id));
                        return true;
                    }
                    case "see_through": {
                        String v = args[3].toLowerCase(Locale.ROOT);
                        if (!v.equals("true") && !v.equals("false")) {
                            sendMsg(sender, "hologram.messages.usage.see_through");
                            return true;
                        }
                        boolean ok = manager.setSeeThrough(id, Boolean.parseBoolean(v));
                        if (ok)
                            sendMsg(sender, "hologram.messages.set.see_through.ok", "%value%", v, "%id%", String.valueOf(id));
                        else sendMsg(sender, "hologram.messages.set.see_through.fail", "%id%", String.valueOf(id));
                        return true;
                    }
                    case "translation": {
                        if (args.length < 6) {
                            sendMsg(sender, "hologram.messages.usage.translation");
                            return true;
                        }
                        try {
                            double x = Double.parseDouble(args[3]);
                            double y = Double.parseDouble(args[4]);
                            double z = Double.parseDouble(args[5]);
                            boolean ok = manager.setTranslation(id, x, y, z);
                            if (ok) sendMsg(sender, "hologram.messages.set.translation.ok", "%id%", String.valueOf(id));
                            else sendMsg(sender, "hologram.messages.set.translation.fail", "%id%", String.valueOf(id));
                        } catch (NumberFormatException e) {
                            sendMsg(sender, "hologram.messages.usage.translation");
                        }
                        return true;
                    }
                    case "rotation": {
                        if (args.length < 6) {
                            sendMsg(sender, "hologram.messages.usage.rotation");
                            return true;
                        }
                        try {
                            double yaw = Double.parseDouble(args[3]);
                            double pitch = Double.parseDouble(args[4]);
                            double roll = Double.parseDouble(args[5]);
                            boolean ok = manager.setRotation(id, yaw, pitch, roll);
                            if (ok) sendMsg(sender, "hologram.messages.set.rotation.ok", "%id%", String.valueOf(id));
                            else sendMsg(sender, "hologram.messages.set.rotation.fail", "%id%", String.valueOf(id));
                        } catch (NumberFormatException e) {
                            sendMsg(sender, "hologram.messages.usage.rotation");
                        }
                        return true;
                    }
                    default: {
                        sendMsg(sender, "hologram.messages.unknown-property");
                        helpSet(sender);
                        return true;
                    }
                }
            }
            case "cleanup": {
                if (!hasOpPerm(sender)) {
                    sendMsg(sender, "hologram.messages.no-permission");
                    return true;
                }
                int removed = manager.purgeAllTagged();
                sendMsg(sender, "hologram.messages.cleanup.removed", "%count%", String.valueOf(removed));
                return true;
            }
            default: {
                help(sender);
                return true;
            }
        }
    }

    private void help(CommandSender sender) {
        sendMsg(sender, "hologram.messages.help.main");
    }

    private void helpSet(CommandSender sender) {
        sendMsg(sender, "hologram.messages.help.set.header");
        sendMsg(sender, "hologram.messages.help.set.text");
        sendMsg(sender, "hologram.messages.help.set.scale");
        sendMsg(sender, "hologram.messages.help.set.background");
        sendMsg(sender, "hologram.messages.help.set.text_opacity");
        sendMsg(sender, "hologram.messages.help.set.alignment");
        sendMsg(sender, "hologram.messages.help.set.billboard");
        sendMsg(sender, "hologram.messages.help.set.shadow");
        sendMsg(sender, "hologram.messages.help.set.see_through");
        sendMsg(sender, "hologram.messages.help.set.translation");
        sendMsg(sender, "hologram.messages.help.set.rotation");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "delete", "list", "bring", "set", "cleanup");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return Arrays.asList("text", "scale", "background", "text_opacity", "alignment", "billboard", "shadow", "see_through", "translation", "rotation");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            switch (args[2].toLowerCase(Locale.ROOT)) {
                case "alignment":
                    return Arrays.asList("left", "center", "right");
                case "billboard":
                    return Arrays.asList("center", "fixed", "vertical", "horizontal");
                case "shadow":
                case "see_through":
                    return Arrays.asList("true", "false");
            }
        }
        return Collections.emptyList();
    }
}