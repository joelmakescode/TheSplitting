package org.thesplitting.src.data.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.thesplitting.src.data.player.PlayerRoles.PlayerRole;
import org.thesplitting.src.data.player.PlayerRoles.PlayerRoles;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerRoleTest {
    @Test
    void constructor_enumName_storesFormattedRole() {
        PlayerRole playerRole = new PlayerRole(PlayerRoles.NOVICE.toString());
        assertEquals("Novice", playerRole.getPlayerRole());
    }

    @Test
    void setPlayerRole_enumName_overwritesWithFormatedRole() {
        PlayerRole playerRole = new PlayerRole(PlayerRoles.NOVICE.toString());
        playerRole.setPlayerRole(PlayerRoles.INTELLECTUAL.toString());
        assertEquals("Intellectual", playerRole.getPlayerRole());
    }

    @ParameterizedTest
    @EnumSource(PlayerRoles.class)
    void getPlayerRole_everyRole_canBeResolvedBackToEnum(PlayerRoles role) {
        PlayerRole playerRole = new PlayerRole(role.name());
        PlayerRoles resolved = PlayerRoles.valueOf(playerRole.getPlayerRole().toUpperCase());
        assertEquals(role, resolved);
    }
}
