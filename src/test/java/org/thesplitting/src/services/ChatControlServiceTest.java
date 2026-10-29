package org.thesplitting.src.services;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thesplitting.src.data.player.PlayerData;
import org.thesplitting.src.data.server.chatcontrol.ChatControlLevel;
import org.thesplitting.src.data.server.chatcontrol.MessageData;
import org.thesplitting.src.data.server.chatcontrol.PlayerChatControlData;
import org.thesplitting.src.services.fileservice.ChatControlFileService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControlServiceTest {
    private static String highlighted(String word) {
        return ChatColor.YELLOW + word + ChatColor.RESET;
    }

    @Nested
    class HighlightWord {
        @Test
        void singleOccurence_wrapsWordInYellow() {
            String result = ChatControlService.highlightWord("you are an idiot", "idiot");

            assertEquals("you are an " + highlighted("idiot"),  result);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"idiot!", "(idiot)", "idiot.", "\"idiot\""})
        void wordNextToPunctuation_isHighlighted(String message) {
            String result = ChatControlService.highlightWord(message, "idiot");

            assertEquals(message.replace("idiot", highlighted("idiot")), result);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"idiotic", "notidiot", "idiot2"})
        void wordInsideLongerWord_isNotHighlighted(String message) {
            String result = ChatControlService.highlightWord(message, "idiot");

            assertEquals(message, result);
        }

        @Test
        void regexCharactersInWord_areMatchedLiterally() {
            String result = ChatControlService.highlightWord("axb a.b", "a.b");

            assertEquals("axb " + highlighted("a.b"), result);
        }
    }

    @Mock
    private JavaPlugin plugin;

    @Mock
    private ChatControlFileService chatControlFileService;

    @Mock
    private PlayerService playerService;

    @Mock
    private Player player;

    @TempDir
    Path dataFolder;

    private ChatControlService chatControlService;

    @BeforeEach
    void setUp() {
        chatControlService = new ChatControlService(plugin, chatControlFileService, playerService);
    }

    private void writeWordList(String fileName, String... lines) throws IOException {
        Path folder = dataFolder.resolve("chat-control");
        Files.createDirectories(folder);
        Files.write(folder.resolve(fileName), List.of(lines));
    }

    private PlayerChatEvent chat(String message) {
        return new PlayerChatEvent(player, message, "<%1$s> %2$s", new HashSet<>());
    }

    private String chatPrefix() {
        return ChatColor.AQUA + "[GLOBAL] " + ChatColor.RESET + ChatColor.GREEN + "Novice | Steve: " + ChatColor.RESET;
    }

    private void playerIsNovice() {
        when(player.getName()).thenReturn("Steve");
        when(playerService.loadPlayerFile(player)).thenReturn(PlayerData.create("Steve"));
    }

    @Nested
    class ChatFilter {

        @BeforeEach
        void setUp() throws IOException {
            when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
            writeWordList("offensive-words.txt", "# comment", "", "  Idiot  ");
            writeWordList("unacceptable-words.txt", "dumm", "# blocked");
            chatControlService.onEnable();
        }

        @Test
        void onEnable_extractsBothWordListsFromJar() {
            verify(plugin).saveResource("chat-control/offensive-words.txt", false);
            verify(plugin).saveResource("chat-control/unacceptable-words.txt", false);
        }

        @Test
        void cleanMessage_isPrefixedAndNotStored() {
            playerIsNovice();
            PlayerChatEvent event = chat("hello world");

            chatControlService.takeOverChatListener(event);

            assertFalse(event.isCancelled());
            assertEquals("%2$s", event.getFormat());
            assertEquals(chatPrefix() + "hello world", event.getMessage());
            verify(chatControlFileService, never()).writeChatControlData(any(), any());
        }

        @Test
        void offensiveMessage_isHighlightedMarkedAndStored() {
            playerIsNovice();
            PlayerChatEvent event = chat("you idiot");

            chatControlService.takeOverChatListener(event);

            assertFalse(event.isCancelled());
            assertEquals(chatPrefix() + "you " + highlighted("idiot") + " " + ChatColor.YELLOW + "[Marked as Offensive]", event.getMessage());
            assertStored(ChatControlLevel.OFFENSIVE, "you idiot");
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"du bist dumm", "DUMM!", "dumm, idiot"})
        void unacceptableMessage_isBlockedAndStored(String message) {
            PlayerChatEvent event = chat(message);

            chatControlService.takeOverChatListener(event);

            assertTrue(event.isCancelled());
            verify(player).sendMessage(endsWith("This message was blocked."));
            assertStored(ChatControlLevel.UNACCEPTABLE, message);
        }

        @Test
        void commentLinesInWordList_areIgnored() {
            playerIsNovice();
            PlayerChatEvent event = chat("blocked comment");

            chatControlService.takeOverChatListener(event);

            assertFalse(event.isCancelled());
            verify(chatControlFileService, never()).writeChatControlData(any(), any());
        }

        @Test
        void wordInsideLongerWord_isNotFiltered() {
            playerIsNovice();
            PlayerChatEvent event = chat("idiotic dummy");

            chatControlService.takeOverChatListener(event);

            assertFalse(event.isCancelled());
            assertEquals(chatPrefix() + "idiotic dummy", event.getMessage());
        }

        private void assertStored(ChatControlLevel level, String message) {
            ArgumentCaptor<MessageData> captor = ArgumentCaptor.forClass(MessageData.class);
            verify(chatControlFileService).writeChatControlData(eq(player), captor.capture());
            assertEquals(level, captor.getValue().level());
            assertEquals(message, captor.getValue().message());
            assertNotNull(captor.getValue().dateTime());
        }
    }

    @Test
    void onEnable_missingWordLists_logsWarningAndLetsEverythingThrough() {
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        chatControlService.onEnable();
        playerIsNovice();
        PlayerChatEvent event = chat("dumm");

        chatControlService.takeOverChatListener(event);

        verify(logger, times(2)).warning(startsWith("Could not load word list"));
        assertFalse(event.isCancelled());
    }

    @Test
    void formatChatDisplay_showsRoleAndName() {
        playerIsNovice();

        assertEquals(chatPrefix(), chatControlService.formatChatDisplay(player));
    }

    @Nested
    class LoadChatControlFile {

        @Test
        void existingFile_isReturned() {
            PlayerChatControlData data = new PlayerChatControlData(UUID.randomUUID(), "Steve");
            when(chatControlFileService.getPlayerChatControlData(player)).thenReturn(data);

            assertSame(data, chatControlService.loadChatControlFile(player));
            verify(chatControlFileService, never()).createPlayerChatControlDataFile(player);
        }

        @Test
        void missingFile_isCreatedAndReturned() {
            PlayerChatControlData data = new PlayerChatControlData(UUID.randomUUID(), "Steve");
            when(chatControlFileService.getPlayerChatControlData(player)).thenReturn(null, data);

            assertSame(data, chatControlService.loadChatControlFile(player));
            verify(chatControlFileService).createPlayerChatControlDataFile(player);
        }
    }
}
