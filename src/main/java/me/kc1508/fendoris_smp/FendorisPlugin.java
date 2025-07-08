package me.kc1508.fendoris_smp;

import me.kc1508.fendoris_smp.commands.ReloadCommand;
import me.kc1508.fendoris_smp.config.ConfigValidator;
import me.kc1508.fendoris_smp.listeners.PlayerJoinQuitListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class FendorisPlugin extends JavaPlugin {

    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    private PlayerJoinQuitListener playerListener;

    // Toggle this to true if you want the config to regenerate every startup (for development)
    private static final boolean devModeConfigReset = false;

    @Override
    public void onEnable() {
        getLogger().info(ANSI_GREEN + "Fendoris SMP plugin is starting..." + ANSI_RESET);

        if (devModeConfigReset) {
            getLogger().warning(ANSI_RED + "Development mode: Overwriting config.yml..." + ANSI_RESET);
            saveResource("config.yml", true); // force overwrite
            reloadConfig();
        } else {
            saveDefaultConfig();
            getConfig().options().copyDefaults(true);
            saveConfig();
        }

        new ConfigValidator(this).validate();

        playerListener = new PlayerJoinQuitListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);

        // Register commands
        Objects.requireNonNull(getCommand("fendorisreload")).setExecutor(new ReloadCommand(this, playerListener));
    }

    @Override
    public void onDisable() {
        getLogger().info(ANSI_RED + "Fendoris SMP plugin is stopping..." + ANSI_RESET);
    }

    public PlayerJoinQuitListener getPlayerListener() {
        return playerListener;
    }
}
