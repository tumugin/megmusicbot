package com.myskng.megmusicbot.bot

import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import picocli.CommandLine
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

class BotCommand : KoinComponent {
    private val processor by inject<BotCommandProcessor>()

    class DiscordCommandLine {
        @CommandLine.Option(names = ["--title"])
        var title: String? = null
        @CommandLine.Option(names = ["--artist"])
        var artist: String? = null
        @CommandLine.Option(names = ["--album"])
        var album: String? = null
        @CommandLine.Option(names = ["/help"])
        var isHelp = false
        @CommandLine.Option(names = ["/join"])
        var isJoin = false
        @CommandLine.Option(names = ["/leave"])
        var isLeave = false
        @CommandLine.Option(names = ["/search"])
        var isSearch = false
        @CommandLine.Option(names = ["/play"])
        var play: Int = -1
        @CommandLine.Option(names = ["/queue"])
        var isQueue = false
    }

    companion object {
        fun splitCommandToArray(command: String): Array<String> {
            val regex = Regex("\"(\"|(?!\").)+\"|[^ ]+")
            val resultList = mutableListOf<String>()
            regex.findAll(command).forEach {
                var match = it.value
                match = match.removePrefix("\"")
                match = match.removeSuffix("\"")
                resultList.add(match)
            }
            return resultList.toTypedArray()
        }
    }

    suspend fun onCommandRecive(command: String, event: MessageReceivedEvent) {
        val discordCommandLine = DiscordCommandLine()
        CommandLine(discordCommandLine).parse(*splitCommandToArray(command))
        when {
            discordCommandLine.isHelp -> {
                processor.outputHelpText(event)
            }
            discordCommandLine.isJoin -> {
                processor.joinVoiceChannel(event)
            }
            discordCommandLine.isLeave -> {
                processor.leaveVoiceChannel(event)
            }
            discordCommandLine.isSearch -> {

            }
            discordCommandLine.play != -1 -> {

            }
            discordCommandLine.isQueue -> {

            }
        }
    }
}