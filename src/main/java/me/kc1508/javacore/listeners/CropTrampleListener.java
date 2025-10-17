package me.kc1508.javacore.listeners;

import me.kc1508.javacore.FendorisPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

public class CropTrampleListener implements Listener {

    private final FendorisPlugin plugin;

    public CropTrampleListener(FendorisPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isPreventionEnabled() {
        return plugin.getConfig().getBoolean("system.prevent-crop-trample", true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!isPreventionEnabled()) return;

        Block block = event.getBlock();
        if (block.getType() == Material.FARMLAND) {
            // Trampling turns FARMLAND into DIRT; cancelling prevents crop pop-off
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPhysical(PlayerInteractEvent event) {
        if (!isPreventionEnabled()) return;

        if (event.getAction() != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getType() == Material.FARMLAND) {
            event.setCancelled(true);
        }
    }
}

