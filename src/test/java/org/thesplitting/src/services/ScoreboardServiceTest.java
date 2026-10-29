package org.thesplitting.src.services;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thesplitting.src.data.player.PlayerData;
import org.thesplitting.src.data.player.PlayerRoles.PlayerRoles;

import java.util.EnumMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreboardServiceTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private Scoreboard board;

    @Mock
    private Player player;

    private MockedStatic<Bukkit> bukkit;
    private final Map<PlayerRoles, Team> teams = new EnumMap<>(PlayerRoles.class);
    private ScoreboardService scoreboardService;

    @BeforeEach
    void setUp() {
        ScoreboardManager manager = mock(ScoreboardManager.class);
        when(manager.getMainScoreboard()).thenReturn(board);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScoreboardManager).thenReturn(manager);

        for (PlayerRoles role : PlayerRoles.values()) {
            teams.put(role, mock(Team.class));
        }

        scoreboardService = new ScoreboardService(playerService);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private void teamsExist() {
        teams.forEach((role, team) -> when(board.getTeam("role_" + role.name())).thenReturn(team));
    }

    @Test
    void onEnable_missingTeams_registersTeamPerRoleWithPrefix() {
        teams.forEach((role, team) -> when(board.registerNewTeam("role_" + role.name())).thenReturn(team));

        scoreboardService.onEnable();

        verify(teams.get(PlayerRoles.NOVICE)).setPrefix(ChatColor.GREEN + "[Novice] ");
        verify(teams.get(PlayerRoles.PRODIGY)).setPrefix(ChatColor.DARK_BLUE + "[Prodigy] ");
        verify(teams.get(PlayerRoles.INTELLECTUAL)).setPrefix(ChatColor.GOLD + "[Intellectual] ");
    }

    @Test
    void onEnable_existingTeams_areReused() {
        teamsExist();

        scoreboardService.onEnable();

        verify(board, never()).registerNewTeam(anyString());
    }

    @Test
    void updatePlayerTeam_addsPlayerToRoleTeamAndRemovesFromOthers() {
        teamsExist();
        when(player.getName()).thenReturn("Steve");
        PlayerData playerData = PlayerData.create("Steve");
        playerData.playerRole().setPlayerRole(PlayerRoles.PRODIGY.name());
        when(playerService.loadPlayerFile(player)).thenReturn(playerData);

        scoreboardService.updatePlayerTeam(player);

        verify(teams.get(PlayerRoles.PRODIGY)).addEntry("Steve");
        verify(teams.get(PlayerRoles.NOVICE)).removeEntry("Steve");
        verify(teams.get(PlayerRoles.INTELLECTUAL)).removeEntry("Steve");
        verify(teams.get(PlayerRoles.PRODIGY), never()).removeEntry("Steve");
    }
}
