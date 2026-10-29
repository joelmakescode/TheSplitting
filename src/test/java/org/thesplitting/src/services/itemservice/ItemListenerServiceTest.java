package org.thesplitting.src.services.itemservice;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thesplitting.src.data.item.PlayerItems.InventoryManagerItem;
import org.thesplitting.src.services.InventoryService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemListenerServiceTest {

    private static final String MANAGER_TITLE = InventoryManagerItem.NAME + " (Page: 1)";

    @Mock
    private InventoryService inventoryService;

    @Mock
    private Player player;

    private ItemListenerService itemListenerService;

    @BeforeEach
    void setUp() {
        itemListenerService = new ItemListenerService(inventoryService);
    }

    @Nested
    class RightClick {

        @Mock
        private ItemStack itemStack;

        @Mock
        private ItemMeta meta;

        @BeforeEach
        void setUp() {
            when(itemStack.getItemMeta()).thenReturn(meta);
        }

        @Test
        void inventoryManagerItem_opensInventoryManager() {
            when(meta.getDisplayName()).thenReturn(InventoryManagerItem.DISPLAY_NAME);

            itemListenerService.TakeOverItemRightClickTask(player, itemStack);

            verify(inventoryService).openInventoryManager(player);
        }

        @Test
        void otherItem_doesNothing() {
            when(meta.getDisplayName()).thenReturn("Fire Sword");

            itemListenerService.TakeOverItemRightClickTask(player, itemStack);

            verifyNoInteractions(inventoryService);
        }
    }

    @Nested
    class InventoryClick {

        @Mock
        private InventoryClickEvent event;

        @BeforeEach
        void setUp() {
            when(event.getWhoClicked()).thenReturn(player);
        }

        @Test
        void managerNextButton_opensNextPage() {
            when(event.getRawSlot()).thenReturn(53);

            itemListenerService.TakeOverInventoryClickTask(event, MANAGER_TITLE);

            verify(inventoryService).nextInventoryPage(player);
            verifyNoMoreInteractions(inventoryService);
        }

        @Test
        void managerRemoverButton_opensItemRemover() {
            when(event.getRawSlot()).thenReturn(49);

            itemListenerService.TakeOverInventoryClickTask(event, MANAGER_TITLE);

            verify(inventoryService).openItemRemover(player);
            verifyNoMoreInteractions(inventoryService);
        }

        @Test
        void managerBackButton_opensPreviousPage() {
            when(event.getRawSlot()).thenReturn(45);

            itemListenerService.TakeOverInventoryClickTask(event, MANAGER_TITLE);

            verify(inventoryService).lastInventoryPage(player);
            verifyNoMoreInteractions(inventoryService);
        }

        @Test
        void managerLeftClickOnItem_opensItemSwitcher() {
            when(event.getRawSlot()).thenReturn(10);
            when(event.getClick()).thenReturn(ClickType.LEFT);
            when(event.getCurrentItem()).thenReturn(mock(ItemStack.class));

            itemListenerService.TakeOverInventoryClickTask(event, MANAGER_TITLE);

            verify(inventoryService).openItemSwitcher(event, player);
        }

        @Test
        void managerLeftClickOnEmptySlot_doesNothing() {
            when(event.getRawSlot()).thenReturn(10);
            when(event.getClick()).thenReturn(ClickType.LEFT);
            when(event.getCurrentItem()).thenReturn(null);

            itemListenerService.TakeOverInventoryClickTask(event, MANAGER_TITLE);

            verifyNoInteractions(inventoryService);
        }

        @Test
        void managerRightClickOnItem_doesNothing() {
            when(event.getRawSlot()).thenReturn(10);
            when(event.getClick()).thenReturn(ClickType.RIGHT);

            itemListenerService.TakeOverInventoryClickTask(event, MANAGER_TITLE);

            verifyNoInteractions(inventoryService);
        }

        @ParameterizedTest
        @ValueSource(ints = {46, 50, 52})
        void managerClickBelowItemArea_doesNothing(int rawSlot) {
            when(event.getRawSlot()).thenReturn(rawSlot);
            when(event.getClick()).thenReturn(ClickType.LEFT);

            itemListenerService.TakeOverInventoryClickTask(event, MANAGER_TITLE);

            verifyNoInteractions(inventoryService);
        }

        @Test
        void itemSwitcherTitle_delegatesToSwitcherAction() {
            itemListenerService.TakeOverInventoryClickTask(event, "Item Switcher");

            verify(inventoryService).itemSwitcherAction(event, player);
            verifyNoMoreInteractions(inventoryService);
        }

        @Test
        void itemRemoverTitle_delegatesToRemoverAction() {
            itemListenerService.TakeOverInventoryClickTask(event, "Item Remover");

            verify(inventoryService).itemRemoverAction(event, player);
            verifyNoMoreInteractions(inventoryService);
        }

        @Test
        void unrelatedTitle_doesNothing() {
            itemListenerService.TakeOverInventoryClickTask(event, "Chest");

            verifyNoInteractions(inventoryService);
        }
    }
}
