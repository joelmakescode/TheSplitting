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
import org.thesplitting.src.data.item.CollectableItems.Items.FireSword;
import org.thesplitting.src.data.item.PlayerItems.StartSwordItem;
import org.thesplitting.src.data.player.PlayerData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerFileServiceTest {

    private static final Path FOLDER = Path.of("players");

    @TempDir
    Path dataFolder;

    @Mock
    private JavaPlugin plugin;

    @Mock
    private Player player;

    private final UUID uuid = UUID.randomUUID();

    private PlayerFileService playerFileService;

    @BeforeEach
    void setUp() {
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(player.getUniqueId()).thenReturn(uuid);
        playerFileService = new PlayerFileService(new ServiceRegistry(plugin), FOLDER);
        playerFileService.onEnable();
    }

    @Test
    void createPlayerFile_writesFileNamedAfterUuid() {
        when(player.getName()).thenReturn("Steve");

        playerFileService.createPlayerFile(player);

        assertTrue(Files.exists(dataFolder.resolve(FOLDER).resolve(uuid + ".json")));
    }

    @Test
    void createPlayerFile_thenRead_returnsStarterData() {
        when(player.getName()).thenReturn("Steve");
        playerFileService.createPlayerFile(player);

        PlayerData playerData = playerFileService.readPlayerFile(player);

        assertAll(
                () -> assertEquals("Steve", playerData.name()),
                () -> assertEquals("Novice", playerData.playerRole().getPlayerRole()),
                () -> assertEquals(1, playerData.playerSettings().getIsStarter()),
                () -> assertEquals(4, playerData.playerStats().getMaxHealth()),
                () -> assertEquals(StartSwordItem.ID, playerData.playerInventory().getInventorySlot(0)),
                () -> assertNull(playerData.playerInventory().getInventorySlot(1)),
                () -> assertEquals(1, playerData.collectableItems().getPossession(StartSwordItem.ID)),
                () -> assertEquals(0, playerData.collectableItems().getPossession(FireSword.ID))
        );
    }

    @Test
    void readPlayerFile_withoutFile_returnsNull() {
        assertNull(playerFileService.readPlayerFile(player));
    }

    @Test
    void writePlayerFile_changedData_isReadBack() {
        PlayerData playerData = PlayerData.create("Steve");
        playerData.collectableItems().setPossession(FireSword.ID, 1);
        playerData.playerInventory().setInventorySlot(0, FireSword.ID);

        playerFileService.writePlayerFile(player, playerData);
        PlayerData result = playerFileService.readPlayerFile(player);

        assertEquals(1, result.collectableItems().getPossession(FireSword.ID));
        assertEquals(FireSword.ID, result.playerInventory().getInventorySlot(0));
    }

    @Test
    void createPlayerFile_existingFile_keepsSavedProgress() {
        when(player.getName()).thenReturn("Steve");
        PlayerData playerData = PlayerData.create("Steve");
        playerData.collectableItems().setPossession(FireSword.ID, 1);
        playerFileService.writePlayerFile(player, playerData);

        playerFileService.createPlayerFile(player);

        assertEquals(1, playerFileService.readPlayerFile(player).collectableItems().getPossession(FireSword.ID));
    }
}
