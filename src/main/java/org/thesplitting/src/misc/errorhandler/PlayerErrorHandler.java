package org.thesplitting.src.misc.errorhandler;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesplitting.src.core.contracts.IService;

public class PlayerErrorHandler implements IService {
    private final JavaPlugin plugin;

    public PlayerErrorHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    /**
     * Handles an error if PlayerData was not found
     * @param player
     */
    public void playerDataNotFoundError(Player player) {
        player.kickPlayer(ChatColor.RED + "Your Player Data could not be loaded. Please try again.");
        plugin.getLogger().warning("Player Data could not be loaded for: " + player.getName());
    }
}
