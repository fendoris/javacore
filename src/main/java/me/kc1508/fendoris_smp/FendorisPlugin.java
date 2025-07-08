package me.kc1508.fendoris_smp;

import me.kc1508.fendoris_smp.commands.PvpCommand;
import me.kc1508.fendoris_smp.commands.ReloadCommand;
import me.kc1508.fendoris_smp.config.ConfigValidator;
import me.kc1508.fendoris_smp.listeners.PlayerJoinQuitListener;
import me.kc1508.fendoris_smp.listeners.PvpListener;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import me.kc1508.fendoris_smp.commands.PvpTabCompleter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class FendorisPlugin extends JavaPlugin {

    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    private PlayerJoinQuitListener playerListener;

    private static final boolean devModeConfigReset = true;

    private final Set<UUID> pvpEnabledPlayers = new HashSet<>();

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        getLogger().info(ANSI_GREEN + "Fendoris SMP plugin is starting..." + ANSI_RESET);

        if (devModeConfigReset) {
            getLogger().warning(ANSI_RED + "Development mode: Overwriting config.yml..." + ANSI_RESET);
            saveResource("config.yml", true);
            reloadConfig();
        } else {
            saveDefaultConfig();
            getConfig().options().copyDefaults(true);
            saveConfig();
        }

        new ConfigValidator(this).validate();

        playerListener = new PlayerJoinQuitListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);

        Objects.requireNonNull(getCommand("fendorisreload")).setExecutor(new ReloadCommand(this, playerListener));
        Objects.requireNonNull(getCommand("pvp")).setExecutor(new PvpCommand(this));
        Objects.requireNonNull(getCommand("pvp")).setTabCompleter(new PvpTabCompleter());
        getServer().getPluginManager().registerEvents(new PvpListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info(ANSI_RED + "Fendoris SMP plugin is stopping..." + ANSI_RESET);
    }

    @SuppressWarnings("unused")
    public PlayerJoinQuitListener getPlayerListener() {
        return playerListener;
    }

    public Set<UUID> getPvpEnabledPlayers() {
        return pvpEnabledPlayers;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPvpToggleEnabled() {
        return getConfig().getBoolean("pvp-toggle-system-enabled", true);
    }

    public boolean isPvpCooldownEnabled() {
        return getConfig().getBoolean("pvp-toggle-cooldown-enabled", true);
    }

    public int getPvpCooldownSeconds() {
        return Math.min(300, Math.max(0, getConfig().getInt("pvp-toggle-cooldown-seconds", 30)));
    }

    public int getPvpMessageCooldownSeconds() {
        return Math.min(300, Math.max(0, getConfig().getInt("pvp-message-cooldown-seconds", 15)));
    }

    // New combat cooldown config options
    public boolean isPvpCombatCooldownEnabled() {
        return getConfig().getBoolean("pvp-toggle-combat-cooldown-enabled", true);
    }

    public int getPvpCombatCooldownSeconds() {
        return Math.min(300, Math.max(0, getConfig().getInt("pvp-toggle-combat-cooldown-seconds", 10)));
    }

    @SuppressWarnings("unused")
    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    @SuppressWarnings("unused")
    public String getMessage(String key, String defaultMessage) {
        return getConfig().getString(key, defaultMessage);
    }
}
