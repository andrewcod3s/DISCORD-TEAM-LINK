package me.arnxld.maxyGamesLinkBot.data

import java.time.Instant
import java.util.*

data class PlayerLink(
    val minecraftUuid: UUID,
    val minecraftUsername: String,
    val discordUserId: String,
    val discordUsername: String,
    val linkedAt: Instant = Instant.now(),
    val assignedTeam: String? = null
)


data class PendingLink(
    val code: String,
    val minecraftUuid: UUID,
    val minecraftUsername: String,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant = Instant.now().plusSeconds(300) // 5 minutes expiry
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
}


data class RoleTeamMapping(
    val discordRoleId: String,
    val discordRoleName: String = "",
    val minecraftTeam: String,
    val priority: Int = 0 
)
