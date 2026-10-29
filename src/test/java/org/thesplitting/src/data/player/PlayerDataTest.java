package org.thesplitting.src.data.player;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.thesplitting.src.data.contracts.ICollectableItem;
import org.thesplitting.src.data.item.CollectableItems.CollectableItemData;
import org.thesplitting.src.data.item.CollectableItems.Items.FireSword;
import org.thesplitting.src.data.item.PlayerItems.InventoryManagerItem;
import org.thesplitting.src.data.item.PlayerItems.StartBookItem;
import org.thesplitting.src.data.item.PlayerItems.StartSwordItem;
import org.thesplitting.src.services.itemservice.ItemService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataTest {

    @Test
    void create_newPlayer_hasStarterProfile() {
        PlayerData playerData = PlayerData.create("test_player");

        assertAll(
                () -> assertEquals("test_player", playerData.name()),
                () -> assertEquals("Novice", playerData.playerRole().getPlayerRole()),
                () -> assertEquals(1, playerData.playerSettings().getIsStarter())
        );
    }

    @Test
    void create_newPlayer_hasStarterStats() {
        PlayerData playerData = PlayerData.create("test_player");

        assertAll(
                () -> assertEquals(4, playerData.playerStats().getMaxHealth()),
                () -> assertEquals(1, playerData.playerStats().getLevel()),
                () -> assertEquals(1, playerData.playerStats().getBolt()),
                () -> assertEquals(1, playerData.playerStats().getInferno()),
                () -> assertEquals(1, playerData.playerStats().getGreen_flame()),
                () -> assertEquals(1, playerData.playerStats().getWater_flow())
        );
    }

    @Test
    void create_newPlayer_hasStarterInventory() {
        PlayerData playerData = PlayerData.create("test_player");

        assertAll(
                () -> assertEquals(StartSwordItem.ID, playerData.playerInventory().getInventorySlot(0)),
                () -> assertEquals(InventoryManagerItem.ID, playerData.playerInventory().getInventorySlot(4)),
                () -> assertEquals(StartBookItem.ID, playerData.playerInventory().getInventorySlot(8))
        );
    }

    @Test
    void create_newPlayer_registerEveryItemWithDefaults() {
        Map<String, CollectableItemData> collectableItems = PlayerData.create("test_player").collectableItems().getCollectableItems();
        List<ICollectableItem> exceptedItems = ItemService.getItems();

        assertEquals(exceptedItems.size(), collectableItems.size());

        for (ICollectableItem collectableItem : exceptedItems) {
            CollectableItemData collectableItemData = collectableItems.get(collectableItem.getId());

            assertNotNull(collectableItemData, "Missing collectable item: " + collectableItem.getId());
            assertEquals(collectableItem.getItemCategory(), collectableItemData.getCategory(), "Wrong item category for: " + collectableItem.getId());
            assertEquals(collectableItem.getDefaultValue(), collectableItemData.getPossession(), "Wrong possession value for: " + collectableItem.getId());
        }
    }

    @Test
    void create_newPlayer_everyPossessionIsZeroOrOne() {
        Map<String, CollectableItemData> collectableItems = PlayerData.create("test_player").collectableItems().getCollectableItems();

        collectableItems.forEach((id, data) -> {
            int possession = data.getPossession();
            assertTrue(possession == 0 || possession == 1, "Invalid possession: " + possession + "for: " + id);
        });
    }

    @Test
    void create_twoPlayer_doNotShareState() {
        PlayerData playerData = PlayerData.create("test_player");
        PlayerData playerData2 = PlayerData.create("test_player2");

        playerData.playerInventory().setInventorySlot(1, FireSword.ID);
        playerData.collectableItems().setPossession(FireSword.ID, 1);
        playerData.playerStats().setLevel(10);

        assertAll(
                () -> assertNull(playerData2.playerInventory().getInventorySlot(1)),
                () -> assertEquals(0, playerData2.collectableItems().getPossession(FireSword.ID)),
                () -> assertEquals(1, playerData2.playerStats().getLevel())
        );
    }
}
