package me.kc1508.javacore.listeners;

import me.kc1508.javacore.commands.SessionCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SessionListener implements Listener {

    private final SessionCommand sessionCommand;

    public SessionListener(SessionCommand sessionCommand) {
        this.sessionCommand = sessionCommand;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        sessionCommand.regenerateSessionCode(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessionCommand.clearSessionCode(event.getPlayer().getUniqueId());
    }
}
