package org.thesplitting.src.services.services;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.thesplitting.src.services.contracts.IService;
import org.thesplitting.src.data.player.PlayerData;
import org.thesplitting.src.services.services.fileservice.PlayerFileService;
import org.thesplitting.src.services.services.itemservice.ItemService;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class PlayerService implements IService {
    private final PlayerFileService playerFileService;
    private final ItemService itemService;

    private final HashMap<UUID, PlayerData> cachePlayerData = new HashMap<>();

    public PlayerService(PlayerFileService playerFileService, ItemService itemService) {
        this.playerFileService = playerFileService;
        this.itemService = itemService;
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    public void initiatePlayerSetup(Player player) {
        PlayerData playerData = checkForCachePlayerData(player.getUniqueId());
        if (playerData == null) {
            playerData = loadPlayerFile(player);
        }

        loadPlayer(playerData, player);

        if (playerData.playerSettings().getIsStarter() == 1) {
            playerData.playerSettings().setIsStarter(0);
        }

        playerFileService.writePlayerFile(player, playerData);
        cachePlayerData.put(player.getUniqueId(), playerData);
    }

    public PlayerData loadPlayerFile(Player player) {
        PlayerData playerData = cachePlayerData.get(player.getUniqueId());
        if (playerData == null) {
            playerData = playerFileService.readPlayerFile(player);
            if (playerData == null) {
                playerFileService.createPlayerFile(player);
            }
            cachePlayerData.put(player.getUniqueId(), playerData);
            return loadPlayerFile(player);
        }

        return playerData;
    }

    public void savePlayerFile(Player player) {
        PlayerData playerData = loadPlayerFile(player);
        playerFileService.writePlayerFile(player, playerData);
    }

    public void updatePlayerFile(Player player, PlayerData playerData) {
        playerFileService.writePlayerFile(player, playerData);
        cachePlayerData.put(player.getUniqueId(), playerData);
    }

    public void removeCachedPlayerData(Player player) {
        PlayerData playerData = checkForCachePlayerData(player.getUniqueId());
        if (playerData != null) {
            cachePlayerData.remove(player.getUniqueId());
        }
    }

    private void loadPlayer(PlayerData playerData, Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setMaxHealth(playerData.playerStats().getMaxHealth());
        player.setLevel(playerData.playerStats().getLevel());

        loadInventory(playerData, player);
    }

    private void loadInventory(PlayerData playerData, Player player) {
        String[] currentInventorySlots = playerData.playerInventory().inventorySlots();
        for (int i = 0; i < currentInventorySlots.length; i++) {
            String key = currentInventorySlots[i];
            if (key == null || key.isEmpty()) {
                continue;
            }
            Objects.requireNonNull(player.getPlayer()).getInventory().setItem(i, itemService.getItemStack(key));
        }
    }

    private PlayerData checkForCachePlayerData(UUID uuid) {
        if (cachePlayerData.containsKey(uuid)) {
            return cachePlayerData.get(uuid);
        }

        return null;
    }
}
