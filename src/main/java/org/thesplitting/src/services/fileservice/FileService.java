package org.thesplitting.src.services.fileservice;

import com.google.gson.*;
import org.thesplitting.src.core.contracts.IService;
import org.thesplitting.src.core.ServiceRegistry;
import org.thesplitting.src.core.contracts.IFileService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileService implements IFileService, IService {
    protected final ServiceRegistry registry;
    protected static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, type, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();
    private final Path folderName;

    public FileService(ServiceRegistry registry, Path folderName) {
        this.registry = registry;
        this.folderName = folderName;
    }

    @Override
    public void onEnable() {
        try {
            folderCreate(folderName);
        } catch (IOException e) {
            logMessage(e);
        }
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void folderCreate(Path folderName) throws IOException {
        Path pluginFolder = registry.getPlugin().getDataFolder().toPath();
        Path folderPath = pluginFolder.resolve(folderName);

        try  {
            Files.createDirectories(folderPath);
        } catch (IOException e) {
            logMessage(e);
        }
    }

    @Override
    public void jsonFileCreate(Path folderName, String fileName, Object input) {
        Path jsonFilePath = buildJsonPath(folderName, fileName);

        try {
            if (Files.notExists(jsonFilePath)) {
                Files.createFile(jsonFilePath);
                Files.writeString(jsonFilePath, gson.toJson(input), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            logMessage(e);
        }
    }

    @Override
    public <T> T jsonFileRead(Path folderName, String fileName, Class<T> clazz) {
        Path jsonFilePath = buildJsonPath(folderName, fileName);

        if (Files.notExists(jsonFilePath)) {
            return null;
        }

        try {
            String json = Files.readString(jsonFilePath);
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            logMessage(e);
        }
        return null;
    }

    @Override
    public void jsonFileWrite(Path folderName, String fileName, Object input) {
        try {
            Path jsonFilePath = buildJsonPath(folderName, fileName);
            Files.writeString(jsonFilePath, gson.toJson(input), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logMessage(e);
        }
    }

    private Path buildJsonPath(Path folderName, String fileName) {
        Path pluginFolder = registry.getPlugin().getDataFolder().toPath();
        Path folderPath = pluginFolder.resolve(folderName);

        if (!fileName.endsWith(".json")) {
            fileName += ".json";
        }

        return folderPath.resolve(fileName);
    }

    private void logMessage(Exception e) {
        registry.getPlugin().getLogger().warning("FileManager failed: " + e.getMessage());
    }
}
