package org.thesplitting.src.services;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldServiceTest {

    @TempDir
    Path root;

    @Mock
    private JavaPlugin plugin;

    @Mock
    private Logger logger;

    private MockedStatic<Bukkit> bukkit;
    private WorldService worldService;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        worldService = new WorldService(plugin);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Nested
    class OnEnable {

        private Path templatePath;
        private Path worldPath;

        @BeforeEach
        void setUp() {
            Path pluginFolder = root.resolve("plugin");
            Path serverFolder = root.resolve("server");
            templatePath = pluginFolder.resolve("templates").resolve("world");
            worldPath = serverFolder.resolve("world");

            when(plugin.getDataFolder()).thenReturn(pluginFolder.toFile());
            when(plugin.getLogger()).thenReturn(logger);
            bukkit.when(Bukkit::getWorldContainer).thenReturn(serverFolder.toFile());
        }

        @Test
        void withoutTemplate_createsTemplateFolderAndSkipsWorld() {
            worldService.onEnable();

            assertTrue(Files.isDirectory(templatePath));
            verify(logger).warning("KEIN WORLD TEMPLATE GEFUNDEN!");
            bukkit.verify(() -> Bukkit.createWorld(any()), never());
        }

        @Test
        void withTemplate_replacesOldWorldWithFreshCopy() throws IOException {
            Files.createDirectories(templatePath.resolve("region"));
            Files.writeString(templatePath.resolve("level.dat"), "level");
            Files.writeString(templatePath.resolve("region").resolve("r.0.0.mca"), "region");
            Files.writeString(templatePath.resolve("session.lock"), "lock");
            Files.writeString(templatePath.resolve("uid.dat"), "uid");
            Files.createDirectories(worldPath);
            Files.writeString(worldPath.resolve("stale.dat"), "old");
            World world = mock(World.class);
            bukkit.when(() -> Bukkit.createWorld(any())).thenReturn(world);

            worldService.onEnable();

            assertEquals("level", Files.readString(worldPath.resolve("level.dat")));
            assertEquals("region", Files.readString(worldPath.resolve("region").resolve("r.0.0.mca")));
            assertFalse(Files.exists(worldPath.resolve("stale.dat")));
            assertFalse(Files.exists(worldPath.resolve("session.lock")));
            assertFalse(Files.exists(worldPath.resolve("uid.dat")));
            assertTrue(Files.exists(templatePath.resolve("session.lock")));
            bukkit.verify(() -> Bukkit.createWorld(argThat((WorldCreator creator) -> creator.name().equals("world"))));
            verify(world).setAutoSave(false);
            verify(world).setKeepSpawnInMemory(true);
        }
    }

    @Nested
    class OnDisable {

        @Test
        void gameWorldNotLoaded_doesNothing() {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(null);

            worldService.onDisable();

            bukkit.verify(() -> Bukkit.unloadWorld(any(World.class), anyBoolean()), never());
        }

        @Test
        void gameWorldLoaded_movesPlayersToDefaultWorldAndUnloadsWithoutSaving() {
            World gameWorld = mock(World.class);
            World defaultWorld = mock(World.class);
            Location spawn = mock(Location.class);
            Player player = mock(Player.class);
            when(defaultWorld.getSpawnLocation()).thenReturn(spawn);
            when(gameWorld.getPlayers()).thenReturn(List.of(player));
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(gameWorld);
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(defaultWorld, gameWorld));

            worldService.onDisable();

            verify(player).teleport(spawn);
            bukkit.verify(() -> Bukkit.unloadWorld(gameWorld, false));
        }
    }

    @Test
    void getGameWorld_returnsLoadedWorld() {
        World world = mock(World.class);
        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);

        assertSame(world, worldService.getGameWorld());
    }
}
