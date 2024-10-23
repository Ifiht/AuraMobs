package dev.aurelium.auramobs.listeners;

import dev.aurelium.auramobs.AuraMobs;
import dev.aurelium.auramobs.GlobalVars;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinLeave implements Listener {

    private final AuraMobs plugin;

    public PlayerJoinLeave(AuraMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST) // handle AuraSkills race condition...
    public void onJoin(PlayerJoinEvent event) {
        synchronized (GlobalVars.class) {
            int playerLevel = plugin.getLevel(event.getPlayer());
            GlobalVars.globalLevel += playerLevel;
            plugin.logInfo("Player " + event.getPlayer().getName() + " joined, they are level " + playerLevel);
            plugin.logInfo("New global level value: " + GlobalVars.globalLevel);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        synchronized (GlobalVars.class) {
            int leaveLevel = GlobalVars.globalLevel - plugin.getLevel(event.getPlayer());
            GlobalVars.globalLevel = Math.max(leaveLevel, 0);
        }
    }
}
