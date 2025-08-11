package me.andrew.maxyGamesLinkBot

import me.andrew.maxyGamesLinkBot.commands.AssignTeamCommand
import me.andrew.maxyGamesLinkBot.commands.LinkCommand
import me.andrew.maxyGamesLinkBot.config.ConfigManager
import me.andrew.maxyGamesLinkBot.discord.DiscordBot
import me.andrew.maxyGamesLinkBot.manager.LinkCodeManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class MaxyGamesLinkBot : JavaPlugin() {
    
    lateinit var configManager: ConfigManager
        private set
    
    lateinit var discordBot: DiscordBot
        private set
    
    private lateinit var linkCodeManager: LinkCodeManager
    
    override fun onEnable() {
        logger.info("> MaxyGames LinkBot is starting up... - SEND TO ANDREW IF ERRORS.")
        saveDefaultFiles()
        configManager = ConfigManager(dataFolder)
        linkCodeManager = LinkCodeManager()
        discordBot = DiscordBot(this, linkCodeManager)
        registerCommands()
        discordBot.start()
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            linkCodeManager.performCleanup()
        }, 6000L, 6000L)
        logger.info("> MaxyGames LinkBot ENABLED :)")
    }
    
    override fun onDisable() {
        logger.info("> MaxyGames LinkBot is shutting down...")
        if (::discordBot.isInitialized) {
            discordBot.shutdown()
        }
        Bukkit.getScheduler().cancelTasks(this)
        logger.info("> MaxyGames LinkBot DISABLED :(")
    }
    
    private lateinit var assignTeamCommand: AssignTeamCommand
    
    private fun registerCommands() {
        getCommand("link")?.setExecutor(LinkCommand(this, linkCodeManager))
        assignTeamCommand = AssignTeamCommand(this)
        getCommand("assignteam")?.setExecutor(assignTeamCommand)
        logger.info("Minecraft commands registered successfully!")
    }
    
    fun getAssignTeamCommand(): AssignTeamCommand = assignTeamCommand

    private fun saveDefaultFiles() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val config = File(dataFolder, "config.yml")
        if (!config.exists()) {
            saveResource("config.yml", false)
        }
        val roleMappings = File(dataFolder, "role-mappings.yml")
        if (!roleMappings.exists()) {
            saveResource("role-mappings.yml", false)
        }
    }
}
