package org.thesplitting.src.data.item.CollectableItems;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemInventorySlotsTest {

    @ParameterizedTest(name = "slot {0} -> {1}")
    @CsvSource({
            "0, MELEE",
            "1, BOW",
            "2, POTION1",
            "3, POTION2"
    })
    void fromSlot_reservedSlot_returnsMatchingRestriction(int slot, ItemInventorySlots expected) {
        assertEquals(expected, ItemInventorySlots.fromSlot(slot));
    }

    @ParameterizedTest(name = "slot {0} -> null")
    @ValueSource(ints = {4, 8, Integer.MAX_VALUE})
    void fromSlot_unreservedSlot_returnsNull(int slot) {
        assertNull(ItemInventorySlots.fromSlot(slot));
    }

    @ParameterizedTest(name = "slot {0} -> null")
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void fromSlot_negativeSlot_returnsNull(int slot) {
        assertNull(ItemInventorySlots.fromSlot(slot));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = ItemInventorySlots.class, mode = EnumSource.Mode.EXCLUDE, names = "NONE")
    void fromSlot_everyRestriction_resolvesBackToItself(ItemInventorySlots restriction) {
        assertEquals(restriction, ItemInventorySlots.fromSlot(restriction.getInventorySlot()));
    }

}
