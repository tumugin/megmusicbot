package com.myskng.megmusicbot.bot

import com.myskng.megmusicbot.text.DefaultLangStrings
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

class BotCommand : KoinComponent {
    private val botStrings by inject<DefaultLangStrings>()
    fun onCommandRecive(command: String, arg: String, event: MessageReceivedEvent) {
        when (command) {
            "help" -> {
                event.channel.sendMessage(botStrings.botHelpText)
            }
            "playurl" ->{

            }
        }
    }
}