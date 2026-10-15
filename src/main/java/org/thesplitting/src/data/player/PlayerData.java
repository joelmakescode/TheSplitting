package org.thesplitting.src.data.player;

import org.thesplitting.src.data.item.CollectableItems.CollectableItems;
import org.thesplitting.src.data.item.PlayerItems.InventoryManagerItem;
import org.thesplitting.src.data.item.PlayerItems.StartBookItem;
import org.thesplitting.src.data.item.PlayerItems.StartSwordItem;
import org.thesplitting.src.data.player.PlayerRoles.PlayerRole;
import org.thesplitting.src.data.player.PlayerRoles.PlayerRoles;

import java.util.HashMap;

import static org.thesplitting.src.services.itemservice.ItemService.getItems;

public record PlayerData(String name, PlayerRole playerRole, PlayerSettings playerSettings, PlayerStats playerStats, PlayerInventory playerInventory, CollectableItems collectableItems) {

    public static PlayerData create(String playerName) {
        return new PlayerData(
                playerName,
                new PlayerRole(PlayerRoles.NOVICE.toString()),
                new PlayerSettings(1),
                new PlayerStats(
                        4,
                        1,
                        1,
                        1,
                        1,
                        1
                ),
                new PlayerInventory(new String[]{
                        StartSwordItem.ID,
                        null,
                        null,
                        null,
                        InventoryManagerItem.ID,
                        null,
                        null,
                        null,
                        StartBookItem.ID
                }),
                createDefaultCollectableItems()
        );
    }

    private static CollectableItems createDefaultCollectableItems() {
        CollectableItems collectableItems = new CollectableItems(new HashMap<>());
        getItems().forEach(collectableItems::addCollectableItem);
        return collectableItems;
    }
}
