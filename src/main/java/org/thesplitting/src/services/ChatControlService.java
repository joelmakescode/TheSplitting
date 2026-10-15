package org.thesplitting.src.services;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesplitting.src.data.player.PlayerData;
import org.thesplitting.src.data.player.PlayerRoles.PlayerRoles;
import org.thesplitting.src.data.server.chatcontrol.ChatControlLevel;
import org.thesplitting.src.data.server.chatcontrol.ChatFilterResult;
import org.thesplitting.src.data.server.chatcontrol.MessageData;
import org.thesplitting.src.data.server.chatcontrol.PlayerChatControlData;
import org.thesplitting.src.misc.helper.ToolBox;
import org.thesplitting.src.core.contracts.IService;
import org.thesplitting.src.services.fileservice.ChatControlFileService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class ChatControlService implements IService {
    private static final Path FOLDER = Path.of("chat-control");
    private static final String OFFENSIVE_RESOURCE = "chat-control/offensive-words.txt";
    private static final String UNACCEPTABLE_RESOURCE = "chat-control/unacceptable-words.txt";

    private final JavaPlugin plugin;
    private final ChatControlFileService chatControlFileService;
    private final PlayerService playerService;
    private Set<String> offensiveWords = Set.of();
    private Set<String> unacceptableWords = Set.of();

    public ChatControlService(JavaPlugin plugin, ChatControlFileService chatControlFileService, PlayerService playerService) {
        this.plugin = plugin;
        this.chatControlFileService = chatControlFileService;
        this.playerService = playerService;
    }

    @Override
    public void onEnable() {
        plugin.saveResource(OFFENSIVE_RESOURCE, false);
        plugin.saveResource(UNACCEPTABLE_RESOURCE, false);

        offensiveWords = loadWordList("offensive-words.txt");
        unacceptableWords = loadWordList("unacceptable-words.txt");
    }

    @Override
    public void onDisable() {

    }

    public PlayerChatControlData loadChatControlFile(Player player) {
        PlayerChatControlData playerChatControlData = chatControlFileService.getPlayerChatControlData(player);
        if (playerChatControlData == null) {
            chatControlFileService.createPlayerChatControlDataFile(player);

            return loadChatControlFile(player);
        }

        return playerChatControlData;
    }

    public void takeOverChatListener(PlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatFilterResult result = evaluate(event.getMessage());
        event.setFormat("%2$s");

        switch (result.level()) {
            case UNACCEPTABLE -> {
                event.setCancelled(true);
                MessageService.errorMessage(player, ChatColor.RED + "This message was blocked.");
            }
            case OFFENSIVE -> event.setMessage(
                    formatChatDisplay(event.getPlayer()) + ChatControlService.highlightWord(event.getMessage(), result.matchedWord())
                            + " " + ChatColor.YELLOW + "[Marked as Offensive]"
            );
            case CLEAN -> event.setMessage(
                    formatChatDisplay(event.getPlayer()) + event.getMessage()
            );
        }

        if (result.level() != ChatControlLevel.CLEAN) {
            chatControlFileService.writeChatControlData(player, MessageData.create(result.message(), result.level()));
        }
    }

    private ChatFilterResult evaluate(String message) {
        String[] tokens = message.toLowerCase(Locale.ROOT).split("[^\\p{L}0-9]+");

        for (String token : tokens) {
            if (unacceptableWords.contains(token)) {
                return new ChatFilterResult(ChatControlLevel.UNACCEPTABLE, token, message);
            }
        }

        for (String token : tokens) {
            if (offensiveWords.contains(token)) {
                return new ChatFilterResult(ChatControlLevel.OFFENSIVE, token, message);
            }
        }

        return new ChatFilterResult(ChatControlLevel.CLEAN, null, message);
    }

    public static String highlightWord(String message, String word) {
        return message.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", ChatColor.YELLOW + "$0" + ChatColor.RESET);
    }

    public String formatChatDisplay(Player player) {
        PlayerData playerData = playerService.loadPlayerFile(player);
        PlayerRoles role = PlayerRoles.valueOf(playerData.playerRole().getPlayerRole().toUpperCase());

        return ChatColor.AQUA + "[GLOBAL] " +  ChatColor.RESET + role.getColor() + ToolBox.formatEnumToWord(role.name()) + " | " + player.getName() + ": " + ChatColor.RESET;
    }

    private Set<String> loadWordList(String fileName) {
        Path path = plugin.getDataFolder().toPath().resolve(FOLDER).resolve(fileName);
        Set<String> words = new HashSet<String>();

        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim().toLowerCase(Locale.ROOT);

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                words.add(trimmed);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load word list from file " + fileName + ": " + e.getMessage());
        }

        return words;
    }
}
