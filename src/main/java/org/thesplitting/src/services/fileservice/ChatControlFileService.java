package org.thesplitting.src.services.fileservice;

import org.bukkit.entity.Player;
import org.thesplitting.src.data.server.chatcontrol.MessageData;
import org.thesplitting.src.data.server.chatcontrol.PlayerChatControlData;
import org.thesplitting.src.core.ServiceRegistry;

import java.nio.file.Path;

public class ChatControlFileService extends FileService {
    private final Path folderName;

    public ChatControlFileService(ServiceRegistry registry, Path folderName) {
        super(registry, folderName);
        this.folderName = folderName;
    }

    public void createPlayerChatControlDataFile(Player player) {
        PlayerChatControlData playerChatControlData = new PlayerChatControlData(player.getUniqueId(), player.getName());
        super.jsonFileCreate(folderName,  player.getUniqueId().toString(), playerChatControlData);
    }

    public PlayerChatControlData getPlayerChatControlData(Player player) {
        return super.jsonFileRead(folderName, player.getUniqueId().toString(), PlayerChatControlData.class);
    }

    public void writeChatControlData(Player player, MessageData messageData) {
        PlayerChatControlData current = getPlayerChatControlData(player);
        if (current == null) {
            current = new PlayerChatControlData(player.getUniqueId(), player.getName());
        }

        current.flaggedMessages().add(messageData);
        super.jsonFileWrite(folderName, player.getUniqueId().toString(), current);
    }
}
