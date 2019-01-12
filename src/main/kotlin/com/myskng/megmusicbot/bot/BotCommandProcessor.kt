package com.myskng.megmusicbot.bot

import com.myskng.megmusicbot.store.BotStateStore
import com.myskng.megmusicbot.text.DefaultLangStrings
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

class BotCommandProcessor : KoinComponent {
    val store by inject<BotStateStore>()
    private val botStrings by inject<DefaultLangStrings>()

    open fun outputHelpText(event: MessageReceivedEvent) {
        event.channel.sendMessage(botStrings.botHelpText)
    }

    open suspend fun joinVoiceChannel(event: MessageReceivedEvent) {
        val client = event.client
        val channel = client.voiceChannels.filter { it.connectedUsers.contains(event.author) }.firstOrNull()
        if (channel != null) {
            channel.join()
            store.songQueue.playQueue(channel.guild.audioManager)
        }
    }

    open fun leaveVoiceChannel(event: MessageReceivedEvent) {
        event.client.connectedVoiceChannels.firstOrNull()?.leave()
    }
}