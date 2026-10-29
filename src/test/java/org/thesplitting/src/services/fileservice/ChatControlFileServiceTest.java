package org.thesplitting.src.services.fileservice;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thesplitting.src.core.ServiceRegistry;
import org.thesplitting.src.data.server.chatcontrol.ChatControlLevel;
import org.thesplitting.src.data.server.chatcontrol.MessageData;
import org.thesplitting.src.data.server.chatcontrol.PlayerChatControlData;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControlFileServiceTest {

    private static final Path FOLDER = Path.of("chat-control");

    @TempDir
    Path dataFolder;

    @Mock
    private JavaPlugin plugin;

    @Mock
    private Player player;

    private final UUID uuid = UUID.randomUUID();

    private ChatControlFileService chatControlFileService;

    @BeforeEach
    void setUp() {
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("Steve");
        chatControlFileService = new ChatControlFileService(new ServiceRegistry(plugin), FOLDER);
        chatControlFileService.onEnable();
    }

    @Test
    void createPlayerChatControlDataFile_thenRead_returnsEmptyRecord() {
        chatControlFileService.createPlayerChatControlDataFile(player);

        PlayerChatControlData data = chatControlFileService.getPlayerChatControlData(player);

        assertEquals(uuid, data.uuid());
        assertEquals("Steve", data.name());
        assertTrue(data.flaggedMessages().isEmpty());
    }

    @Test
    void writeChatControlData_existingFile_appendsMessage() {
        chatControlFileService.createPlayerChatControlDataFile(player);
        MessageData first = new MessageData(LocalDateTime.of(2026, 10, 29, 12, 0), "first", ChatControlLevel.OFFENSIVE);
        MessageData second = new MessageData(LocalDateTime.of(2026, 10, 29, 12, 5), "second", ChatControlLevel.UNACCEPTABLE);

        chatControlFileService.writeChatControlData(player, first);
        chatControlFileService.writeChatControlData(player, second);

        assertEquals(List.of(first, second), chatControlFileService.getPlayerChatControlData(player).flaggedMessages());
    }

    @Test
    void writeChatControlData_withoutFile_createsRecordWithMessage() {
        MessageData message = new MessageData(LocalDateTime.of(2026, 10, 29, 12, 0), "text", ChatControlLevel.OFFENSIVE);

        chatControlFileService.writeChatControlData(player, message);

        PlayerChatControlData data = chatControlFileService.getPlayerChatControlData(player);
        assertEquals(uuid, data.uuid());
        assertEquals(List.of(message), data.flaggedMessages());
    }
}
