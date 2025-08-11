package me.arnxld.maxyGamesLinkBot.config

import me.arnxld.maxyGamesLinkBot.data.PlayerLink
import me.arnxld.maxyGamesLinkBot.data.RoleTeamMapping
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ConfigManager(private val dataFolder: File) {
    private val yaml = Yaml(DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        isPrettyFlow = true
    })
    
    private val linkedUsersFile = File(dataFolder, "linked-users.yml")
    private val roleMappingsFile = File(dataFolder, "role-mappings.yml")
    private val configFile = File(dataFolder, "config.yml")
    
    private val linkedUsers = ConcurrentHashMap<UUID, PlayerLink>()
    private val roleMappings = mutableListOf<RoleTeamMapping>()
    
    var discordBotToken: String = ""
        private set
    var discordGuildId: String = ""
        private set
    
    init {
        dataFolder.mkdirs()
        loadConfig()
        loadLinkedUsers()
        loadRoleMappings()
    }
    
    fun loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig()
        }
        
        try {
            val configData = yaml.load<Map<String, Any>>(FileReader(configFile)) ?: return
            discordBotToken = configData["discord-bot-token"] as? String ?: ""
            discordGuildId = configData["discord-guild-id"] as? String ?: ""
        } catch (e: Exception) {
            println("Error loading config: ${e.message}")
        }
    }
    
    private fun createDefaultConfig() {
        val defaultConfig = mapOf(
            "discord-bot-token" to "YOUR_BOT_TOKEN_HERE",
            "discord-guild-id" to "YOUR_GUILD_ID_HERE"
        )
        
        configFile.createNewFile()
        FileWriter(configFile).use { writer ->
            yaml.dump(defaultConfig, writer)
        }
    }
    
    fun loadLinkedUsers() {
        if (!linkedUsersFile.exists()) return
        
        try {
            val data = yaml.load<Map<String, Map<String, Any>>>(FileReader(linkedUsersFile)) ?: return
            
            linkedUsers.clear()
            data.forEach { (uuidStr, userData) ->
                try {
                    val uuid = UUID.fromString(uuidStr)
                    val link = PlayerLink(
                        minecraftUuid = uuid,
                        minecraftUsername = userData["minecraft-username"] as String,
                        discordUserId = userData["discord-user-id"] as String,
                        discordUsername = userData["discord-username"] as String,
                        linkedAt = Instant.parse(userData["linked-at"] as String),
                        assignedTeam = userData["assigned-team"] as? String
                    )
                    linkedUsers[uuid] = link
                } catch (e: Exception) {
                    println("Error loading user link $uuidStr: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Error loading linked users: ${e.message}")
        }
    }
    
    fun saveLinkedUsers() {
        try {
            val data = linkedUsers.mapKeys { it.key.toString() }.mapValues { (_, link) ->
                mapOf(
                    "minecraft-username" to link.minecraftUsername,
                    "discord-user-id" to link.discordUserId,
                    "discord-username" to link.discordUsername,
                    "linked-at" to link.linkedAt.toString(),
                    "assigned-team" to link.assignedTeam
                )
            }
            
            FileWriter(linkedUsersFile).use { writer ->
                yaml.dump(data, writer)
            }
        } catch (e: Exception) {
            println("Error saving linked users: ${e.message}")
        }
    }
    
    fun loadRoleMappings() {
        if (!roleMappingsFile.exists()) {
            createDefaultRoleMappings()
            return
        }
        
        try {
            val data = yaml.load<List<Map<String, Any>>>(FileReader(roleMappingsFile)) ?: return
            
            roleMappings.clear()
            data.forEach { mappingData ->
                try {
                    val mapping = RoleTeamMapping(
                        discordRoleId = mappingData["discord-role-id"] as String,
                        discordRoleName = mappingData["discord-role-name"] as? String ?: "",
                        minecraftTeam = mappingData["minecraft-team"] as String,
                        priority = (mappingData["priority"] as? Number)?.toInt() ?: 0
                    )
                    roleMappings.add(mapping)
                } catch (e: Exception) {
                    println("Error loading role mapping: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Error loading role mappings: ${e.message}")
        }
    }
    
    private fun createDefaultRoleMappings() {
        val defaultMappings = listOf(
            mapOf(
                "discord-role-id" to "ROLE_ID_HERE",
                "discord-role-name" to "Admin",
                "minecraft-team" to "admin",
                "priority" to 100
            ),
            mapOf(
                "discord-role-id" to "ROLE_ID_HERE",
                "discord-role-name" to "Moderator",
                "minecraft-team" to "moderator",
                "priority" to 50
            ),
            mapOf(
                "discord-role-id" to "ROLE_ID_HERE",
                "discord-role-name" to "Member",
                "minecraft-team" to "member",
                "priority" to 10
            )
        )
        
        roleMappingsFile.createNewFile()
        FileWriter(roleMappingsFile).use { writer ->
            yaml.dump(defaultMappings, writer)
        }
    }
    
    fun saveRoleMappings() {
        try {
            val data = roleMappings.map { mapping ->
                mapOf(
                    "discord-role-id" to mapping.discordRoleId,
                    "discord-role-name" to mapping.discordRoleName,
                    "minecraft-team" to mapping.minecraftTeam,
                    "priority" to mapping.priority
                )
            }
            
            FileWriter(roleMappingsFile).use { writer ->
                yaml.dump(data, writer)
            }
        } catch (e: Exception) {
            println("Error saving role mappings: ${e.message}")
        }
    }
    
    fun addLinkedUser(link: PlayerLink) {
        linkedUsers[link.minecraftUuid] = link
        saveLinkedUsers()
    }
    
    fun removeLinkedUser(uuid: UUID) {
        linkedUsers.remove(uuid)
        saveLinkedUsers()
    }
    
    fun getLinkedUser(uuid: UUID): PlayerLink? = linkedUsers[uuid]
    
    fun getLinkedUserByDiscordId(discordId: String): PlayerLink? = 
        linkedUsers.values.find { it.discordUserId == discordId }
    
    fun getAllLinkedUsers(): List<PlayerLink> = linkedUsers.values.toList()
    
    fun getRoleMappings(): List<RoleTeamMapping> = roleMappings.sortedByDescending { it.priority }
    
    fun addRoleMapping(mapping: RoleTeamMapping) {
        roleMappings.removeIf { it.discordRoleId == mapping.discordRoleId }
        roleMappings.add(mapping)
        saveRoleMappings()
    }
    
    fun removeRoleMapping(roleId: String) {
        roleMappings.removeIf { it.discordRoleId == roleId }
        saveRoleMappings()
    }
}
