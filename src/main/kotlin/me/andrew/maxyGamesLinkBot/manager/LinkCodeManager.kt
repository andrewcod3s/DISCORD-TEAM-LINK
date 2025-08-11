package me.arnxld.maxyGamesLinkBot.manager

import me.arnxld.maxyGamesLinkBot.data.PendingLink
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class LinkCodeManager {
    private val pendingLinks = ConcurrentHashMap<String, PendingLink>()
    private val playerCodes = ConcurrentHashMap<UUID, String>()
    

    fun generateCode(playerUuid: UUID, playerName: String): String {
        playerCodes[playerUuid]?.let { oldCode ->
            pendingLinks.remove(oldCode)
        }
        
        var code: String
        do {
            code = String.format("%06d", Random.nextInt(100000, 999999))
        } while (pendingLinks.containsKey(code))
        
        val pendingLink = PendingLink(
            code = code,
            minecraftUuid = playerUuid,
            minecraftUsername = playerName
        )
        
        pendingLinks[code] = pendingLink
        playerCodes[playerUuid] = code
        
        cleanupExpiredCodes()
        
        return code
    }
    

    fun validateAndConsumeCode(code: String): PendingLink? {
        val pendingLink = pendingLinks[code] ?: return null
        
        if (pendingLink.isExpired()) {
            pendingLinks.remove(code)
            playerCodes.remove(pendingLink.minecraftUuid)
            return null
        }
        
        pendingLinks.remove(code)
        playerCodes.remove(pendingLink.minecraftUuid)
        
        return pendingLink
    }
    

    fun getCodeForPlayer(playerUuid: UUID): String? {
        val code = playerCodes[playerUuid] ?: return null
        val pendingLink = pendingLinks[code] ?: return null
        
        return if (pendingLink.isExpired()) {
            pendingLinks.remove(code)
            playerCodes.remove(playerUuid)
            null
        } else {
            code
        }
    }
    
   
    fun hasActiveCode(playerUuid: UUID): Boolean {
        return getCodeForPlayer(playerUuid) != null
    }
    

    private fun cleanupExpiredCodes() {
        val expiredCodes = pendingLinks.values
            .filter { it.isExpired() }
            .map { it.code }
        
        expiredCodes.forEach { code ->
            val pendingLink = pendingLinks.remove(code)
            pendingLink?.let { playerCodes.remove(it.minecraftUuid) }
        }
    }
    

    fun performCleanup() {
        cleanupExpiredCodes()
    }
}
