package org.thesplitting.src.services.itemservice;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.thesplitting.src.data.item.PlayerItems.InventoryManagerItem;
import org.thesplitting.src.core.contracts.IService;
import org.thesplitting.src.services.InventoryService;

import java.util.Objects;

public record ItemListenerService(InventoryService inventoryService) implements IService {

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    public void TakeOverItemRightClickTask(Player player, ItemStack itemStack) {
        if (Objects.requireNonNull(itemStack.getItemMeta()).getDisplayName().equals(InventoryManagerItem.DISPLAY_NAME)) {
            inventoryService.openInventoryManager(player);
        }
    }

    public void TakeOverInventoryClickTask(InventoryClickEvent event, String eventTitle) {
        Player player = (Player) event.getWhoClicked();

        if (eventTitle.startsWith(InventoryManagerItem.NAME)) {
            if (event.getRawSlot() == 53) {
                inventoryService.nextInventoryPage(player);
            } else if (event.getRawSlot() == 49) {
                inventoryService.openItemRemover(player);
            } else if (event.getRawSlot() == 45) {
                inventoryService.lastInventoryPage(player);
            } else if (event.getClick().isLeftClick() && event.getRawSlot() < 45 && event.getCurrentItem() != null){
                inventoryService.openItemSwitcher(event, player);
            }
        } else if (eventTitle.equals("Item Switcher")) {
            inventoryService.itemSwitcherAction(event, player);
        } else if (eventTitle.equals("Item Remover")) {
            inventoryService.itemRemoverAction(event, player);
        }
    }
}
