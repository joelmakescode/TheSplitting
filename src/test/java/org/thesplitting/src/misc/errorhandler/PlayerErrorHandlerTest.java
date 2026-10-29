package org.thesplitting.src.misc.errorhandler;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerErrorHandlerTest {

    @Mock
    private JavaPlugin plugin;

    @Mock
    private Logger logger;

    @Mock
    private Player player;

    private PlayerErrorHandler playerErrorHandler;

    @BeforeEach
    void setUp() {
        when(plugin.getLogger()).thenReturn(logger);
        playerErrorHandler = new PlayerErrorHandler(plugin);
    }

    @Test
    void playerDataNotFoundError_kicksPlayerWithRedReason() {
        playerErrorHandler.playerDataNotFoundError(player);

        verify(player).kickPlayer(ChatColor.RED + "Your Player Data could not be loaded. Please try again.");
    }

    @Test
    void playerDataNotFoundError_logsWarningWithPlayerName() {
        when(player.getName()).thenReturn("Test");
        playerErrorHandler.playerDataNotFoundError(player);
        verify(logger).warning("Player Data could not be loaded for: Test");
    }
}
