package org.thesplitting.src.services.fileservice;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thesplitting.src.core.ServiceRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final Path FOLDER = Path.of("test-folder");

    record Sample(String name, int value) {}

    record Timed(LocalDateTime time) {}

    @TempDir
    Path dataFolder;

    @Mock
    private JavaPlugin plugin;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        fileService = new FileService(new ServiceRegistry(plugin), FOLDER);
    }

    private Path jsonPath(String fileName) {
        return dataFolder.resolve(FOLDER).resolve(fileName + ".json");
    }

    @Test
    void onEnable_createsFolderInsidePluginFolder() {
        fileService.onEnable();

        assertTrue(Files.isDirectory(dataFolder.resolve(FOLDER)));
    }

    @Test
    void jsonFileCreate_newFile_writesObjectAsJson() throws IOException {
        fileService.onEnable();

        fileService.jsonFileCreate(FOLDER, "sample", new Sample("Steve", 3));

        String json = Files.readString(jsonPath("sample"));
        assertTrue(json.contains("\"name\": \"Steve\""), json);
        assertTrue(json.contains("\"value\": 3"), json);
    }

    @Test
    void jsonFileCreate_existingFile_keepsExistingContent() {
        fileService.onEnable();
        fileService.jsonFileCreate(FOLDER, "sample", new Sample("Steve", 3));

        fileService.jsonFileCreate(FOLDER, "sample", new Sample("Alex", 9));

        assertEquals(new Sample("Steve", 3), fileService.jsonFileRead(FOLDER, "sample", Sample.class));
    }

    @Test
    void jsonFileCreate_missingFolder_logsWarning() {
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);

        fileService.jsonFileCreate(FOLDER, "sample", new Sample("Steve", 3));

        assertFalse(Files.exists(jsonPath("sample")));
        verify(logger).warning(startsWith("FileManager failed"));
    }

    @Test
    void jsonFileRead_missingFile_returnsNull() {
        fileService.onEnable();

        assertNull(fileService.jsonFileRead(FOLDER, "missing", Sample.class));
    }

    @Test
    void jsonFileRead_fileNameWithExtension_readsSameFile() {
        fileService.onEnable();
        fileService.jsonFileCreate(FOLDER, "sample", new Sample("Steve", 3));

        assertEquals(new Sample("Steve", 3), fileService.jsonFileRead(FOLDER, "sample.json", Sample.class));
    }

    @Test
    void jsonFileRead_invalidJson_returnsNullAndLogsWarning() throws IOException {
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        fileService.onEnable();
        Files.writeString(jsonPath("broken"), "{ not json");

        assertNull(fileService.jsonFileRead(FOLDER, "broken", Sample.class));
        verify(logger).warning(startsWith("FileManager failed"));
    }

    @Test
    void jsonFileWrite_existingFile_overwritesContent() {
        fileService.onEnable();
        fileService.jsonFileCreate(FOLDER, "sample", new Sample("Steve", 3));

        fileService.jsonFileWrite(FOLDER, "sample", new Sample("Steve", 4));

        assertEquals(new Sample("Steve", 4), fileService.jsonFileRead(FOLDER, "sample", Sample.class));
    }

    @Test
    void localDateTime_isStoredAsIsoStringAndReadBack() throws IOException {
        fileService.onEnable();
        LocalDateTime time = LocalDateTime.of(2026, 10, 29, 18, 30, 15);

        fileService.jsonFileCreate(FOLDER, "timed", new Timed(time));

        assertTrue(Files.readString(jsonPath("timed")).contains("\"2026-10-29T18:30:15\""));
        assertEquals(time, fileService.jsonFileRead(FOLDER, "timed", Timed.class).time());
    }
}
