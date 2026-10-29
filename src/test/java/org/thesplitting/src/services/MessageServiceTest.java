package org.thesplitting.src.services;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

class MessageServiceTest {
    private static final String PREFIX = ChatColor.GOLD + "[The Splitting] ";

    private Player player;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
    }

    @Test
    void successMessage_sendsGreenMessageWithPrefix() {
        MessageService.successMessage(player, "Item Switch successful!");

        verify(player).sendMessage(PREFIX + ChatColor.GREEN + "Item Switch successful!");
    }

    static Stream<Arguments> messageTypes() {
        return Stream.of(
                Arguments.of("success", (BiConsumer<Player, String>) MessageService::successMessage, ChatColor.GREEN),
                Arguments.of("info", (BiConsumer<Player, String>) MessageService::infoMessage, ChatColor.BLUE),
                Arguments.of("warn", (BiConsumer<Player, String>) MessageService::warnMessage, ChatColor.YELLOW),
                Arguments.of("error", (BiConsumer<Player, String>) MessageService::errorMessage, ChatColor.RED)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("messageTypes")
    void everyMessageType_sendsPrefixColorOnlyOnce(String type, BiConsumer<Player, String> sender, ChatColor color) {
        sender.accept(player, "Hello");

        verify(player, times(1)).sendMessage(PREFIX + color + "Hello");
        verifyNoMoreInteractions(player);
    }
}
