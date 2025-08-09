package me.kc1508.javacore.hologram;

import me.kc1508.javacore.FendorisPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.logging.Logger;

public class HologramManager {
    private final FendorisPlugin plugin;
    private final MiniMessage mini;
    private final Logger log;
    private final NamespacedKey pdcKey;

    // id -> entity UUID
    private final Map<Integer, UUID> active = new HashMap<>();

    public HologramManager(FendorisPlugin plugin) {
        this.plugin = plugin;
        this.mini = MiniMessage.miniMessage();
        this.log = plugin.getLogger();
        this.pdcKey = new NamespacedKey(plugin, "hologram-id");
    }

    public void loadFromConfig() {
        // Despawn any active we created to avoid duplication
        despawnAll();
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection root = cfg.getConfigurationSection("holograms");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                try {
                    int id = Integer.parseInt(key);
                    ConfigurationSection sec = root.getConfigurationSection(key);
                    if (sec != null) spawnFromSection(id, sec);
                } catch (NumberFormatException nfe) {
                    log.warning("[Hologram] Non-numeric hologram key: " + key + " — skipping.");
                }
            }
        }
        // Ensure next-hologram is greater than any existing id
        int next = Math.max(0, cfg.getInt("next-hologram", 0));
        int maxId = active.keySet().stream().max(Integer::compareTo).orElse(-1);
        if (next <= maxId) {
            cfg.set("next-hologram", maxId + 1);
            plugin.saveConfig();
        }
    }

    public void reload() {
        loadFromConfig();
    }

    private void despawnAll() {
        for (UUID uuid : active.values()) {
            Entity e = Bukkit.getEntity(uuid);
            if (e != null) e.remove();
        }
        active.clear();
    }

    private Location sectionToLocation(ConfigurationSection sec) {
        String worldName = sec.getString("location.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        double x = sec.getDouble("location.x", 0.0);
        double y = sec.getDouble("location.y", 64.0);
        double z = sec.getDouble("location.z", 0.0);
        float yaw = (float) sec.getDouble("location.yaw", 0.0);
        float pitch = (float) sec.getDouble("location.pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void writeLocation(ConfigurationSection sec, Location loc) {
        sec.set("location.world", Objects.requireNonNull(loc.getWorld()).getName());
        sec.set("location.x", loc.getX());
        sec.set("location.y", loc.getY());
        sec.set("location.z", loc.getZ());
        sec.set("location.yaw", loc.getYaw());
        sec.set("location.pitch", loc.getPitch());
    }

    private void applyTextAndBasicProps(TextDisplay display, ConfigurationSection sec) {
        String raw = sec.getString("text", "Example Text");
        // Support \\n literal newlines -> MiniMessage <newline>
        raw = raw.replace("\\n", "<newline>");
        Component comp;
        try {
            comp = mini.deserialize(raw);
        } catch (Exception e) {
            comp = Component.text(raw);
        }
        display.text(comp);

        ConfigurationSection props = sec.getConfigurationSection("properties");
        if (props != null) {
            display.setShadowed(props.getBoolean("shadow", true));
            display.setSeeThrough(props.getBoolean("see_through", false));

            String align = props.getString("alignment", "center").toUpperCase(Locale.ROOT);
            try {
                display.setAlignment(TextDisplay.TextAlignment.valueOf(align));
            } catch (IllegalArgumentException ignored) {
                display.setAlignment(TextDisplay.TextAlignment.CENTER);
            }

            String billboard = props.getString("billboard", "center").toUpperCase(Locale.ROOT);
            try {
                display.setBillboard(Billboard.valueOf(billboard));
            } catch (IllegalArgumentException ignored) {
                display.setBillboard(Billboard.CENTER);
            }
        }
    }

    private void applyVisualProps(TextDisplay display, @Nullable ConfigurationSection props) {
        if (props == null) return;

        // Background color (accepts "#RRGGBB" or "#AARRGGBB")
        String bg = props.getString("background", null);
        if (bg != null) {
            Color c = parseColor(bg);
            if (c != null) display.setBackgroundColor(c);
        }

        // Text opacity 0..255
        int opacity = props.getInt("text_opacity", -1);
        if (opacity >= 0) {
            if (opacity < 0) opacity = 0;
            if (opacity > 255) opacity = 255;
            display.setTextOpacity((byte) (opacity & 0xFF));
        }

        // Transformation: translation + scale + rotation (yaw/pitch/roll in degrees)
        double s = props.getDouble("scale", 1.0);
        float sf = (float) s;
        Vector3f scale = new Vector3f(sf, sf, sf);

        ConfigurationSection t = props.getConfigurationSection("translation");
        float tx = t != null ? (float) t.getDouble("x", 0.0) : 0.0f;
        float ty = t != null ? (float) t.getDouble("y", 0.0) : 0.0f;
        float tz = t != null ? (float) t.getDouble("z", 0.0) : 0.0f;
        Vector3f translation = new Vector3f(tx, ty, tz);

        ConfigurationSection r = props.getConfigurationSection("rotation");
        float yawDeg = r != null ? (float) r.getDouble("yaw", 0.0) : 0.0f;
        float pitchDeg = r != null ? (float) r.getDouble("pitch", 0.0) : 0.0f;
        float rollDeg = r != null ? (float) r.getDouble("roll", 0.0) : 0.0f;
        float yaw = (float) Math.toRadians(yawDeg);
        float pitch = (float) Math.toRadians(pitchDeg);
        float roll = (float) Math.toRadians(rollDeg);

        // Use leftRotation for a simple rotation; rightRotation = identity
        Quaternionf left = new Quaternionf().rotationYXZ(yaw, pitch, roll);
        Quaternionf right = new Quaternionf();

        display.setTransformation(new Transformation(translation, left, scale, right));
    }

    private @Nullable Color parseColor(String s) {
        String str = s.trim();
        if (str.startsWith("#")) str = str.substring(1);
        try {
            long val = Long.parseLong(str, 16);
            if (str.length() == 6) {
                int r = (int) ((val >> 16) & 0xFF);
                int g = (int) ((val >> 8) & 0xFF);
                int b = (int) (val & 0xFF);
                return Color.fromRGB(r, g, b);
            } else if (str.length() == 8) {
                int a = (int) ((val >> 24) & 0xFF);
                int r = (int) ((val >> 16) & 0xFF);
                int g = (int) ((val >> 8) & 0xFF);
                int b = (int) (val & 0xFF);
                try {
                    // Available in modern Bukkit
                    return Color.fromARGB(a, r, g, b);
                } catch (NoSuchMethodError err) {
                    // Fallback: ARGB not supported; approximate via RGB (alpha ignored)
                    return Color.fromRGB(r, g, b);
                }
            }
        } catch (NumberFormatException ignored) { }
        return null;
    }

    private void tagWithId(TextDisplay display, int id) {
        PersistentDataContainer pdc = display.getPersistentDataContainer();
        pdc.set(pdcKey, PersistentDataType.INTEGER, id);
    }

    private void spawnFromSection(int id, ConfigurationSection sec) {
        Location loc = sectionToLocation(sec);
        TextDisplay display = loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            applyTextAndBasicProps(td, sec);
            applyVisualProps(td, sec.getConfigurationSection("properties"));
            tagWithId(td, id);
        });
        active.put(id, display.getUniqueId());
    }

    public int createAtPlayer(Player player, @Nullable String initialText) {
        FileConfiguration cfg = plugin.getConfig();
        int id = Math.max(0, cfg.getInt("next-hologram", 0));

        // Create section
        ConfigurationSection sec = cfg.createSection("holograms." + id);
        sec.set("text", initialText == null ? "New Hologram" : initialText);
        writeLocation(sec, player.getLocation());
        ConfigurationSection props = sec.createSection("properties");
        props.set("shadow", true);
        props.set("see_through", false);
        props.set("alignment", "center");
        props.set("billboard", "center");
        props.set("background", "#00000000"); // ARGB (fully transparent)
        props.set("text_opacity", 255);       // 0..255
        props.set("scale", 1.0);
        ConfigurationSection t = props.createSection("translation");
        t.set("x", 0.0); t.set("y", 0.0); t.set("z", 0.0);
        ConfigurationSection r = props.createSection("rotation");
        r.set("yaw", 0.0); r.set("pitch", 0.0); r.set("roll", 0.0);

        // Persist + bump counter
        cfg.set("next-hologram", id + 1);
        plugin.saveConfig();

        // Spawn
        spawnFromSection(id, sec);
        return id;
    }

    public boolean bringToPlayer(int id, Player player, boolean matchYawPitch) {
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("holograms." + id);
        if (sec == null) return false;
        Location dst = player.getLocation();
        if (!matchYawPitch) {
            Location current = sectionToLocation(sec);
            dst.setYaw(current.getYaw());
            dst.setPitch(current.getPitch());
        }
        // Update config & entity
        writeLocation(sec, dst);
        Entity e = getOrRespawn(id, sec);
        if (e != null) e.teleport(dst);
        plugin.saveConfig();
        return true;
    }

    public boolean delete(int id) {
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("holograms." + id);
        if (sec == null) return false;
        Entity e = get(id);
        if (e != null) e.remove();
        active.remove(id);
        cfg.set("holograms." + id, null);
        // Do not change next-hologram per requirement
        plugin.saveConfig();
        return true;
    }

    public Entity get(int id) {
        UUID u = active.get(id);
        return u == null ? null : Bukkit.getEntity(u);
    }

    private Entity getOrRespawn(int id, ConfigurationSection sec) {
        Entity e = get(id);
        if (e != null && !e.isDead()) return e;
        spawnFromSection(id, sec);
        return get(id);
    }

    public List<String> listSummary() {
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection root = cfg.getConfigurationSection("holograms");
        if (root == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            Location loc = sectionToLocation(sec);
            String txt = sec.getString("text", "Example Text");
            out.add("#" + key + " • " + loc.getWorld().getName() + " " +
                    String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ()) +
                    " • " + (txt.length() > 32 ? txt.substring(0, 32) + "…" : txt));
        }
        return out;
    }

    public boolean setText(int id, String raw) {
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("holograms." + id);
        if (sec == null) return false;
        sec.set("text", raw);
        Entity e = getOrRespawn(id, sec);
        if (e instanceof TextDisplay td) {
            applyTextAndBasicProps(td, sec);
            applyVisualProps(td, sec.getConfigurationSection("properties"));
        }
        plugin.saveConfig();
        return true;
    }
}