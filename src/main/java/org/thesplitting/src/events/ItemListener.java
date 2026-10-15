package org.thesplitting.src.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesplitting.src.data.item.PlayerItems.InventoryManagerItem;
import org.thesplitting.src.core.contracts.IService;
import org.thesplitting.src.services.itemservice.ItemListenerService;

public record ItemListener(JavaPlugin plugin, ItemListenerService itemListenerManager) implements Listener, IService {
    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {

    }

    @EventHandler
    public void onItemRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) { return; }
        if (!item.hasItemMeta()) { return; }

        itemListenerManager.TakeOverItemRightClickTask(player, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String eventTitle = event.getView().getTitle();

        if (!(event.getWhoClicked() instanceof Player)) { return; }
        if (!eventTitle.startsWith(InventoryManagerItem.NAME) && !eventTitle.equals("Item Switcher") && !eventTitle.equals("Item Remover")) { return; }

        itemListenerManager.TakeOverInventoryClickTask(event, eventTitle);
    }
}
