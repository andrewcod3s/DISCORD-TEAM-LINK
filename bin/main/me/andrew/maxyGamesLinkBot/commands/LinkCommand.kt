package me.andrew.maxyGamesLinkBot.commands

import me.andrew.maxyGamesLinkBot.MaxyGamesLinkBot
import me.andrew.maxyGamesLinkBot.manager.LinkCodeManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object LinkColors {
    val CYAN = TextColor.color(0x14B9CC)
    val CYAN_DARK = TextColor.color(0x108CA0) 
    val CYAN_DARKEST = TextColor.color(0x0B5E6E)
    val WHITE = NamedTextColor.WHITE
    val GRAY = NamedTextColor.GRAY
    val RED = NamedTextColor.RED
    val YELLOW = NamedTextColor.YELLOW
}

class LinkCommand(
    private val plugin: MaxyGamesLinkBot,
    private val linkCodeManager: LinkCodeManager
) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage(
                Component.text("This command can only be used by players!", LinkColors.RED)
            )
            return true
        }

        val player = sender

        val existingLink = plugin.configManager.getLinkedUser(player.uniqueId)
        if (existingLink != null) {
            player.sendMessage(
                Component.text("You are already linked to Discord user: ", LinkColors.YELLOW)
                    .append(Component.text(existingLink.discordUsername, LinkColors.CYAN))
            )
            return true
        }

        val code = if (linkCodeManager.hasActiveCode(player.uniqueId)) {
            linkCodeManager.getCodeForPlayer(player.uniqueId)!!
        } else {
            linkCodeManager.generateCode(player.uniqueId, player.name)
        }

        val horizontalRule = Component.text("───────────────────────────", LinkColors.CYAN_DARK)
        val header = Component.text("Discord Link Code", LinkColors.CYAN_DARKEST)

        val codeLabel = Component.text("Your code: ", LinkColors.WHITE)
        val codeValue = Component.text("`$code`", LinkColors.CYAN)
            .hoverEvent(HoverEvent.showText(Component.text("$code", LinkColors.CYAN)))
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(code))


        player.sendMessage(Component.empty())
        player.sendMessage(horizontalRule)
        player.sendMessage(header)
        player.sendMessage(Component.empty())
        player.sendMessage(codeLabel.append(codeValue))
        player.sendMessage(Component.empty())
        player.sendMessage(Component.empty())
        val linkCmd = Component.text("/link $code", LinkColors.CYAN)
            .hoverEvent(HoverEvent.showText(Component.text("/link $code", LinkColors.CYAN)))
        player.sendMessage(
            Component.text("In Discord, run ", LinkColors.GRAY)
                .append(linkCmd)
        )
        player.sendMessage(Component.text("Expires in 5 minutes", LinkColors.RED))
        player.sendMessage(horizontalRule)
        player.sendMessage(Component.empty())

        return true
    }
}
