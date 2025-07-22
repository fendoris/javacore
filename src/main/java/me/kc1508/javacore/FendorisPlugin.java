package me.kc1508.javacore;

import me.kc1508.javacore.commands.PvpCommand;
import me.kc1508.javacore.commands.ReloadCommand;
import me.kc1508.javacore.commands.PvpTabCompleter;
import me.kc1508.javacore.commands.SessionCommand;
import me.kc1508.javacore.commands.SessionTabCompleter;
import me.kc1508.javacore.config.ConfigValidator;
import me.kc1508.javacore.listeners.AllowedCommandListener;
import me.kc1508.javacore.listeners.PlayerDeathListener;
import me.kc1508.javacore.listeners.PlayerJoinQuitListener;
import me.kc1508.javacore.listeners.ServerPingListener;
import me.kc1508.javacore.listeners.PvpListener;
import me.kc1508.javacore.tablist.TabListManager;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class FendorisPlugin extends JavaPlugin {

    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    private PlayerJoinQuitListener playerListener;
    private AllowedCommandListener allowedCommandListener;
    private ServerPingListener serverPingListener;

    private static final boolean devModeConfigReset = true;

    private final Set<UUID> pvpEnabledPlayers = new HashSet<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private TabListManager tabListManager;

    @Override
    public void onEnable() {
        getLogger().info(ANSI_GREEN + "JavaCore Plugin Starting" + ANSI_RESET);

        if (devModeConfigReset) {
            getLogger().warning(ANSI_RED + "Development Mode Enabled, Overwriting config.yml" + ANSI_RESET);
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

        allowedCommandListener = new AllowedCommandListener(this);
        getServer().getPluginManager().registerEvents(allowedCommandListener, this);
        allowedCommandListener.reloadBlockedCommands();

        PvpCommand pvpCommand = new PvpCommand(this);
        Objects.requireNonNull(getCommand("pvp")).setExecutor(pvpCommand);
        Objects.requireNonNull(getCommand("pvp")).setTabCompleter(pvpCommand);

        getServer().getPluginManager().registerEvents(new PvpListener(this, pvpCommand), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);

        serverPingListener = new ServerPingListener(this);
        getServer().getPluginManager().registerEvents(serverPingListener, this);

        Objects.requireNonNull(getCommand("fendorisreload")).setExecutor(
                new ReloadCommand(this, playerListener, allowedCommandListener, serverPingListener)
        );

        Objects.requireNonNull(getCommand("session")).setExecutor(new SessionCommand(this));
        Objects.requireNonNull(getCommand("session")).setTabCompleter(new SessionTabCompleter());

        tabListManager = new TabListManager(this);

        // Move ensureTeamsExist() call here, AFTER TabListManager is instantiated
        tabListManager.ensureTeamsExist();

        if (getConfig().getBoolean("tablist-enabled", false)) {
            tabListManager.start();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info(ANSI_RED + "JavaCore Plugin Stopping" + ANSI_RESET);
        if (tabListManager != null) tabListManager.stop();
    }

    @SuppressWarnings("unused")
    public PlayerJoinQuitListener getPlayerListener() {
        return playerListener;
    }

    @SuppressWarnings("unused")
    public AllowedCommandListener getBlockedCommandListener() {
        return allowedCommandListener;
    }

    @SuppressWarnings("unused")
    public ServerPingListener getServerPingListener() {
        return serverPingListener;
    }

    public Set<UUID> getPvpEnabledPlayers() {
        return pvpEnabledPlayers;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPvpToggleEnabled() {
        return getConfig().getBoolean("system.pvp.enabled", true);
    }

    @SuppressWarnings("unused")
    public boolean isPvpCooldownEnabled() {
        return getConfig().getBoolean("system.pvp.cooldown-enabled", true);
    }

    @SuppressWarnings("unused")
    public int getPvpCooldownSeconds() {
        return Math.min(300, Math.max(0, getConfig().getInt("system.pvp.cooldown-seconds", 30)));
    }

    public int getPvpMessageCooldownSeconds() {
        return Math.min(300, Math.max(0, getConfig().getInt("system.pvp.message-cooldown-seconds", 15)));
    }

    public boolean isPvpCombatCooldownEnabled() {
        return getConfig().getBoolean("system.pvp.combat-cooldown-enabled", true);
    }

    @SuppressWarnings("unused")
    public int getPvpCombatCooldownSeconds() {
        return Math.min(300, Math.max(0, getConfig().getInt("system.pvp.combat-cooldown-seconds", 10)));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isCommandWhitelistEnabled() {
        return getConfig().getBoolean("command-whitelist-enabled", true);
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public void broadcastToOPsExceptSender(String senderName, String messageKey, String... replacements) {
        if (!getConfig().getBoolean("operator-command-logs-enabled", true)) return;

        String raw = getConfig().getString(messageKey);
        if (raw == null || raw.isBlank()) {
            raw = "[Missing config string for " + messageKey + "]";
            getLogger().warning("[broadcastToOPsExceptSender] Missing config key: " + messageKey);
        }

        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = replacements[i];
                String value = replacements[i + 1];
                raw = raw.replace(placeholder, value);
            }
        } else {
            getLogger().warning("[broadcastToOPsExceptSender] Replacement array length is not even!");
        }

        final String message = raw;

        getServer().getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("fendoris.operator") && !player.getName().equalsIgnoreCase(senderName)) {
                player.sendMessage(miniMessage.deserialize(message));
            }
        });

        String plain = miniMessage.stripTags(message);
        getLogger().info("[Command Log] " + plain);
    }
}
