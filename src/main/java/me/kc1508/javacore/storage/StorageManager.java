package me.kc1508.javacore.storage;

import me.kc1508.javacore.FendorisPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class StorageManager {

    private final FendorisPlugin plugin;
    private File storageFile;
    private FileConfiguration storageConfig;

    public StorageManager(FendorisPlugin plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }
        storageFile = new File(plugin.getDataFolder(), "storage.yml");
        if (!storageFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                storageFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create storage.yml: " + e.getMessage());
            }
        }
        storageConfig = YamlConfiguration.loadConfiguration(storageFile);
    }

    public void reload() {
        storageConfig = YamlConfiguration.loadConfiguration(storageFile);
    }

    public void save() {
        try {
            storageConfig.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save storage.yml: " + e.getMessage());
        }
    }

    public Optional<Location> getHome(UUID uuid) {
        String base = "homes." + uuid;
        if (!storageConfig.isConfigurationSection(base)) {
            return Optional.empty();
        }
        String worldName = storageConfig.getString(base + ".world");
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        if (world == null) {
            return Optional.empty();
        }
        double x = storageConfig.getDouble(base + ".x", Double.NaN);
        double y = storageConfig.getDouble(base + ".y", Double.NaN);
        double z = storageConfig.getDouble(base + ".z", Double.NaN);
        float yaw = (float) storageConfig.getDouble(base + ".yaw", 0.0);
        float pitch = (float) storageConfig.getDouble(base + ".pitch", 0.0);

        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, x, y, z, yaw, pitch));
    }

    public void setHome(UUID uuid, Location loc) {
        String base = "homes." + uuid;
        storageConfig.set(base + ".world", loc.getWorld() != null ? loc.getWorld().getName() : null);
        storageConfig.set(base + ".x", loc.getX());
        storageConfig.set(base + ".y", loc.getY());
        storageConfig.set(base + ".z", loc.getZ());
        storageConfig.set(base + ".yaw", loc.getYaw());
        storageConfig.set(base + ".pitch", loc.getPitch());
        save();
    }
}

