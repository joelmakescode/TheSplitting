package org.thesplitting.src.services.itemservice;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thesplitting.src.core.ServiceRegistry;
import org.thesplitting.src.data.contracts.ICollectableItem;
import org.thesplitting.src.data.item.CollectableItems.ItemInventorySlots;
import org.thesplitting.src.data.item.CollectableItems.Items.FireSword;
import org.thesplitting.src.data.item.PlayerItems.InventoryManagerItem;
import org.thesplitting.src.data.item.PlayerItems.StartBookItem;
import org.thesplitting.src.data.item.PlayerItems.StartSwordItem;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private JavaPlugin plugin;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(new ServiceRegistry(plugin));
    }

    @Test
    void getItems_returnsOnlyCollectableItems() {
        Set<String> ids = ItemService.getItems().stream()
                .map(ICollectableItem::getId)
                .collect(Collectors.toSet());

        assertEquals(Set.of(StartBookItem.ID, StartSwordItem.ID, FireSword.ID), ids);
    }

    @Test
    void getItem_beforeEnable_returnsNull() {
        assertNull(itemService.getItem(FireSword.ID));
    }

    @Test
    void resolveId_beforeEnable_returnsNull() {
        assertNull(itemService.resolveId(mock(ItemStack.class)));
    }

    @Nested
    class Enabled {

        @Mock
        private ItemStack stack;

        @Mock
        private BookMeta meta;

        @Mock
        private PersistentDataContainer container;

        @BeforeEach
        void setUp() {
            when(plugin.getName()).thenReturn("TheSplitting");
            itemService.onEnable();
        }

        private void stackWithId(String id) {
            when(stack.hasItemMeta()).thenReturn(true);
            when(stack.getItemMeta()).thenReturn(meta);
            when(meta.getPersistentDataContainer()).thenReturn(container);
            when(container.get(argThat(isItemIdKey()), eq(PersistentDataType.STRING))).thenReturn(id);
        }

        private org.mockito.ArgumentMatcher<NamespacedKey> isItemIdKey() {
            return key -> key != null && key.toString().equals("thesplitting:item_id");
        }

        @Test
        void getItem_registeredId_returnsItem() {
            assertInstanceOf(FireSword.class, itemService.getItem(FireSword.ID));
        }

        @Test
        void getItem_nullOrUnknownId_returnsNull() {
            assertAll(
                    () -> assertNull(itemService.getItem(null)),
                    () -> assertNull(itemService.getItem("unknown"))
            );
        }

        @Test
        void getCollectableItem_nonCollectableItem_returnsNull() {
            assertNull(itemService.getCollectableItem(InventoryManagerItem.ID));
        }

        @Test
        void getCollectableItem_collectableItem_returnsItem() {
            assertInstanceOf(StartSwordItem.class, itemService.getCollectableItem(StartSwordItem.ID));
        }

        @Test
        void getItemStack_unknownId_returnsNull() {
            assertNull(itemService.getItemStack("unknown"));
        }

        @Test
        void getItemStack_knownId_tagsStackWithItemId() {
            ItemFactory itemFactory = mock(ItemFactory.class);
            when(itemFactory.getItemMeta(any())).thenReturn(meta);
            when(meta.getPersistentDataContainer()).thenReturn(container);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getItemFactory).thenReturn(itemFactory);

                assertNotNull(itemService.getItemStack(FireSword.ID));
            }

            verify(container).set(argThat(isItemIdKey()), eq(PersistentDataType.STRING), eq(FireSword.ID));
        }

        @Test
        void resolveId_nullStack_returnsNull() {
            assertNull(itemService.resolveId(null));
        }

        @Test
        void resolveId_stackWithoutMeta_returnsNull() {
            when(stack.hasItemMeta()).thenReturn(false);

            assertNull(itemService.resolveId(stack));
        }

        @Test
        void resolveId_taggedStack_returnsStoredId() {
            stackWithId(FireSword.ID);

            assertEquals(FireSword.ID, itemService.resolveId(stack));
        }

        @Test
        void resolveInventorySlots_collectableItem_returnsItemSlot() {
            stackWithId(StartSwordItem.ID);

            assertEquals(ItemInventorySlots.MELEE, itemService.resolveInventorySlots(stack));
        }

        @Test
        void resolveInventorySlots_nonCollectableItem_returnsNone() {
            stackWithId(InventoryManagerItem.ID);

            assertEquals(ItemInventorySlots.NONE, itemService.resolveInventorySlots(stack));
        }

        @Test
        void resolveInventorySlots_untaggedStack_returnsNone() {
            when(stack.hasItemMeta()).thenReturn(false);

            assertEquals(ItemInventorySlots.NONE, itemService.resolveInventorySlots(stack));
        }
    }
}
