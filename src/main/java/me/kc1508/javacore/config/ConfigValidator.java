package me.kc1508.javacore.config;

import me.kc1508.javacore.FendorisPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigValidator {

    private final FendorisPlugin plugin;
    private final FileConfiguration config;

    private static final String PREFIX = "[Fendoris] [ConfigValidator] ";
    private static final String FALLBACK = "§cLanguage string invalid in config.";

    public ConfigValidator(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void validate() {
        boolean changed = false;

        // Join/Quit Messages
        changed |= checkBooleanDefault("private-join-message-enabled", true);
        changed |= checkBooleanDefault("public-join-message-enabled", true);
        changed |= checkBooleanDefault("public-quit-message-enabled", true);

        changed |= checkStringDefault("private-join-message", "<gray>%player% connected</gray><reset>");
        changed |= checkStringDefault("public-join-message", "<gray>%player% connected</gray><reset>");
        changed |= checkStringDefault("public-quit-message", "<gray>%player% disconnected</gray><reset>");

        // PvP System
        changed |= checkBooleanDefault("system.pvp.enabled", true);
        changed |= checkBooleanDefault("system.pvp.cooldown-enabled", true);
        changed |= checkIntDefault("system.pvp.cooldown-seconds", 30);
        changed |= checkIntDefault("system.pvp.message-cooldown-seconds", 15);

        changed |= checkBooleanDefault("system.pvp.combat-cooldown-enabled", true);
        changed |= checkIntDefault("system.pvp.combat-cooldown-seconds", 10);

        // PvP Message Strings
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

        // Session Code System
        changed |= checkBooleanDefault("session.enabled", true);
        changed |= checkIntDefault("session.code-length", 6);
        changed |= checkStringDefault("session.only-player-message", "<red>Only players can use this command.<reset>");
        changed |= checkStringDefault("session.usage-message", "<red>Usage: /session (Shows your current Session Code)<reset>");
        changed |= checkStringDefault("session.disabled-message", "<red>The Session Code system is currently disabled.<reset>");
        changed |= checkStringDefault("session.code-message", "<reset><newline><reset><red>Your session code is <red><bold>%session_code%<reset><red>, reconnecting will generate another one.<reset>");

        // Death Messages
        changed |= checkBooleanDefault("death-message-enabled", true);
        changed |= checkStringDefault("death-message-prefix", "<red>[Death]</red>");

        // Reload Command
        changed |= checkStringDefault("reload.no-permission", "<red>You don't have permission to reload the plugin.</red>");
        changed |= checkStringDefault("reload.reload-success", "<gold>Config reloaded.</gold>");
        changed |= checkStringDefault("reload.reload-broadcast", "<gray>[<gold>Operator</gold>: <white>%player%</white> reloaded the plugin]</gray>");

        // Spawn System
        changed |= checkBooleanDefault("spawn.enabled", true);

        changed |= checkStringDefault("spawn.location-world", "world");
        changed |= checkDoubleDefault("spawn.location-x", 0.5);
        changed |= checkDoubleDefault("spawn.location-y", 100.0);
        changed |= checkDoubleDefault("spawn.location-z", 0.5);
        changed |= checkDoubleDefault("spawn.location-yaw", 0.0);
        changed |= checkDoubleDefault("spawn.location-pitch", 0.0);

        changed |= checkIntDefault("spawn.cooldown", 0);

        changed |= checkStringDefault("spawn.only-player-message", "<red>Only players can use this command.<reset>");
        changed |= checkStringDefault("spawn.usage-message", "<red>Usage: /spawn (Teleports you to the server spawn location)<reset>");
        changed |= checkStringDefault("spawn.disabled-message", "<red>The Spawn system is currently disabled on this server.<reset>");
        changed |= checkStringDefault("spawn.teleport-success-message", "<green>You have been teleported to the server spawn.<reset>");
        changed |= checkStringDefault("spawn.no-permission-message", "<red>You don't have permission to use this command.<reset>");
        changed |= checkStringDefault("spawn.sendtospawn-usage", "<red>Usage: /sendtospawn <player><reset>");
        changed |= checkStringDefault("spawn.player-not-online", "<red>That player is not online.<reset>");
        changed |= checkStringDefault("spawn.sent-to-spawn-message", "<green>You have been sent to spawn by an operator.<reset>");
        changed |= checkStringDefault("spawn.operator-sent-message", "<green>Sent %player% to spawn.<reset>");
        changed |= checkStringDefault("spawn.cooldown-message", "<red>You must wait %seconds%s before using this again.<reset>");
        changed |= checkStringDefault("spawn.set-success", "<green>Spawn location saved to config.<reset>");

        if (changed) {
            plugin.saveConfig();
            plugin.getLogger().info(PREFIX + "Missing/invalid config values set to defaults and saved.");
        }
    }

    private boolean checkBooleanDefault(String path, boolean defaultValue) {
        if (!config.contains(path) || !(config.get(path) instanceof Boolean)) {
            config.set(path, defaultValue);
            plugin.getLogger().warning(PREFIX + "Invalid or missing boolean at '" + path + "'. Setting default: " + defaultValue);
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
}
