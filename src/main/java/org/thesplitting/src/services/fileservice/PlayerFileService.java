package org.thesplitting.src.services.fileservice;

import org.bukkit.entity.Player;
import org.thesplitting.src.data.player.PlayerData;
import org.thesplitting.src.core.ServiceRegistry;

import java.nio.file.Path;

public class PlayerFileService extends FileService {
    protected final Path folderName;

    public PlayerFileService(ServiceRegistry registry, Path folderName) {
        super(registry, folderName);
        this.folderName = folderName;
    }

    public void createPlayerFile(Player player) {
        PlayerData playerData = PlayerData.create(player.getName());
        super.jsonFileCreate(folderName, player.getUniqueId().toString(), playerData);
    }

    public PlayerData readPlayerFile(Player player) {
        return super.jsonFileRead(folderName, player.getUniqueId().toString(), PlayerData.class);
    }

    public void writePlayerFile(Player player, Object input) {
        super.jsonFileWrite(folderName, player.getUniqueId().toString(), input);
    }
}
