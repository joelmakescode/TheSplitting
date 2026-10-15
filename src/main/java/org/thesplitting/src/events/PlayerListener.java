package org.thesplitting.src.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesplitting.src.misc.errorhandler.PlayerErrorHandler;
import org.thesplitting.src.misc.exceptions.PlayerDataNotFoundException;
import org.thesplitting.src.services.*;
import org.thesplitting.src.core.contracts.IService;

import java.util.logging.Level;

record PlayerJoinRequest(String uuid, String name){}
record PlayerJoinResponse(String uuid, String name, String createdAt, String updatedAt){}

public record PlayerListener(JavaPlugin plugin, PlayerService playerService, ChatControlService chatControlService, InventoryService inventoryService, ScoreboardService scoreboardService, PlayerErrorHandler playerErrorHandler, ApiService apiService) implements Listener, IService {

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        try {
            playerService.initiatePlayerSetup(player);
            chatControlService.loadChatControlFile(player);
            scoreboardService.updatePlayerTeam(player);
            apiService
                    .post("/minecraft/users/join", new PlayerJoinRequest(player.getUniqueId().toString(), player.getName()), PlayerJoinResponse.class)
                    .thenAccept(response -> {
                        apiService.runSync(() -> {
                            MessageService.infoMessage(player, "Successfully joined the server!");
                        });
                    })
                    .exceptionally(exception -> {
                        plugin.getLogger().log(Level.SEVERE, "Failed to join the server!", exception);
                        exception.printStackTrace();
                        return null;
                    });
        } catch (Exception ex) {
            playerErrorHandler.playerDataNotFoundError(player);
            throw new PlayerDataNotFoundException(player.getName());
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        inventoryService.clearPlayerCache(e.getPlayer().getUniqueId());
        playerService.savePlayerFile(e.getPlayer());
        playerService.removeCachedPlayerData(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.setDeathMessage(null);
        e.setDroppedExp(0);
        e.getDrops().clear();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            playerService.initiatePlayerSetup(player);
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // This has to be made area specific later
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        // This has to be made area specific later
        e.setCancelled(true);
    }

    @EventHandler
    public void onChatMessage(PlayerChatEvent event) {
        chatControlService.takeOverChatListener(event);
    }
}
