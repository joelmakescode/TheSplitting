package org.thesplitting.src.data.item.CollectableItems;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thesplitting.src.data.contracts.ICollectableItem;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class CollectableItemsTest {

    private record FakeItem(String id, ItemCategories category, int defaultValue) implements ICollectableItem {

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return id;
        }

        @Override
        public ItemStack getItemStack() {
            return null;
        }

        @Override
        public ItemCategories getItemCategory() {
            return category;
        }

        @Override
        public int getDefaultValue() {
            return defaultValue;
        }
    }

    private CollectableItems collectableItems;

    @BeforeEach
    void setup() {
        collectableItems = new CollectableItems(new HashMap<>());
    }

    @Test
    void addCollectableItem_newItem_storesCategoryAndDefaultValue() {
        FakeItem sword = new FakeItem("test_sword", ItemCategories.MELEE, 1);
        collectableItems.addCollectableItem(sword);

        CollectableItemData data = collectableItems.getCollectableItems().get("test_sword");
        assertNotNull(data);
        assertEquals(ItemCategories.MELEE, data.getCategory());
        assertEquals(1, data.getPossession());
    }

    @Test
    void addCollectableItem_itemWithoutOverrides_usesInterfaceDefaults() {
        ICollectableItem minimal = new ICollectableItem() {
            @Override
            public String getId() {
                return "minimal";
            }

            @Override
            public String getName() {
                return "Minimal";
            }

            @Override
            public ItemStack getItemStack() {
                return null;
            }
        };

        collectableItems.addCollectableItem(minimal);

        CollectableItemData data = collectableItems.getCollectableItems().get("minimal");
        assertNotNull(data);
        assertEquals(ItemCategories.MISCELLANEOUS, data.getCategory());
        assertEquals(0, data.getPossession());
    }

    @Test
    void addCollectableItem_newItem_throwsNullPointerException() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> collectableItems.addCollectableItem(null)
        );

        assertEquals("collectableItem is null", exception.getMessage());
    }

    @Test
    void addCollectableItem_existingId_resetPossessionToDefault() {
        FakeItem sword = new FakeItem("test_sword", ItemCategories.MELEE, 0);
        collectableItems.addCollectableItem(sword);
        collectableItems.setPossession("test_sword", 1);

        collectableItems.addCollectableItem(sword);

        assertEquals(0, collectableItems.getPossession("test_sword"));
    }

    @Test
    void getPossession_unknownId_returnsZero() {
        assertEquals(0, collectableItems.getPossession("test_sword"));
    }

    @Test
    void setPossession_knownId_updatesPossession() {
        FakeItem sword = new FakeItem("test_sword", ItemCategories.MELEE, 0);
        collectableItems.addCollectableItem(sword);
        CollectableItemData data = collectableItems.getCollectableItems().get("test_sword");

        collectableItems.setPossession("test_sword", 1);

        assertNotNull(data);
        assertEquals(1, data.getPossession());
    }
}
