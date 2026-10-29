package org.thesplitting.src.events;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thesplitting.src.misc.errorhandler.PlayerErrorHandler;
import org.thesplitting.src.misc.exceptions.PlayerDataNotFoundException;
import org.thesplitting.src.services.ApiService;
import org.thesplitting.src.services.ChatControlService;
import org.thesplitting.src.services.InventoryService;
import org.thesplitting.src.services.PlayerService;
import org.thesplitting.src.services.ScoreboardService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerListenerTest {

    private static final String JOIN_PATH = "/minecraft/users/join";

    @Mock
    private JavaPlugin plugin;

    @Mock
    private PlayerService playerService;

    @Mock
    private ChatControlService chatControlService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ScoreboardService scoreboardService;

    @Mock
    private PlayerErrorHandler playerErrorHandler;

    @Mock
    private ApiService apiService;

    @Mock
    private Player player;

    private PlayerListener playerListener;

    @BeforeEach
    void setUp() {
        playerListener = new PlayerListener(plugin, playerService, chatControlService, inventoryService, scoreboardService, playerErrorHandler, apiService);
    }

    @Test
    void onEnable_registersListener() {
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);

        playerListener.onEnable();

        verify(pluginManager).registerEvents(playerListener, plugin);
    }

    @Nested
    class OnJoin {

        private final UUID uuid = UUID.randomUUID();

        private PlayerJoinEvent joinEvent() {
            return new PlayerJoinEvent(player, "joined");
        }

        private void playerIdentity() {
            when(player.getUniqueId()).thenReturn(uuid);
            when(player.getName()).thenReturn("Steve");
        }

        @Test
        void setsUpPlayerChatControlAndScoreboard() {
            playerIdentity();
            when(apiService.post(eq(JOIN_PATH), any(), eq(PlayerJoinResponse.class))).thenReturn(new CompletableFuture<>());

            playerListener.onJoin(joinEvent());

            InOrder inOrder = inOrder(playerService, chatControlService, scoreboardService);
            inOrder.verify(playerService).initiatePlayerSetup(player);
            inOrder.verify(chatControlService).loadChatControlFile(player);
            inOrder.verify(scoreboardService).updatePlayerTeam(player);
        }

        @Test
        void reportsJoinToApiWithUuidAndName() {
            playerIdentity();
            ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
            when(apiService.post(eq(JOIN_PATH), body.capture(), eq(PlayerJoinResponse.class))).thenReturn(new CompletableFuture<>());

            playerListener.onJoin(joinEvent());

            assertEquals(new PlayerJoinRequest(uuid.toString(), "Steve"), body.getValue());
        }

        @Test
        void successfulApiResponse_sendsWelcomeMessageOnMainThread() {
            playerIdentity();
            PlayerJoinResponse response = new PlayerJoinResponse(uuid.toString(), "Steve", "now", "now");
            when(apiService.post(eq(JOIN_PATH), any(), eq(PlayerJoinResponse.class))).thenReturn(CompletableFuture.completedFuture(response));
            ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);

            playerListener.onJoin(joinEvent());

            verify(apiService).runSync(task.capture());
            verify(player, never()).sendMessage(any(String.class));
            task.getValue().run();
            verify(player).sendMessage(endsWith("Successfully joined the server!"));
        }

        @Test
        void failedApiRequest_isLoggedAndPlayerStays() {
            playerIdentity();
            Logger logger = mock(Logger.class);
            when(plugin.getLogger()).thenReturn(logger);
            when(apiService.post(eq(JOIN_PATH), any(), eq(PlayerJoinResponse.class)))
                    .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("API 503: down")));

            playerListener.onJoin(joinEvent());

            verify(logger).log(eq(Level.SEVERE), eq("Failed to join the server!"), any(Throwable.class));
            verify(apiService, never()).runSync(any());
            verifyNoInteractions(playerErrorHandler);
            verify(player, never()).kickPlayer(any());
        }

        @Test
        void failingSetup_kicksPlayerAndThrows() {
            when(player.getName()).thenReturn("Steve");
            doThrow(new RuntimeException("broken file")).when(playerService).initiatePlayerSetup(player);

            assertThrows(PlayerDataNotFoundException.class, () -> playerListener.onJoin(joinEvent()));

            verify(playerErrorHandler).playerDataNotFoundError(player);
            verifyNoInteractions(apiService);
        }
    }

    @Test
    void onLeave_savesBeforeClearingCaches() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        playerListener.onLeave(new PlayerQuitEvent(player, "left"));

        verify(inventoryService).clearPlayerCache(uuid);
        InOrder inOrder = inOrder(playerService);
        inOrder.verify(playerService).savePlayerFile(player);
        inOrder.verify(playerService).removeCachedPlayerData(player);
    }

    @Test
    void onDeath_removesMessageExperienceAndDrops() {
        List<ItemStack> drops = new ArrayList<>(List.of(mock(ItemStack.class)));
        PlayerDeathEvent event = new PlayerDeathEvent(player, drops, 5, "Steve died");

        playerListener.onDeath(event);

        assertNull(event.getDeathMessage());
        assertEquals(0, event.getDroppedExp());
        assertTrue(event.getDrops().isEmpty());
    }

    @Test
    void onRespawn_setsUpPlayerOnNextTick() {
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        PlayerRespawnEvent event = mock(PlayerRespawnEvent.class);
        when(event.getPlayer()).thenReturn(player);
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            playerListener.onRespawn(event);
        }

        verify(scheduler).runTask(eq(plugin), task.capture());
        verify(playerService, never()).initiatePlayerSetup(player);
        task.getValue().run();
        verify(playerService).initiatePlayerSetup(player);
    }

    @Test
    void onInventoryClick_isCancelled() {
        InventoryClickEvent event = mock(InventoryClickEvent.class);

        playerListener.onInventoryClick(event);

        verify(event).setCancelled(true);
    }

    @Test
    void onPlayerDropItem_isCancelled() {
        PlayerDropItemEvent event = mock(PlayerDropItemEvent.class);

        playerListener.onPlayerDropItem(event);

        verify(event).setCancelled(true);
    }

    @Test
    void onChatMessage_delegatesToChatControl() {
        PlayerChatEvent event = new PlayerChatEvent(player, "hello", "<%1$s> %2$s", new HashSet<>());

        playerListener.onChatMessage(event);

        verify(chatControlService).takeOverChatListener(event);
    }
}
