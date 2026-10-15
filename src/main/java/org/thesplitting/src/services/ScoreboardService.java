package org.thesplitting.src.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.thesplitting.src.data.player.PlayerData;
import org.thesplitting.src.data.player.PlayerRoles.PlayerRoles;
import org.thesplitting.src.misc.helper.ToolBox;
import org.thesplitting.src.core.contracts.IService;

public class ScoreboardService implements IService {
    private final PlayerService playerService;

    public ScoreboardService(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public void onEnable() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        for (PlayerRoles role : PlayerRoles.values()) {
            registerRoleTeam(board, role);
        }
    }

    @Override
    public void onDisable() {

    }

    public void updatePlayerTeam(Player player) {
        PlayerData playerData = playerService.loadPlayerFile(player);
        PlayerRoles role = PlayerRoles.valueOf(playerData.playerRole().getPlayerRole().toUpperCase());

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = registerRoleTeam(board, role);

        removeFromOtherRoleTeams(board, player, role);
        team.addEntry(player.getName());
    }

    private Team registerRoleTeam(Scoreboard board, PlayerRoles role) {
        String teamName = teamName(role);
        Team team = board.getTeam(teamName);

        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        team.setPrefix(role.getColor() + "[" + ToolBox.formatEnumToWord(role.name()) + "] ");

        return team;
    }

    private void removeFromOtherRoleTeams(Scoreboard board, Player player, PlayerRoles currentRole) {
        for (PlayerRoles role : PlayerRoles.values()) {
            if (role == currentRole) {
                continue;
            }

            Team team = board.getTeam(teamName(role));
            if (team != null) {
                team.removeEntry(player.getName());
            }
        }
    }

    private String teamName(PlayerRoles role) {
        return "role_" + role.name();
    }
}
