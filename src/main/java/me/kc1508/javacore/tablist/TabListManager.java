package me.kc1508.javacore.tablist;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.security.SecureRandom;
import java.util.*;

public class TabListManager {

    private final FendorisPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private BukkitTask task;
    private BukkitTask tpsSamplerTask;
    private final Map<Integer, String> onlineCountSubstitutions = new HashMap<>();
    private final Map<UUID, String> sessionCodes = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Deque<Long> tickTimesNanos = new ArrayDeque<>();
    private static final long TEN_SECONDS_NANOS = 10_000_000_000L;

    private Team operatorTeam;
    private Team defaultTeam;

    private boolean warnedScoreboardManagerNotReady = false;
    private boolean colorsSet = false;

    private static final Set<String> VALID_COLORS = Set.of(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold",
            "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"
    );

    public TabListManager(FendorisPlugin plugin) {
        this.plugin = plugin;
        loadOnlineCountSubstitutions();
        ensureTeamsExist();
    }

    public void ensureTeamsExist() {
        // noinspection ConstantConditions
        if (Bukkit.getScoreboardManager() == null) {
            if (!warnedScoreboardManagerNotReady) {
                plugin.getLogger().warning("ScoreboardManager not ready yet, skipping team setup.");
                warnedScoreboardManagerNotReady = true;
            }
            return;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        operatorTeam = scoreboard.getTeam("fendoris_0_operator");
        if (operatorTeam == null) {
            operatorTeam = scoreboard.registerNewTeam("fendoris_0_operator");
        } else {
            operatorTeam.getEntries().forEach(operatorTeam::removeEntry);
        }

        defaultTeam = scoreboard.getTeam("fendoris_1_default");
        if (defaultTeam == null) {
            defaultTeam = scoreboard.registerNewTeam("fendoris_1_default");
        } else {
            defaultTeam.getEntries().forEach(defaultTeam::removeEntry);
        }

        if (!colorsSet) {
            String operatorColor = plugin.getConfig().getString("tab-operator-color", "red").toLowerCase(Locale.ROOT);
            if (!VALID_COLORS.contains(operatorColor)) {
                plugin.getLogger().warning("Invalid color for 'tab-operator-color': '" + operatorColor + "'. Defaulting to 'red'.");
                operatorColor = "red";
            }

            plugin.getLogger().info("Setting Team Colors at Startup:");
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                    "team modify fendoris_0_operator color " + operatorColor);
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                    "team modify fendoris_1_default color white");
            colorsSet = true;
        }
    }

    private void checkTeamsInitialized() {
        if (operatorTeam == null || defaultTeam == null) {
            ensureTeamsExist();
        }
    }

    public void assignPlayerToTeam(Player player) {
        if (!plugin.getConfig().getBoolean("tablist-enabled", false)) return;

        checkTeamsInitialized();

        boolean isOperator = player.hasPermission("fendoris.operator") || player.isOp();

        if (isOperator) {
            if (!operatorTeam.hasEntry(player.getName())) {
                operatorTeam.addEntry(player.getName());
            }
            defaultTeam.removeEntry(player.getName());
        } else {
            if (!defaultTeam.hasEntry(player.getName())) {
                defaultTeam.addEntry(player.getName());
            }
            operatorTeam.removeEntry(player.getName());
        }
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("tablist-enabled", false)) return;

        int interval = plugin.getConfig().getInt("tablist-update-interval-seconds", 10);
        if (interval < 1) interval = 1;

        updateAllPlayers();

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPlayers, interval * 20L, interval * 20L);

        // Start a 1-tick sampler to maintain a 10s TPS window
        if (tpsSamplerTask == null) {
            tpsSamplerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::recordTickSample, 0L, 1L);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        if (tpsSamplerTask != null) {
            tpsSamplerTask.cancel();
            tpsSamplerTask = null;
        }
    }

    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTabList(player);
        }
    }

    public void updateTabList(Player player) {
        if (!plugin.getConfig().getBoolean("tablist-enabled", false)) return;

        checkTeamsInitialized();
        assignPlayerToTeam(player);

        String headerRaw = plugin.getConfig().getString("tablist-header", "");
        String footerRaw = plugin.getConfig().getString("tablist-footer", "");

        String tps = formatTps(getTpsLast10s());

        String header = replacePlaceholders(headerRaw, player, tps);
        String footer = replacePlaceholders(footerRaw, player, tps);

        Component headerComponent = miniMessage.deserialize(header);
        Component footerComponent = miniMessage.deserialize(footer);

        player.sendPlayerListHeaderAndFooter(headerComponent, footerComponent);
    }

    private void loadOnlineCountSubstitutions() {
        onlineCountSubstitutions.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("online-count-substitutions");
        if (section != null) {
            for (int i = 1; i <= 9; i++) {
                String text = section.getString(String.valueOf(i));
                if (text != null && !text.isBlank()) {
                    onlineCountSubstitutions.put(i, text.trim());
                } else {
                    onlineCountSubstitutions.put(i, spelledOutNumber(i) + " Online Player" + (i > 1 ? "s" : ""));
                }
            }
        } else {
            for (int i = 1; i <= 9; i++) {
                onlineCountSubstitutions.put(i, spelledOutNumber(i) + " Online Player" + (i > 1 ? "s" : ""));
            }
        }
    }

    private String spelledOutNumber(int n) {
        return switch (n) {
            case 1 -> "One";
            case 2 -> "Two";
            case 3 -> "Three";
            case 4 -> "Four";
            case 5 -> "Five";
            case 6 -> "Six";
            case 7 -> "Seven";
            case 8 -> "Eight";
            case 9 -> "Nine";
            default -> String.valueOf(n);
        };
    }

    private String formatTps(double rawTps) {
        if (rawTps >= 19.95) return "20";
        return String.format("%.2f", rawTps);
    }

    private void recordTickSample() {
        long now = System.nanoTime();
        tickTimesNanos.addLast(now);
        // Evict samples older than 10 seconds
        while (!tickTimesNanos.isEmpty() && (now - tickTimesNanos.peekFirst()) > TEN_SECONDS_NANOS) {
            tickTimesNanos.removeFirst();
        }
    }

    private double getTpsLast10s() {
        // Ensure the deque is trimmed relative to current time
        long now = System.nanoTime();
        while (!tickTimesNanos.isEmpty() && (now - tickTimesNanos.peekFirst()) > TEN_SECONDS_NANOS) {
            tickTimesNanos.removeFirst();
        }

        int samples = tickTimesNanos.size();
        if (samples <= 1) {
            // Fallback to Paper's 1m TPS if we don't have enough samples yet
            try {
                double[] arr = plugin.getServer().getTPS();
                return (arr != null && arr.length > 0) ? Math.min(20.0, arr[0]) : 20.0;
            } catch (Throwable ignored) {
                return 20.0;
            }
        }

        long oldest = tickTimesNanos.peekFirst();
        double windowSeconds = (now - oldest) / 1_000_000_000.0;
        // Avoid division by zero and keep within [0.05s, 10s]
        windowSeconds = Math.max(0.05, Math.min(10.0, windowSeconds));

        double tps = samples / windowSeconds;
        if (tps > 20.0) tps = 20.0;
        return tps;
    }

    private int getOnlineCount() {
        int testCount = plugin.getConfig().getInt("tablist-testing-online-count", -1);
        return (testCount > 0) ? testCount : Bukkit.getOnlinePlayers().size();
    }

    private String getOnlineCountText(int onlineCount) {
        return onlineCountSubstitutions.getOrDefault(onlineCount, onlineCount + " Online Players");
    }

    private String generateSessionCode(int length) {
        final String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(random.nextInt(charset.length())));
        }
        return sb.toString();
    }

    public String getSessionCode(Player player) {
        if (!plugin.getConfig().getBoolean("session-code-enabled", false)) return "";
        return sessionCodes.computeIfAbsent(player.getUniqueId(), uuid -> {
            int len = plugin.getConfig().getInt("session-code-length", 6);
            len = Math.max(4, Math.min(32, len));
            return generateSessionCode(len);
        });
    }

    private String replacePlaceholders(String input, Player player, String tps) {
        int onlineCount = getOnlineCount();
        String onlineCountText = getOnlineCountText(onlineCount);
        String sessionCode = getSessionCode(player);

        String replaced = input
                .replace("%player%", player.getName())
                .replace("%ping%", String.valueOf(player.getPing()))
                .replace("%online%", onlineCountText)
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%tps%", tps)
                .replace("%session_code%", sessionCode)
                .replace("<newline>", "\n");

        String[] lines = replaced.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                lines[i] = "\u200A";
            }
        }

        int paddingSpaces = plugin.getConfig().getInt("tablist-padding-spaces", 0);
        if (paddingSpaces > 0) {
            String leftPadding = "\u00A0".repeat(paddingSpaces);
            String rightPadding = "\u00A0".repeat(paddingSpaces);
            for (int i = 0; i < lines.length; i++) {
                lines[i] = leftPadding + lines[i] + rightPadding;
            }
        }

        return String.join("\n", lines);
    }

    public void clearSessionCode(UUID playerId) {
        sessionCodes.remove(playerId);
    }
}
