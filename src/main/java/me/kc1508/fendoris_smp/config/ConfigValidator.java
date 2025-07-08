package me.kc1508.fendoris_smp.config;

import me.kc1508.fendoris_smp.FendorisPlugin;

public class ConfigValidator {
    private final FendorisPlugin plugin;

    public ConfigValidator(FendorisPlugin plugin) {
        this.plugin = plugin;
    }

    public void validate() {
        checkBoolean("private-join-message-enabled");
        checkBoolean("public-join-message-enabled");
        checkBoolean("public-quit-message-enabled");

        checkString("private-join-message");
        checkString("public-join-message");
        checkString("public-quit-message");

        checkBoolean("pvp-toggle-system-enabled");
        checkBoolean("pvp-toggle-cooldown-enabled");
        checkInt("pvp-toggle-cooldown-seconds");
        checkInt("pvp-message-cooldown-seconds");

        // New keys
        checkBoolean("pvp-toggle-combat-cooldown-enabled");
        checkInt("pvp-toggle-combat-cooldown-seconds");
    }

    private void checkBoolean(String path) {
        Object value = plugin.getConfig().get(path);
        if (!(value instanceof Boolean)) {
            plugin.getLogger().warning("Invalid config value at '" + path + "': expected true or false. Default will be used.");
        }
    }

    private void checkString(String path) {
        Object value = plugin.getConfig().get(path);
        if (!(value instanceof String)) {
            plugin.getLogger().warning("Invalid config value at '" + path + "': expected a string. Default will be used.");
        }
    }

    private void checkInt(String path) {
        Object value = plugin.getConfig().get(path);
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
