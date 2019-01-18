package com.myskng.megmusicbot.bot

import com.myskng.megmusicbot.exception.CommandSyntaxException
import com.myskng.megmusicbot.store.BotConfig
import com.myskng.megmusicbot.store.BotStateStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.IListener
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent
import java.util.logging.Level
import java.util.logging.Logger

class BotConnectionManager : KoinComponent {
    val store by inject<BotStateStore>()
    val logger by inject<Logger>()
    val botCommand by inject<BotCommand>()
    val config by inject<BotConfig>()
    lateinit var discordClient: IDiscordClient

    fun initializeBotConnection() {
        val clientBuilder = ClientBuilder()
        clientBuilder.withToken(config.discordAPIKey)
        discordClient = clientBuilder.login()
        arrayOf(onReady, onMessageReceive, onBotOnlyOnVoiceChannelEvent).forEach {
            discordClient.dispatcher.registerListener(it)
        }
    }

    private val onReady = IListener<ReadyEvent> {
        logger.log(Level.INFO, "[BotConnectionManager] Discord connected.")
    }

    private val onMessageReceive = IListener<MessageReceivedEvent> { event ->
        GlobalScope.async {
            if (botCommand.isBotCommand(event.message.content)) {
                try {
                    botCommand.onCommandRecive(event.message.content, event)
                } catch (ex: CommandSyntaxException) {
                    event.channel.sendMessage(ex.message)
                }
            }
        }
    }

    private val onBotOnlyOnVoiceChannelEvent = IListener<UserVoiceChannelLeaveEvent> { event ->
        if (event.voiceChannel.connectedUsers.count() == 1 && event.voiceChannel.isConnected) {
            store.songQueue.stop()
            event.voiceChannel.leave()
        }
    }
}