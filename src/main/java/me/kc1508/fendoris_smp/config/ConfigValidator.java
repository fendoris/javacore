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
}
