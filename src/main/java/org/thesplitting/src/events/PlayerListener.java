package org.thesplitting.src.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.thesplitting.src.data.server.chatcontrol.ChatControlLevel;
import org.thesplitting.src.data.server.chatcontrol.ChatFilterResult;
import org.thesplitting.src.data.server.chatcontrol.MessageData;
import org.thesplitting.src.misc.errorhandler.PlayerErrorHandler;
import org.thesplitting.src.misc.exceptions.PlayerDataNotFoundException;
import org.thesplitting.src.services.services.ChatControlService;
import org.thesplitting.src.services.services.InventoryService;
import org.thesplitting.src.services.services.MessageService;
import org.thesplitting.src.services.services.PlayerService;
import org.thesplitting.src.services.services.ScoreboardService;
import org.thesplitting.src.services.contracts.IService;
import org.thesplitting.src.services.ServiceRegistry;
import org.thesplitting.src.services.services.fileservice.ChatControlFileService;

public record PlayerListener(ServiceRegistry registry, PlayerService playerService, ChatControlService chatControlService, InventoryService inventoryService, ScoreboardService scoreboardService, PlayerErrorHandler playerErrorHandler) implements Listener, IService {

    @Override
    public void onEnable() {
        registry.getPlugin().getServer().getPluginManager().registerEvents(this, registry.getPlugin());
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

        Bukkit.getScheduler().runTask(registry.getPlugin(), () -> {
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
