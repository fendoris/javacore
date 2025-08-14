package me.kc1508.javacore.config;

import me.kc1508.javacore.FendorisPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.List;

public class ConfigValidator {

    private final FendorisPlugin plugin;
    private final FileConfiguration config;

    private static final String PREFIX = "[Fendoris] [ConfigValidator] ";

    public ConfigValidator(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void validate() {
        boolean changed = false;

        // --- Join / Quit ---
        changed |= checkBooleanDefault("private-join-message-enabled");
        changed |= checkBooleanDefault("public-join-message-enabled");
        changed |= checkBooleanDefault("public-quit-message-enabled");
        changed |= checkStringDefault("private-join-message", "<gray>%player% connected</gray><reset>");
        changed |= checkStringDefault("public-join-message", "<gray>%player% connected</gray><reset>");
        changed |= checkStringDefault("public-quit-message", "<gray>%player% disconnected</gray><reset>");

        // --- PvP ---
        changed |= checkBooleanDefault("system.pvp.enabled");
        changed |= checkBooleanDefault("system.pvp.cooldown-enabled");
        changed |= checkIntDefault("system.pvp.cooldown-seconds", 30);
        changed |= checkIntDefault("system.pvp.message-cooldown-seconds", 15);
        changed |= checkBooleanDefault("system.pvp.combat-cooldown-enabled");
        changed |= checkIntDefault("system.pvp.combat-cooldown-seconds", 10);

        changed |= checkStringDefault("system.pvp.only-player-message", "<red>Only players can use this command.<reset>");
        changed |= checkStringDefault("system.pvp.usage-message", "<red>Usage: /pvp (Toggle PvP on or off for yourself)<reset>");
        changed |= checkStringDefault("system.pvp.toggle-disabled-message", "<red>The PvP toggle system is currently disabled on this server.<reset>");
        changed |= checkStringDefault("system.pvp.combat-cooldown-message-less-than-1", "<red>You must wait less than 1s after combat to toggle PvP.<reset>");
        changed |= checkStringDefault("system.pvp.combat-cooldown-message", "<red>You must wait %seconds%s after combat to toggle PvP.<reset>");
        changed |= checkStringDefault("system.pvp.toggle-cooldown-message-less-than-1", "<red>You must wait less than 1s to toggle PvP again.<reset>");
        changed |= checkStringDefault("system.pvp.toggle-cooldown-message", "<red>You must wait %seconds%s to toggle PvP again.<reset>");
        changed |= checkStringDefault("system.pvp.disabled-message", "<red>You have disabled PvP.<reset>");
        changed |= checkStringDefault("system.pvp.enabled-message", "<red>You have enabled PvP.<reset>");
        changed |= checkStringDefault("system.pvp.operator-pvp-toggle-off", "<gray>[<red>Player</red>: <white>%player%</white> toggled their PvP <red>off</red>]</gray>");
        changed |= checkStringDefault("system.pvp.operator-pvp-toggle-on", "<gray>[<red>Player</red>: <white>%player%</white> toggled their PvP <green>on</green>]</gray>");
        changed |= checkStringDefault("system.pvp.attacker-disabled-message", "<red>You cannot PvP while your PvP is disabled.<reset>");
        changed |= checkStringDefault("system.pvp.victim-disabled-message", "<red>This player has PvP disabled.<reset>");

        // --- Session Codes ---
        changed |= checkBooleanDefault("session.enabled");
        changed |= checkIntDefault("session.code-length", 6);
        changed |= checkStringDefault("session.only-player-message", "<red>Only players can use this command.<reset>");
        changed |= checkStringDefault("session.usage-message", "<red>Usage: /session (Shows your current Session Code)<reset>");
        changed |= checkStringDefault("session.disabled-message", "<red>The Session Code system is currently disabled.<reset>");
        changed |= checkStringDefault("session.code-message", "<reset><newline><reset><red>Your session code is <click:suggest_command:'%session_code%'><red><bold>%session_code%</bold><red></click>, reconnecting will generate another one.<reset><reset><newline><reset>");

        // --- Death Messages ---
        changed |= checkBooleanDefault("death-message-enabled");
        changed |= checkStringDefault("death-message-prefix", "<i>Death:");

        // --- Reload ---
        changed |= checkStringDefault("reload.no-permission", "<red>You don't have permission to reload the plugin.</red>");
        changed |= checkStringDefault("reload.reload-success", "<gold>Config reloaded.</gold>");
        changed |= checkStringDefault("reload.reload-broadcast", "<gray>[<gold>Operator</gold>: <white>%player%</white> reloaded the plugin]</gray>");

        // --- Spawn ---
        changed |= checkBooleanDefault("spawn.enabled");
        changed |= checkStringDefault("spawn.location-world", "world");
        changed |= checkDoubleDefault("spawn.location-x", 0.5);
        changed |= checkDoubleDefault("spawn.location-y", 100.0);
        changed |= checkDoubleDefault("spawn.location-z", 0.5);
        changed |= checkDoubleDefault("spawn.location-yaw", 0.0);
        changed |= checkDoubleDefault("spawn.location-pitch", 0.0);
        changed |= checkIntDefault("spawn.cooldown", 3);
        changed |= checkStringDefault("spawn.cooldown-message", "<red>You must wait %seconds%s before using this again.<reset>");
        changed |= checkStringDefault("spawn.cooldown-message-less-than-1", "<red>You must wait less than 1s to use /spawn again.<reset>");
        changed |= checkIntDefault("spawn.teleport-delay-seconds", 5);
        changed |= checkStringDefault("spawn.teleport-delay-start-message", "<yellow>Teleporting to spawn in %seconds%s... Don't move!<reset>");
        changed |= checkStringDefault("spawn.teleport-cancelled-message", "<red>Teleport cancelled because you moved.<reset>");
        changed |= checkBooleanDefault("spawn.teleport-particles-enabled");
        changed |= checkStringDefault("spawn.teleport-particle-name", "minecraft:portal");
        changed |= checkIntDefault("spawn.teleport-particle-count", 20);
        changed |= checkBooleanDefault("spawn.teleport-sound-enabled");
        changed |= checkStringDefault("spawn.teleport-sound-name", "minecraft:block.note_block.pling");
        changed |= checkDoubleDefault("spawn.teleport-sound-volume", 1.0);
        changed |= checkDoubleDefault("spawn.teleport-sound-pitch", 1.0);
        changed |= checkStringDefault("spawn.only-player-message", "<red>Only players can use this command.<reset>");
        changed |= checkStringDefault("spawn.usage-message", "<red>Usage: /spawn (Teleports you to the server spawn location)<reset>");
        changed |= checkStringDefault("spawn.disabled-message", "<red>The Spawn system is currently disabled on this server.<reset>");
        changed |= checkStringDefault("spawn.teleport-success-message", "<green>You have been teleported to the server spawn.<reset>");
        changed |= checkStringDefault("spawn.no-permission-message", "<red>You don't have permission to use this command.<reset>");
        changed |= checkStringDefault("spawn.sendtospawn-usage", "<red>Usage: /sendtospawn <player><reset>");
        changed |= checkStringDefault("spawn.player-not-online", "<red>That player is not online.<reset>");
        changed |= checkStringDefault("spawn.sent-to-spawn-message", "<green>You have been sent to spawn by an operator.<reset>");
        changed |= checkStringDefault("spawn.operator-sent-message", "<green>Sent %player% to spawn.<reset>");
        changed |= checkStringDefault("spawn.set-success", "<green>Spawn location saved to config.<reset>");
        changed |= checkStringDefault("spawn.teleport-already-in-progress-message", "<red>You are already teleporting to spawn!<reset>");

        // --- Holograms (messages only) ---
        changed |= checkStringDefault("hologram.messages.no-permission", "<red>You don't have permission to use this command.<reset>");
        changed |= checkStringDefault("hologram.messages.only-player", "<red>Only players can use this command.<reset>");
        changed |= checkStringDefault("hologram.messages.help.main", "<gray>/hologram create | delete <id> | list | bring <id> | set <id> ... | cleanup</gray>");
        changed |= checkStringDefault("hologram.messages.help.set.header", "<gray>Usage:</gray>");
        changed |= checkStringDefault("hologram.messages.help.set.text", "<gray>/hologram set <id> text <message...></gray>");
        changed |= checkStringDefault("hologram.messages.help.set.scale", "<gray>/hologram set <id> scale <number></gray>");
        changed |= checkStringDefault("hologram.messages.help.set.background", "<gray>/hologram set <id> background <#AARRGGBB|#RRGGBB></gray>");
        changed |= checkStringDefault("hologram.messages.help.set.text_opacity", "<gray>/hologram set <id> text_opacity <0-255></gray>");
        changed |= checkStringDefault("hologram.messages.help.set.alignment", "<gray>/hologram set <id> alignment <left|center|right></gray>");
        changed |= checkStringDefault("hologram.messages.help.set.billboard", "<gray>/hologram set <id> billboard <center|fixed|vertical|horizontal></gray>");
        changed |= checkStringDefault("hologram.messages.help.set.shadow", "<gray>/hologram set <id> shadow <true|false></gray>");
        changed |= checkStringDefault("hologram.messages.help.set.see_through", "<gray>/hologram set <id> see_through <true|false></gray>");
        changed |= checkStringDefault("hologram.messages.help.set.translation", "<gray>/hologram set <id> translation <x> <y> <z></gray>");
        changed |= checkStringDefault("hologram.messages.help.set.rotation", "<gray>/hologram set <id> rotation <yaw> <pitch> <roll></gray>");
        changed |= checkStringDefault("hologram.messages.usage.delete", "<red>Usage: /hologram delete <id></red>");
        changed |= checkStringDefault("hologram.messages.usage.bring", "<red>Usage: /hologram bring <id></red>");
        changed |= checkStringDefault("hologram.messages.usage.scale", "<red>Usage: /hologram set <id> scale <number></red>");
        changed |= checkStringDefault("hologram.messages.usage.text_opacity", "<red>Usage: /hologram set <id> text_opacity <0-255></red>");
        changed |= checkStringDefault("hologram.messages.usage.translation", "<red>Usage: /hologram set <id> translation <x> <y> <z></red>");
        changed |= checkStringDefault("hologram.messages.usage.rotation", "<red>Usage: /hologram set <id> rotation <yaw> <pitch> <roll></red>");
        changed |= checkStringDefault("hologram.messages.usage.shadow", "<red>Usage: /hologram set <id> shadow <true|false></red>");
        changed |= checkStringDefault("hologram.messages.usage.see_through", "<red>Usage: /hologram set <id> see_through <true|false></red>");
        changed |= checkStringDefault("hologram.messages.created", "<green>Created hologram <white>#%id%</white> at your location.</green>");
        changed |= checkStringDefault("hologram.messages.list-header", "<gold>Holograms:</gold>");
        changed |= checkStringDefault("hologram.messages.list-empty", "<gray>No holograms configured.</gray>");
        changed |= checkStringDefault("hologram.messages.id-not-number", "<red>ID must be a number.</red>");
        changed |= checkStringDefault("hologram.messages.not-found", "<red>Hologram <white>#%id%</white> not found.</red>");
        changed |= checkStringDefault("hologram.messages.unknown-property", "<red>Unknown property.</red>");
        changed |= checkStringDefault("hologram.messages.brought", "<green>Brought hologram <white>#%id%</white> to your location.</green>");
        changed |= checkStringDefault("hologram.messages.deleted", "<green>Deleted hologram <white>#%id%</white>.</green>");
        changed |= checkStringDefault("hologram.messages.set.text.ok", "<green>Updated text for <white>#%id%</white>.</green>");
        changed |= checkStringDefault("hologram.messages.set.scale.ok", "<green>Scale set to <white>%value%</white> for <white>#%id%</white>.</green>");
        changed |= checkStringDefault("hologram.messages.set.scale.fail", "<red>Failed to set scale for <white>#%id%</white>.</red>");
        changed |= checkStringDefault("hologram.messages.set.background.ok", "<green>Background set to <white>%value%</white> for <white>#%id%</white>.</green>");
        changed |= checkStringDefault("hologram.messages.set.background.invalid", "<red>Invalid color. Use #RRGGBB or #AARRGGBB.</red>");
        changed |= checkStringDefault("hologram.messages.set.text_opacity.ok", "<green>Text opacity set to <white>%value%</white> for <white>#%id%</white>.</green>");
        changed |= checkStringDefault("hologram.messages.set.text_opacity.fail", "<red>Failed to set text opacity for <white>#%id%</white>.</red>");
        changed |= checkStringDefault("hologram.messages.set.alignment.ok", "<green>Alignment set to <white>%value%</white> for <white>#%id%</white>.</green>");
        changed |= checkStringDefault("hologram.messages.set.alignment.invalid", "<red>Alignment must be left, center, or right.</red>");
        changed |= checkStringDefault("hologram.messages.set.billboard.ok", "<green>Billboard set to <white>%value%</white> for <white>#%id%</white>.</green>");
        changed |= checkStringDefault("hologram.messages.set.billboard.invalid", "<red>Billboard must be center, fixed, vertical, or horizontal.</red>");
        changed |= checkStringDefault("hologram.messages.set.shadow.ok", "<green>Shadow set to <white>%value%</white> for <white>#%id%</white>.</green>");
        changed |= checkStringDefault("hologram.messages.set.shadow.fail", "<red>Failed to set shadow for <white>#%id%</white>.</red>");
        changed |= checkStringDefault("hologram.messages.set.see_through.ok", "<green>See-through set to <white>%value%</white> for <white>#%id%</white>.</green>");
        changed |= checkStringDefault("hologram.messages.set.see_through.fail", "<red>Failed to set see-through for <white>#%id%</white>.</red>");
        changed |= checkStringDefault("hologram.messages.set.translation.ok", "<green>Translation set for <white>#%id%</white>.</green>");
        changed |= checkStringDefault("hologram.messages.set.translation.fail", "<red>Failed to set translation for <white>#%id%</white>.</red>");
        changed |= checkStringDefault("hologram.messages.set.rotation.ok", "<green>Rotation set for <white>#%id%</white>.</green>");
        changed |= checkStringDefault("hologram.messages.set.rotation.fail", "<red>Failed to set rotation for <white>#%id%</white>.</red>");
        changed |= checkStringDefault("hologram.messages.cleanup.removed", "<green>Removed <yellow>%count%</yellow> hologram display(s).</green>");

        // --- Chat (all strings & settings) ---
        // Migrate old on/off/true/false to enabled/disabled-message
        changed |= migrateToggleKeys("chat.togglechat");
        changed |= migrateToggleKeys("chat.togglepm");

        // SafeMini-aware defaults: %playername% for click action, %player% for styled component
        changed |= checkStringDefault("chat.format.global", "<hover:show_text:'<gray>(Click) Message %player%</gray>'><white><click:suggest_command:'/msg %playername% '>%player%</click></white></hover>: %message%");
        changed |= checkStringDefault("chat.format.operator", "<hover:show_text:'<gray>(Click) Message %player%</gray>'><gold><click:suggest_command:'/msg %playername% '>%player%</click></gold></hover>: %message%");

        changed |= checkStringDefault("chat.msg.self", "<gray>[To <white>%target%</white>]</gray> %message%");
        changed |= checkStringDefault("chat.msg.self-operator", "<gray>[To <gold>%target%</gold>]</gray> %message%");
        changed |= checkStringDefault("chat.msg.other", "<gray>[From <white>%player%</white>]</gray> %message%");
        changed |= checkStringDefault("chat.msg.other-operator", "<gray>[From <gold>%player%</gold>]</gray> %message%");

        changed |= checkStringDefault("chat.message.usage", "<red>Usage: /message <player> <message></red>");
        changed |= checkStringDefault("chat.reply.none", "<red>No recent private conversation.</red>");
        changed |= checkStringDefault("chat.reply.self", "<red>You cannot message yourself.</red>");
        changed |= checkStringDefault("chat.reply.usage", "<red>Usage: /reply <message></red>");
        changed |= checkStringDefault("chat.pm.not-found", "<red>Player not found.</red>");
        changed |= checkStringDefault("chat.pm.blocked", "<red>This player is not accepting private messages.</red>");

        changed |= checkStringDefault("chat.togglechat.enabled-message", "<green>Public chat disabled.</green>");
        changed |= checkStringDefault("chat.togglechat.disabled-message", "<green>Public chat enabled.</green>");
        changed |= checkStringDefault("chat.togglechat.reminder", "<red>You have chat disabled. Use /togglechat to enable.</red>");
        changed |= checkStringDefault("chat.togglepm.enabled-message", "<green>Private messages disabled.</green>");
        changed |= checkStringDefault("chat.togglepm.disabled-message", "<green>Private messages enabled.</green>");

        changed |= checkBooleanDefault("chat.cooldown.enabled");
        changed |= checkDoubleDefault("chat.cooldown.seconds", 3.0);
        changed |= checkStringDefault("chat.cooldown.message", "<red>You must wait %seconds%s before chatting again.</red>");

        changed |= checkBooleanDefault("chat.mention-sound-enabled");
        changed |= checkStringDefault("chat.mention-sound-name", "minecraft:block.note_block.pling");
        changed |= checkDoubleDefault("chat.mention-sound-volume", 1.0);
        changed |= checkDoubleDefault("chat.mention-sound-pitch", 1.0);

        changed |= checkStringDefault("chat.urls.replacement", "***");
        changed |= checkStringListDefault("chat.urls.allowlist", Arrays.asList("fendoris.com", "discord.com/invite/fendoris"));

        changed |= checkBooleanDefault("chat.profanity.enabled");
        changed |= checkStringListDefault("chat.profanity.words", List.of("nigger"));

        // --- HelpMe (/ineedhelp) ---
        changed |= checkBooleanDefault("helpme.enabled");
        changed |= checkIntDefault("helpme.cooldown-seconds", 60);
        changed |= checkIntDefault("helpme.cooldown-no-ops-online-seconds", 10);
        changed |= checkStringDefault("helpme.only-player-message", "<red>Only players can use this command.</red>");
        changed |= checkStringDefault("helpme.disabled-message", "<red>This command is currently disabled.</red>");
        changed |= checkStringDefault("helpme.default-message", "Player needs help.");
        changed |= checkStringDefault("helpme.broadcast", "<gold>[Help]</gold> <white>%player%</white>: %message%");
        changed |= checkStringDefault("helpme.ack-message", "<green>Operators have been notified.</green>");
        changed |= checkStringDefault("helpme.no-operators", "<yellow>No operators are online.</yellow>");
        changed |= checkStringDefault("helpme.cooldown-message", "<red>Wait %seconds%s before using this again.</red>");

        if (changed) {
            plugin.saveConfig();
            plugin.getLogger().info(PREFIX + "Missing/invalid config values set to defaults and saved.");
        }
    }

    private boolean migrateToggleKeys(String base) {
        boolean mutated = false;

        String onVal = config.getString(base + ".on");
        if (onVal == null) onVal = config.getString(base + ".true");
        String offVal = config.getString(base + ".off");
        if (offVal == null) offVal = config.getString(base + ".false");

        if (onVal != null && !config.isString(base + ".enabled-message")) {
            config.set(base + ".enabled-message", onVal);
            mutated = true;
        }
        if (offVal != null && !config.isString(base + ".disabled-message")) {
            config.set(base + ".disabled-message", offVal);
            mutated = true;
        }
        if (config.contains(base + ".on")) {
            config.set(base + ".on", null);
            mutated = true;
        }
        if (config.contains(base + ".off")) {
            config.set(base + ".off", null);
            mutated = true;
        }
        if (config.contains(base + ".true")) {
            config.set(base + ".true", null);
            mutated = true;
        }
        if (config.contains(base + ".false")) {
            config.set(base + ".false", null);
            mutated = true;
        }

        return mutated;
    }

    private boolean checkBooleanDefault(String path) {
        if (!config.contains(path) || !(config.get(path) instanceof Boolean)) {
            config.set(path, true);
            plugin.getLogger().warning(PREFIX + "Invalid or missing boolean at '" + path + "'. Setting default: " + true);
            return true;
        }
        return false;
    }

    private boolean checkStringDefault(String path, String defaultValue) {
        if (!config.contains(path) || !(config.get(path) instanceof String)) {
            config.set(path, defaultValue);
            plugin.getLogger().warning(PREFIX + "Invalid or missing string at '" + path + "'. Setting default.");
            return true;
        }
        return false;
    }

    private boolean checkIntDefault(String path, int defaultValue) {
        Object value = config.get(path);
        if (!(value instanceof Integer)) {
            config.set(path, defaultValue);
            plugin.getLogger().warning(PREFIX + "Invalid or missing integer at '" + path + "'. Setting default: " + defaultValue);
            return true;
        }
        int intValue = (Integer) value;
        if (intValue < -1 || intValue > 600) {
            config.set(path, Math.max(-1, Math.min(600, intValue)));
            plugin.getLogger().warning(PREFIX + "Integer out of bounds at '" + path + "'. Clamping.");
            return true;
        }
        return false;
    }

    private boolean checkDoubleDefault(String path, double defaultValue) {
        Object value = config.get(path);
        if (value instanceof Number) {
            return false;
        } else {
            config.set(path, defaultValue);
            plugin.getLogger().warning(PREFIX + "Invalid or missing number at '" + path + "'. Setting default: " + defaultValue);
            return true;
        }
    }

    private boolean checkStringListDefault(String path, List<String> defaultValue) {
        Object value = config.get(path);
        if (!(value instanceof List)) {
            config.set(path, defaultValue);
            plugin.getLogger().warning(PREFIX + "Invalid or missing list at '" + path + "'. Setting default.");
            return true;
        }
        return false;
    }
}
