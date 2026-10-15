package org.thesplitting.src.services;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class MessageService {
    private static final String TITLE = ChatColor.GOLD + "[The Splitting] ";

    public static void successMessage(Player player, String message) {
        player.sendMessage(TITLE + ChatColor.GREEN + message);
    }

    public static void infoMessage(Player player, String message) {
        player.sendMessage(TITLE + ChatColor.BLUE + message);
    }

    public static void warnMessage(Player player, String message) {
        player.sendMessage(TITLE + ChatColor.YELLOW + message);
    }

    public static void errorMessage(Player player, String message) {
        player.sendMessage(TITLE + ChatColor.RED + message);
    }
}
