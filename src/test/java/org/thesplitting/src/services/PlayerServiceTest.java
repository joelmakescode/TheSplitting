package org.thesplitting.src.services;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thesplitting.src.data.item.PlayerItems.InventoryManagerItem;
import org.thesplitting.src.data.item.PlayerItems.StartBookItem;
import org.thesplitting.src.data.item.PlayerItems.StartSwordItem;
import org.thesplitting.src.data.player.PlayerData;
import org.thesplitting.src.services.fileservice.PlayerFileService;
import org.thesplitting.src.services.itemservice.ItemService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerFileService playerFileService;

    @Mock
    private ItemService itemService;

    @Mock
    private Player player;

    private PlayerService playerService;
    private PlayerData playerData;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerFileService, itemService);
        playerData = PlayerData.create("Steve");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    @Nested
    class LoadPlayerFile {

        @Test
        void existingFile_readsFileOnlyOnce() {
            when(playerFileService.readPlayerFile(player)).thenReturn(playerData);

            PlayerData first = playerService.loadPlayerFile(player);
            PlayerData second = playerService.loadPlayerFile(player);

            assertSame(playerData, first);
            assertSame(playerData, second);
            verify(playerFileService, times(1)).readPlayerFile(player);
            verify(playerFileService, never()).createPlayerFile(player);
        }

        @Test
        void missingFile_createsFileAndReturnsNewData() {
            when(playerFileService.readPlayerFile(player)).thenReturn(null, playerData);

            PlayerData result = playerService.loadPlayerFile(player);

            assertSame(playerData, result);
            verify(playerFileService).createPlayerFile(player);
        }

        @Test
        void afterRemovingCache_readsFileAgain() {
            when(playerFileService.readPlayerFile(player)).thenReturn(playerData);
            playerService.loadPlayerFile(player);

            playerService.removeCachedPlayerData(player);
            playerService.loadPlayerFile(player);

            verify(playerFileService, times(2)).readPlayerFile(player);
        }
    }

    @Nested
    class InitiatePlayerSetup {

        @Mock
        private PlayerInventory inventory;

        @BeforeEach
        void setUp() {
            when(player.getPlayer()).thenReturn(player);
            when(player.getInventory()).thenReturn(inventory);
            when(playerFileService.readPlayerFile(player)).thenReturn(playerData);
        }

        @Test
        void appliesStatsAndSurvivalMode() {
            playerService.initiatePlayerSetup(player);

            verify(player).setGameMode(GameMode.SURVIVAL);
            verify(player).setMaxHealth(4);
            verify(player).setLevel(1);
        }

        @Test
        void placesEveryStoredItemInItsSlot() {
            ItemStack sword = mock(ItemStack.class);
            ItemStack manager = mock(ItemStack.class);
            ItemStack book = mock(ItemStack.class);
            when(itemService.getItemStack(StartSwordItem.ID)).thenReturn(sword);
            when(itemService.getItemStack(InventoryManagerItem.ID)).thenReturn(manager);
            when(itemService.getItemStack(StartBookItem.ID)).thenReturn(book);

            playerService.initiatePlayerSetup(player);

            verify(inventory).setItem(0, sword);
            verify(inventory).setItem(4, manager);
            verify(inventory).setItem(8, book);
            verify(inventory, times(3)).setItem(anyInt(), any());
        }

        @Test
        void emptySlots_areSkipped() {
            playerData.playerInventory().setInventorySlot(0, "");

            playerService.initiatePlayerSetup(player);

            verify(itemService, never()).getItemStack("");
            verify(itemService, times(2)).getItemStack(anyString());
        }

        @Test
        void starterFlag_isClearedAndSaved() {
            playerService.initiatePlayerSetup(player);

            assertEquals(0, playerData.playerSettings().getIsStarter());
            verify(playerFileService).writePlayerFile(player, playerData);
        }

        @Test
        void cachesPlayerData() {
            playerService.initiatePlayerSetup(player);
            playerService.initiatePlayerSetup(player);

            verify(playerFileService, times(1)).readPlayerFile(player);
        }
    }

    @Test
    void savePlayerFile_writesCachedData() {
        when(playerFileService.readPlayerFile(player)).thenReturn(playerData);
        playerService.loadPlayerFile(player);

        playerService.savePlayerFile(player);

        verify(playerFileService).writePlayerFile(player, playerData);
    }

    @Test
    void updatePlayerFile_writesAndCachesNewData() {
        playerService.updatePlayerFile(player, playerData);

        assertSame(playerData, playerService.loadPlayerFile(player));
        verify(playerFileService).writePlayerFile(player, playerData);
        verify(playerFileService, never()).readPlayerFile(player);
    }
}
