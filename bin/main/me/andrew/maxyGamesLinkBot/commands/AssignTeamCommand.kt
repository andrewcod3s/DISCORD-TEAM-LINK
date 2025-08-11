package me.arnxld.maxyGamesLinkBot.commands

import me.arnxld.maxyGamesLinkBot.MaxyGamesLinkBot
import net.dv8tion.jda.api.entities.Member
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender


data class TeamSyncResult(
    val successCount: Int,
    val failCount: Int,
    val errors: List<String>
)

class AssignTeamCommand(private val plugin: MaxyGamesLinkBot) : CommandExecutor {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("linkbot.admin")) {
            sender.sendMessage(
                Component.text("You don't have permission to use this command!", NamedTextColor.RED)
            )
            return true
        }
        
        if (args.isNotEmpty()) {
            sender.sendMessage(
                Component.text("Usage: /assignteam", NamedTextColor.YELLOW)
            )
            sender.sendMessage(
                Component.text("This command assigns ALL linked users to their appropriate teams based on Discord roles.", NamedTextColor.GRAY)
            )
            return true
        }
        
        sender.sendMessage(
            Component.text("Syncing teams for all linked users...", NamedTextColor.YELLOW)
        )
        
        val result = syncAllTeams()
        
        result.errors.forEach { error ->
            sender.sendMessage(Component.text(error, NamedTextColor.RED))
        }
        
        sender.sendMessage(Component.empty())
        sender.sendMessage(
            Component.text("Team assignment complete! ", NamedTextColor.GREEN)
                .append(Component.text("${result.successCount} assigned, ", NamedTextColor.AQUA))
                .append(Component.text("${result.failCount} failed", NamedTextColor.RED))
        )
        
        plugin.logger.info("${sender.name} executed team assignment: ${result.successCount} assigned, ${result.failCount} failed")
        
        return true
    }

    fun syncAllTeams(): TeamSyncResult {
        val linkedUsers = plugin.configManager.getAllLinkedUsers()
        if (linkedUsers.isEmpty()) {
            return TeamSyncResult(0, 0, listOf("No linked users found!"))
        }
        
        val guild = plugin.discordBot.getGuild()
        
        if (guild == null) {
            return TeamSyncResult(0, 0, listOf("Discord bot is not connected or guild not found!"))
        }
        
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()
        
        linkedUsers.forEach { linkedUser ->
            try {
                val member = guild.getMemberById(linkedUser.discordUserId)
                if (member == null) {
                    errors.add("Discord member not found for ${linkedUser.minecraftUsername}")
                    failCount++
                    return@forEach
                }
                
                val assignedTeam = determineTeamForMember(member)
                
                if (assignedTeam != null) {
                    val updatedLink = linkedUser.copy(assignedTeam = assignedTeam)
                    plugin.configManager.addLinkedUser(updatedLink)
                    
                    val player = Bukkit.getPlayer(linkedUser.minecraftUuid)
                    if (player != null) {
                        assignPlayerToTeam(linkedUser.minecraftUsername, assignedTeam)
                    }
                    
                    plugin.logger.info("Assigned ${linkedUser.minecraftUsername} to team $assignedTeam")
                    successCount++
                } else {
                    errors.add("⚠ ${linkedUser.minecraftUsername} → No matching role found")
                    failCount++
                }
            } catch (e: Exception) {
                errors.add("✗ Failed to process ${linkedUser.minecraftUsername}: ${e.message}")
                failCount++
            }
        }
        
        return TeamSyncResult(successCount, failCount, errors)
    }
    
    private fun determineTeamForMember(member: Member): String? {
        val roleMappings = plugin.configManager.getRoleMappings()
        val memberRoles = member.roles.map { it.id }.toSet()
        
        return roleMappings
            .filter { it.discordRoleId in memberRoles }
            .maxByOrNull { it.priority }
            ?.minecraftTeam
    }
    
    private fun assignPlayerToTeam(playerName: String, teamName: String) {
        try {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "team add $teamName"
            )
            
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "team join $teamName $playerName"
            )
            
            plugin.logger.info("Assigned $playerName to team $teamName")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to assign $playerName to team $teamName: ${e.message}")
        }
    }
    

}
