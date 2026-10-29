package org.thesplitting.src.services;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.thesplitting.src.data.item.CollectableItems.CollectableItemData;
import org.thesplitting.src.data.item.CollectableItems.ItemCategories;
import org.thesplitting.src.data.item.CollectableItems.ItemInventorySlots;
import org.thesplitting.src.data.item.CollectableItems.Items.FireSword;
import org.thesplitting.src.data.item.PlayerItems.InventoryManagerItem;
import org.thesplitting.src.data.item.PlayerItems.StartBookItem;
import org.thesplitting.src.data.item.PlayerItems.StartSwordItem;
import org.thesplitting.src.data.player.PlayerData;
import org.thesplitting.src.services.itemservice.ItemService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final String PAGE_1 = InventoryManagerItem.NAME + " (Page: 1)";
    private static final String PAGE_2 = InventoryManagerItem.NAME + " (Page: 2)";
    private static final String PAGE_3 = InventoryManagerItem.NAME + " (Page: 3)";

    @Mock
    private ItemService itemService;

    @Mock
    private PlayerService playerService;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private Player player;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private PlayerInventory playerInventory;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private InventoryView openView;

    private MockedStatic<Bukkit> bukkit;
    private final Map<String, Inventory> inventories = new HashMap<>();
    private final Map<String, Integer> sizes = new HashMap<>();
    private final Map<String, ItemStack> stacks = new HashMap<>();
    private PlayerData playerData;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        ItemFactory itemFactory = mock(ItemFactory.class, withSettings().strictness(Strictness.LENIENT));
        when(itemFactory.getItemMeta(any())).thenAnswer(invocation -> mock(ItemMeta.class));

        bukkit = mockStatic(Bukkit.class, withSettings().strictness(Strictness.LENIENT));
        bukkit.when(Bukkit::getItemFactory).thenReturn(itemFactory);
        bukkit.when(() -> Bukkit.createInventory(isNull(), anyInt(), anyString())).thenAnswer(invocation -> {
            Inventory inventory = mock(Inventory.class, withSettings().strictness(Strictness.LENIENT));
            inventories.put(invocation.getArgument(2), inventory);
            sizes.put(invocation.getArgument(2), invocation.getArgument(1));
            return inventory;
        });

        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(playerInventory);
        when(player.getOpenInventory()).thenReturn(openView);

        playerData = PlayerData.create("Steve");
        inventoryService = new InventoryService(itemService, playerService);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private void playerDataIsLoaded() {
        when(playerService.loadPlayerFile(player)).thenReturn(playerData);
    }

    private void itemStacksExist() {
        when(itemService.getItemStack(anyString()))
                .thenAnswer(invocation -> stacks.computeIfAbsent(invocation.getArgument(0), id -> mock(ItemStack.class)));
    }

    private ItemStack itemNamed(String displayName) {
        ItemStack itemStack = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(itemStack.getItemMeta()).thenReturn(meta);
        when(meta.getDisplayName()).thenReturn(displayName);
        return itemStack;
    }

    private void verifyManagerReopened() {
        verify(player).openInventory(inventories.get(PAGE_1));
    }

    @Nested
    class InventoryManager {

        @BeforeEach
        void setUp() {
            playerDataIsLoaded();
            itemStacksExist();
        }

        @Test
        void open_showsOwnedItemsOfFirstCategoryAndNavigation() {
            inventoryService.openInventoryManager(player);

            Inventory page = inventories.get(PAGE_1);
            assertEquals(54, sizes.get(PAGE_1));
            verify(page).setItem(0, stacks.get(StartSwordItem.ID));
            verify(page).setItem(eq(45), any(ItemStack.class));
            verify(page).setItem(eq(49), any(ItemStack.class));
            verify(page).setItem(eq(53), any(ItemStack.class));
            verify(itemService, never()).getItemStack(FireSword.ID);
            verify(player).openInventory(page);
        }

        @Test
        void nextPage_showsNextCategory() {
            inventoryService.openInventoryManager(player);
            when(openView.getTitle()).thenReturn(PAGE_1);

            inventoryService.nextInventoryPage(player);

            verify(inventories.get(PAGE_2)).setItem(0, stacks.get(StartBookItem.ID));
        }

        @Test
        void nextPage_onLastPage_staysOnLastPage() {
            inventoryService.openInventoryManager(player);
            when(openView.getTitle()).thenReturn(PAGE_2);

            inventoryService.nextInventoryPage(player);

            assertTrue(inventories.containsKey(PAGE_2));
            assertFalse(inventories.containsKey(PAGE_3));
        }

        @Test
        void previousPage_onFirstPage_staysOnFirstPage() {
            inventoryService.openInventoryManager(player);
            when(openView.getTitle()).thenReturn(PAGE_1);

            inventoryService.lastInventoryPage(player);

            bukkit.verify(() -> Bukkit.createInventory(isNull(), eq(54), eq(PAGE_1)), times(2));
            assertFalse(inventories.containsKey(InventoryManagerItem.NAME + " (Page: 0)"));
        }

        @Test
        void nextPage_withoutCache_opensFirstPage() {
            inventoryService.nextInventoryPage(player);

            assertTrue(inventories.containsKey(PAGE_1));
            verify(player).openInventory(inventories.get(PAGE_1));
        }

        @Test
        void clearPlayerCache_forcesReloadOnNextNavigation() {
            inventoryService.openInventoryManager(player);

            inventoryService.clearPlayerCache(player.getUniqueId());
            inventoryService.nextInventoryPage(player);

            verify(playerService, times(2)).loadPlayerFile(player);
        }

        @Test
        void fullCategory_isSplitAcrossPages() {
            Map<String, CollectableItemData> items = playerData.collectableItems().getCollectableItems();
            for (int i = 0; i < 46; i++) {
                items.put(String.format("melee_%02d", i), new CollectableItemData(ItemCategories.MELEE, 1));
            }

            inventoryService.openInventoryManager(player);
            when(openView.getTitle()).thenReturn(PAGE_1);
            inventoryService.nextInventoryPage(player);

            Inventory firstPage = inventories.get(PAGE_1);
            Inventory secondPage = inventories.get(PAGE_2);
            verify(firstPage).setItem(44, stacks.get("melee_44"));
            verify(firstPage, never()).setItem(anyInt(), eq(stacks.get("melee_45")));
            verify(secondPage).setItem(0, stacks.get("melee_45"));
            verify(secondPage).setItem(1, stacks.get(StartSwordItem.ID));
        }
    }

    @Test
    void openInventoryManager_noOwnedItems_sendsErrorAndOpensNothing() {
        playerData.collectableItems().getCollectableItems().values().forEach(data -> data.setPossession(0));
        playerDataIsLoaded();

        inventoryService.openInventoryManager(player);

        verify(player).sendMessage(endsWith("There are no items in your inventory yet."));
        verify(player, never()).openInventory(any(Inventory.class));
    }

    @Nested
    class ItemSwitcher {

        @Mock(strictness = Mock.Strictness.LENIENT)
        private InventoryClickEvent event;

        @Mock
        private ItemStack chosenItem;

        @BeforeEach
        void setUp() {
            when(event.getClickedInventory()).thenReturn(mock(Inventory.class));
            when(openView.getItem(4)).thenReturn(chosenItem);
        }

        @Test
        void open_showsChosenItemHotbarAndControls() {
            ItemStack sword = mock(ItemStack.class);
            when(playerInventory.getItem(0)).thenReturn(sword);
            when(event.getCurrentItem()).thenReturn(chosenItem);

            inventoryService.openItemSwitcher(event, player);

            Inventory switcher = inventories.get("Item Switcher");
            assertEquals(27, sizes.get("Item Switcher"));
            verify(switcher).setItem(4, chosenItem);
            verify(switcher).setItem(9, sword);
            for (int slot : new int[]{10, 11, 12, 14, 15, 16, 17, 22}) {
                verify(switcher).setItem(eq(slot), any(ItemStack.class));
            }
            verify(player).openInventory(switcher);
        }

        @Test
        void clickOutsideInventory_doesNothing() {
            when(event.getClickedInventory()).thenReturn(null);

            inventoryService.itemSwitcherAction(event, player);

            verifyNoInteractions(playerService);
            verify(player, never()).sendMessage(anyString());
        }

        @Test
        void backButton_reopensInventoryManager() {
            playerDataIsLoaded();
            itemStacksExist();
            when(event.getRawSlot()).thenReturn(22);

            inventoryService.itemSwitcherAction(event, player);

            verifyManagerReopened();
        }

        @Test
        void slotReservedForOtherType_warnsAndChangesNothing() {
            when(event.getRawSlot()).thenReturn(10);
            when(itemService.resolveInventorySlots(chosenItem)).thenReturn(ItemInventorySlots.MELEE);

            inventoryService.itemSwitcherAction(event, player);

            verify(player).sendMessage(endsWith("Inventory slot is reserved for: Bow"));
            verify(playerInventory, never()).setItem(anyInt(), any());
            verify(playerService, never()).updatePlayerFile(any(), any());
        }

        @Test
        void itemAlreadyInTargetSlot_warnsAndReopensManager() {
            playerDataIsLoaded();
            itemStacksExist();
            when(event.getRawSlot()).thenReturn(9);
            when(itemService.resolveInventorySlots(chosenItem)).thenReturn(ItemInventorySlots.MELEE);
            when(playerInventory.getItem(0)).thenReturn(chosenItem);

            inventoryService.itemSwitcherAction(event, player);

            verify(player).sendMessage(endsWith("Can't switch to a weapon that is already in use."));
            verify(playerService, never()).updatePlayerFile(any(), any());
            verifyManagerReopened();
        }

        @Test
        void itemInOtherSlot_warnsAndChangesNothing() {
            when(event.getRawSlot()).thenReturn(9);
            when(itemService.resolveInventorySlots(chosenItem)).thenReturn(ItemInventorySlots.MELEE);
            when(playerInventory.getItem(0)).thenReturn(mock(ItemStack.class));
            when(playerInventory.contains(chosenItem)).thenReturn(true);

            inventoryService.itemSwitcherAction(event, player);

            verify(player).sendMessage(endsWith("This item is already in your inventory."));
            verify(playerInventory, never()).setItem(anyInt(), any());
        }

        @Test
        void validSwitch_equipsItemAndSavesPlayerData() {
            playerDataIsLoaded();
            itemStacksExist();
            ItemStack oldSword = mock(ItemStack.class);
            when(event.getRawSlot()).thenReturn(9);
            when(itemService.resolveInventorySlots(chosenItem)).thenReturn(ItemInventorySlots.MELEE);
            when(itemService.resolveId(chosenItem)).thenReturn(FireSword.ID);
            when(playerInventory.getItem(0)).thenReturn(oldSword);
            when(playerInventory.contains(chosenItem)).thenReturn(false);

            inventoryService.itemSwitcherAction(event, player);

            verify(playerInventory).removeItem(oldSword);
            verify(playerInventory).setItem(0, chosenItem);
            assertEquals(FireSword.ID, playerData.playerInventory().getInventorySlot(0));
            verify(playerService).updatePlayerFile(player, playerData);
            verify(player).sendMessage(endsWith("Item switch successful!"));
            verifyManagerReopened();
        }
    }

    @Nested
    class ItemRemover {

        @Mock(strictness = Mock.Strictness.LENIENT)
        private InventoryClickEvent event;

        @Mock(strictness = Mock.Strictness.LENIENT)
        private Inventory clickedInventory;

        @BeforeEach
        void setUp() {
            when(event.getClickedInventory()).thenReturn(clickedInventory);
            lenient().when(playerService.loadPlayerFile(player)).thenReturn(playerData);
        }

        @Test
        void open_showsHotbarAndControls() {
            ItemStack sword = mock(ItemStack.class);
            when(playerInventory.getItem(0)).thenReturn(sword);

            inventoryService.openItemRemover(player);

            Inventory remover = inventories.get("Item Remover");
            assertEquals(18, sizes.get("Item Remover"));
            verify(remover).setItem(0, sword);
            for (int slot : new int[]{1, 2, 3, 5, 6, 7, 8, 13}) {
                verify(remover).setItem(eq(slot), any(ItemStack.class));
            }
            verify(player).openInventory(remover);
        }

        @Test
        void clickOutsideInventory_doesNothing() {
            when(event.getClickedInventory()).thenReturn(null);

            inventoryService.itemRemoverAction(event, player);

            verify(playerService, never()).updatePlayerFile(any(), any());
            verify(player, never()).sendMessage(anyString());
        }

        @Test
        void backButton_reopensInventoryManager() {
            itemStacksExist();
            when(event.getRawSlot()).thenReturn(13);

            inventoryService.itemRemoverAction(event, player);

            verifyManagerReopened();
        }

        @Test
        void emptySlotPlaceholder_warnsAndRemovesNothing() {
            ItemStack placeholder = itemNamed("Slot: 2 (Reserved for Bow)");
            when(event.getRawSlot()).thenReturn(1);
            when(event.getCurrentItem()).thenReturn(placeholder);

            inventoryService.itemRemoverAction(event, player);

            verify(player).sendMessage(endsWith("You cannot remove an empty slot."));
            verify(playerInventory, never()).remove(any(ItemStack.class));
        }

        @Test
        void inventoryWithManagerItem_isProtected() {
            ItemStack sword = itemNamed("Starter Sword");
            when(event.getRawSlot()).thenReturn(0);
            when(event.getCurrentItem()).thenReturn(sword);
            when(clickedInventory.contains(Material.NETHER_STAR)).thenReturn(true);

            inventoryService.itemRemoverAction(event, player);

            verify(player).sendMessage(endsWith("This is not allowed."));
            verify(playerInventory, never()).remove(any(ItemStack.class));
        }

        @Test
        void equippedItem_isRemovedAndSlotCleared() {
            ItemStack sword = itemNamed("Starter Sword");
            when(event.getRawSlot()).thenReturn(0);
            when(event.getCurrentItem()).thenReturn(sword);
            when(playerInventory.contains(sword)).thenReturn(true);

            inventoryService.itemRemoverAction(event, player);

            verify(playerInventory).remove(sword);
            verify(player).closeInventory();
            assertNull(playerData.playerInventory().getInventorySlot(0));
            verify(playerService).updatePlayerFile(player, playerData);
            verify(player).sendMessage(endsWith("Item successfully removed!"));
        }

        @Test
        void itemNotInPlayerInventory_isIgnored() {
            ItemStack sword = itemNamed("Starter Sword");
            when(event.getRawSlot()).thenReturn(0);
            when(event.getCurrentItem()).thenReturn(sword);
            when(playerInventory.contains(sword)).thenReturn(false);

            inventoryService.itemRemoverAction(event, player);

            verify(playerInventory, never()).remove(any(ItemStack.class));
            verify(playerService, never()).updatePlayerFile(any(), any());
        }
    }
}
