package org.thesplitting.src.services.itemservice;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.thesplitting.src.data.contracts.ICollectableItem;
import org.thesplitting.src.data.item.CollectableItems.ItemInventorySlots;
import org.thesplitting.src.data.item.CollectableItems.Items.FireSword;
import org.thesplitting.src.data.contracts.IItem;
import org.thesplitting.src.data.item.PlayerItems.InventoryManagerItem;
import org.thesplitting.src.data.item.PlayerItems.StartBookItem;
import org.thesplitting.src.data.item.PlayerItems.StartSwordItem;
import org.thesplitting.src.misc.annotations.Nullable;
import org.thesplitting.src.core.contracts.IService;
import org.thesplitting.src.core.ServiceRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ItemService implements IService {
    private final ServiceRegistry registry;
    private NamespacedKey idKey;
    private final static List<IItem> ITEM_INSTANCES = List.of(
            new StartBookItem(),
            new StartSwordItem(),
            new InventoryManagerItem(),

            new FireSword()
    );
    private final Map<String, IItem> items = new HashMap<>();

    public ItemService(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onEnable() {
        idKey = new NamespacedKey(registry.getPlugin(), "item_id");
        ITEM_INSTANCES.forEach(item -> items.put(item.getId(), item));
    }

    @Override
    public void onDisable() {

    }

    public static List<ICollectableItem> getItems() {
        return ITEM_INSTANCES.stream()
                .filter(ICollectableItem.class::isInstance)
                .map(ICollectableItem.class::cast)
                .toList();
    }

    public ItemStack getItemStack(String id) {
        IItem item = items.get(id);
        if (item == null) return null;

        ItemStack stack = item.getItemStack();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public String resolveId(ItemStack stack) {
        if (idKey == null) return null;
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public IItem getItem(String id) {
        if (id == null) return null;
        return items.get(id);
    }

   @Nullable
   public ICollectableItem getCollectableItem(String id) {
        return getItem(id) instanceof ICollectableItem collectableItem ? collectableItem : null;
   }

   public ItemInventorySlots resolveInventorySlots(ItemStack stack) {
        ICollectableItem item = getCollectableItem(resolveId(stack));
        return item == null ? ItemInventorySlots.NONE : item.getInventorySlot();
   }
}
