package me.kc1508.javacore.config;

import me.kc1508.javacore.FendorisPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigValidator {

    private final FendorisPlugin plugin;
    private final FileConfiguration config;

    public ConfigValidator(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void validate() {
        // Join/Quit Messages
        checkBoolean("private-join-message-enabled");
        checkBoolean("public-join-message-enabled");
        checkBoolean("public-quit-message-enabled");

        checkString("private-join-message");
        checkString("public-join-message");
        checkString("public-quit-message");

        // PvP System
        checkBoolean("system.pvp.enabled");
        checkBoolean("system.pvp.cooldown-enabled");
        checkInt("system.pvp.cooldown-seconds");
        checkInt("system.pvp.message-cooldown-seconds");

        checkBoolean("system.pvp.combat-cooldown-enabled");
        checkInt("system.pvp.combat-cooldown-seconds");

        // PvP Message Strings
        checkString("system.pvp.only-player-message");
        checkString("system.pvp.usage-message");
        checkString("system.pvp.toggle-disabled-message");
        checkString("system.pvp.combat-cooldown-message-less-than-1");
        checkString("system.pvp.combat-cooldown-message");
        checkString("system.pvp.toggle-cooldown-message-less-than-1");
        checkString("system.pvp.toggle-cooldown-message");
        checkString("system.pvp.disabled-message");
        checkString("system.pvp.enabled-message");
        checkString("system.pvp.operator-pvp-toggle-off");
        checkString("system.pvp.operator-pvp-toggle-on");
        checkString("system.pvp.attacker-disabled-message");
        checkString("system.pvp.victim-disabled-message");

        // Death Messages
        checkBoolean("death-message-enabled");
        checkString("death-message-prefix");
    }

    private void checkBoolean(String path) {
        Object value = config.get(path);
        if (!(value instanceof Boolean)) {
            plugin.getLogger().warning("Invalid config value at '" + path + "': expected true or false. Default will be used.");
        }
    }

    private void checkString(String path) {
        Object value = config.get(path);
        if (!(value instanceof String)) {
            plugin.getLogger().warning("Invalid config value at '" + path + "': expected a string. Default will be used.");
        }
    }

    private void checkInt(String path) {
        Object value = config.get(path);
        if (!(value instanceof Integer)) {
            plugin.getLogger().warning("Invalid config value at '" + path + "': expected an integer. Default will be used.");
            return;
        }

        int intValue = (Integer) value;
        if (intValue < 0 || intValue > 300) {
            plugin.getLogger().warning("Config value at '" + path + "' is out of bounds (0-300). Clamping.");
        }
    }
}
