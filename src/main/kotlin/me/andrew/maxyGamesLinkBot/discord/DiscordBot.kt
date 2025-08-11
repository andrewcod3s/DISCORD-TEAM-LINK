package me.arnxld.maxyGamesLinkBot.discord

import me.arnxld.maxyGamesLinkBot.MaxyGamesLinkBot
import me.arnxld.maxyGamesLinkBot.data.PlayerLink
import me.arnxld.maxyGamesLinkBot.manager.LinkCodeManager
import me.arnxld.maxyGamesLinkBot.utils.PlayerAvatarUtil
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import org.bukkit.Bukkit
import java.awt.Color
import java.time.Instant
import java.util.UUID

class DiscordBot(
    private val plugin: MaxyGamesLinkBot,
    private val linkCodeManager: LinkCodeManager
) : ListenerAdapter() {
    
    private var jda: JDA? = null
    private var guild: Guild? = null
    private val lightEmbedColor = Color(245, 245, 245)
    
    fun getGuild(): Guild? = guild
    
    fun start() {
        val token = plugin.configManager.discordBotToken
        val guildId = plugin.configManager.discordGuildId
        
        if (token.isEmpty() || token == "YOUR_BOT_TOKEN_HERE") {
            plugin.logger.severe("Discord bot token not configured! Please set it in config.yml")
            return
        }
        
        if (guildId.isEmpty() || guildId == "YOUR_GUILD_ID_HERE") {
            plugin.logger.severe("Discord guild ID not configured! Please set it in config.yml")
            return
        }
        
        try {
            jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(this)
                .build()
                
            plugin.logger.info("Discord bot starting up...")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to start Discord bot: ${e.message}")
        }
    }
    
    fun shutdown() {
        jda?.shutdown()
    }
    
    override fun onReady(event: ReadyEvent) {
        plugin.logger.info("Discord bot is ready!")
        
        val guildId = plugin.configManager.discordGuildId
        guild = jda?.getGuildById(guildId)
        
        if (guild == null) {
            plugin.logger.severe("Could not find Discord guild with ID: $guildId")
            return
        }
        
        guild!!.updateCommands().addCommands(
            Commands.slash("link", "Link your Minecraft account with Discord")
                .addOption(OptionType.STRING, "code", "The 6-digit code from Minecraft", true),
            Commands.slash("linkeduserteams", "Show all linked users and their assigned teams"),
            Commands.slash("whois", "Lookup link between Minecraft and Discord")
                .addOption(OptionType.USER, "user", "Discord user to lookup", false)
                .addOption(OptionType.STRING, "player", "Minecraft username or UUID", false)
        ).queue(
            { plugin.logger.info("Slash commands registered successfully!") },
            { error -> plugin.logger.severe("Failed to register slash commands: ${error.message}") }
        )
        
        guild!!.loadMembers().onSuccess { members ->
            plugin.logger.info("Guild member cache loaded (${members.size} members)")
            
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
                syncTeamsOnStartup()
            }, 60L) 
        }.onError { error ->
            plugin.logger.warning("Failed to load guild member cache: ${error.message}")
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
                syncTeamsOnStartup()
            }, 60L)
        }
    }
    

    private fun syncTeamsOnStartup() {
        try {
            plugin.logger.info("Syncing teams for all linked users...")
            
            val result = plugin.getAssignTeamCommand().syncAllTeams()
            
            if (result.successCount > 0 || result.failCount > 0) {
                plugin.logger.info("Team assignment complete! ${result.successCount} assigned, ${result.failCount} failed")
                
                result.errors.forEach { error ->
                    plugin.logger.warning("Team sync error: $error")
                }
            } else {
                plugin.logger.info("No linked users found to sync teams for")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to sync teams on startup: ${e.message}")
        }
    }
    
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "link" -> handleLinkCommand(event)
            "linkeduserteams" -> handleLinkedUserTeamsCommand(event)
            "whois" -> handleWhoisCommand(event)
        }
    }
    
    private fun handleLinkCommand(event: SlashCommandInteractionEvent) {
        val code = event.getOption("code")?.asString ?: run {
            event.reply("❌ Please provide a linking code!").setEphemeral(true).queue()
            return
        }
        
        val existingLink = plugin.configManager.getLinkedUserByDiscordId(event.user.id)
        if (existingLink != null) {
            event.reply("❌ You are already linked to **${existingLink.minecraftUsername}**!")
                .setEphemeral(true).queue()
            return
        }
        
        val pendingLink = linkCodeManager.validateAndConsumeCode(code)
        if (pendingLink == null) {
            event.reply("❌ Invalid or expired linking code! Please generate a new one in Minecraft using `/link`")
                .setEphemeral(true).queue()
            return
        }
        
        val assignedTeam: String? = null
        
        val playerLink = PlayerLink(
            minecraftUuid = pendingLink.minecraftUuid,
            minecraftUsername = pendingLink.minecraftUsername,
            discordUserId = event.user.id,
            discordUsername = event.user.effectiveName,
            assignedTeam = assignedTeam
        )
        
        plugin.configManager.addLinkedUser(playerLink)
        
        val embedBuilder = EmbedBuilder()
            .setTitle("✅ Account Linked Successfully!")
            .setDescription("Your Discord account has been linked to **${pendingLink.minecraftUsername}**")
            .addField("Minecraft Username", pendingLink.minecraftUsername, true)
            .addField("Discord User", event.user.effectiveName, true)
            .addField("Assigned Team", assignedTeam ?: "None", true)
            .setColor(lightEmbedColor)
            .setThumbnail(PlayerAvatarUtil.getPlayerBustUrl(pendingLink.minecraftUuid))
            .setTimestamp(Instant.now())
            .setFooter("MaxyGames LinkBot", null)

        val embed = embedBuilder.build()
        
        event.replyEmbeds(embed).queue()
        
        plugin.logger.info("Successfully linked ${pendingLink.minecraftUsername} (${pendingLink.minecraftUuid}) to Discord user ${event.user.effectiveName} (${event.user.id})")
    }
    
    private fun handleLinkedUserTeamsCommand(event: SlashCommandInteractionEvent) {
        val linkedUsers = plugin.configManager.getAllLinkedUsers()
        
        if (linkedUsers.isEmpty()) {
            event.reply("No users are currently linked!").setEphemeral(true).queue()
            return
        }
        
        val embed = EmbedBuilder()
            .setTitle("Linked Users & Teams")
            .setDescription("All users who have linked their Minecraft and Discord accounts")
            .setColor(Color.BLUE)
            .setTimestamp(Instant.now())
            .setFooter("MaxyGames LinkBot • ${linkedUsers.size} linked users", null)
        
        val usersByTeam = linkedUsers.groupBy { it.assignedTeam ?: "No Team" }
        
        usersByTeam.forEach { (team, users) ->
            val userList = users.joinToString("\n") { user ->
                "• **${user.minecraftUsername}** (<@${user.discordUserId}>)"
            }
            
            embed.addField(
                "$team (${users.size})",
                userList,
                false
            )
        }
        
        event.replyEmbeds(embed.build()).queue()
    }
    
    private fun handleWhoisCommand(event: SlashCommandInteractionEvent) {
        val userOption = event.getOption("user")?.asUser
        val playerOption = event.getOption("player")?.asString
        
        if (userOption == null && playerOption == null) {
            event.reply("❌ Provide a Discord user or a Minecraft username/UUID.").setEphemeral(true).queue()
            return
        }
        
        var link: PlayerLink? = null
        
        if (userOption != null) {
            link = plugin.configManager.getLinkedUserByDiscordId(userOption.id)
        }
        
        if (link == null && playerOption != null) {
            val uuid: UUID? = try { UUID.fromString(playerOption) } catch (_: Exception) { null }
            link = if (uuid != null) {
                plugin.configManager.getLinkedUser(uuid)
            } else {
                plugin.configManager.getAllLinkedUsers()
                    .find { it.minecraftUsername.equals(playerOption, ignoreCase = true) }
            }
        }
        
        if (link == null) {
            event.reply("No link found.").setEphemeral(true).queue()
            return
        }
        
        val member = guild?.getMemberById(link.discordUserId)
        val discordDisplay = member?.effectiveName ?: link.discordUsername
        val assignedTeam = link.assignedTeam ?: "None"
        
        val embed = EmbedBuilder()
            .setTitle("Link Lookup")
            .setDescription("Minecraft ⇄ Discord association")
            .addField("Minecraft", "**${link.minecraftUsername}** (\`${link.minecraftUuid}\`)", false)
            .addField("Discord", "<@${link.discordUserId}> (${discordDisplay})", false)
            .addField("Assigned Team", assignedTeam, true)
            .addField("Linked At", "<t:${link.linkedAt.epochSecond}:R>", true)
            .setColor(lightEmbedColor)
            .setThumbnail(PlayerAvatarUtil.getPlayerBustUrl(link.minecraftUuid))
            .setTimestamp(Instant.now())
            .setFooter("MaxyGames LinkBot", null)
            .build()
        
        event.replyEmbeds(embed).queue()
    }
    

}
