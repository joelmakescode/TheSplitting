package org.thesplitting.src.events;

import org.bukkit.Server;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thesplitting.src.data.item.PlayerItems.InventoryManagerItem;
import org.thesplitting.src.services.itemservice.ItemListenerService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemListenerTest {

    @Mock
    private JavaPlugin plugin;

    @Mock
    private ItemListenerService itemListenerService;

    @Mock
    private Player player;

    private ItemListener itemListener;

    @BeforeEach
    void setUp() {
        itemListener = new ItemListener(plugin, itemListenerService);
    }

    @Test
    void onEnable_registersListener() {
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);

        itemListener.onEnable();

        verify(pluginManager).registerEvents(itemListener, plugin);
    }

    @Nested
    class RightClick {

        @Mock
        private PlayerInteractEvent event;

        @Mock
        private ItemStack item;

        @ParameterizedTest
        @EnumSource(value = Action.class, names = {"RIGHT_CLICK_AIR", "RIGHT_CLICK_BLOCK"})
        void rightClickWithItem_delegatesToService(Action action) {
            when(event.getAction()).thenReturn(action);
            when(event.getPlayer()).thenReturn(player);
            when(event.getItem()).thenReturn(item);
            when(item.hasItemMeta()).thenReturn(true);

            itemListener.onItemRightClick(event);

            verify(itemListenerService).TakeOverItemRightClickTask(player, item);
        }

        @ParameterizedTest
        @EnumSource(value = Action.class, names = {"LEFT_CLICK_AIR", "LEFT_CLICK_BLOCK", "PHYSICAL"})
        void otherActions_areIgnored(Action action) {
            when(event.getAction()).thenReturn(action);

            itemListener.onItemRightClick(event);

            verifyNoInteractions(itemListenerService);
        }

        @Test
        void emptyHand_isIgnored() {
            when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
            when(event.getItem()).thenReturn(null);

            itemListener.onItemRightClick(event);

            verifyNoInteractions(itemListenerService);
        }

        @Test
        void itemWithoutMeta_isIgnored() {
            when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
            when(event.getItem()).thenReturn(item);
            when(item.hasItemMeta()).thenReturn(false);

            itemListener.onItemRightClick(event);

            verifyNoInteractions(itemListenerService);
        }
    }

    @Nested
    class InventoryClick {

        @Mock
        private InventoryClickEvent event;

        @Mock
        private InventoryView view;

        @BeforeEach
        void setUp() {
            when(event.getView()).thenReturn(view);
        }

        @ParameterizedTest
        @ValueSource(strings = {InventoryManagerItem.NAME + " (Page: 2)", "Item Switcher", "Item Remover"})
        void pluginInventory_delegatesToService(String title) {
            when(view.getTitle()).thenReturn(title);
            when(event.getWhoClicked()).thenReturn(player);

            itemListener.onInventoryClick(event);

            verify(itemListenerService).TakeOverInventoryClickTask(event, title);
        }

        @Test
        void foreignInventory_isIgnored() {
            when(view.getTitle()).thenReturn("Chest");
            when(event.getWhoClicked()).thenReturn(player);

            itemListener.onInventoryClick(event);

            verifyNoInteractions(itemListenerService);
        }

        @Test
        void clickByNonPlayer_isIgnored() {
            when(view.getTitle()).thenReturn("Item Switcher");
            when(event.getWhoClicked()).thenReturn(mock(HumanEntity.class));

            itemListener.onInventoryClick(event);

            verify(itemListenerService, never()).TakeOverInventoryClickTask(any(), any());
        }
    }
}
