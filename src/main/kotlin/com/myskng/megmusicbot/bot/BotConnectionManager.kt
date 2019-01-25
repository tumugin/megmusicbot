package com.myskng.megmusicbot.bot

import com.myskng.megmusicbot.exception.CommandSyntaxException
import com.myskng.megmusicbot.store.BotConfig
import kotlinx.coroutines.*
import org.koin.standalone.KoinComponent
import org.koin.standalone.get
import org.koin.standalone.inject
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.IListener
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class BotConnectionManager : KoinComponent, CoroutineScope {
    private val logger by inject<Logger>()
    private val botCommands = mutableMapOf<String, BotCommand>()
    private val config by inject<BotConfig>()
    private lateinit var discordClient: IDiscordClient

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + get<Job>()

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

    val onMessageReceive = IListener<MessageReceivedEvent> { event ->
        launch {
            val botCommand = botCommands.getOrPut(event.guild.stringID, get())
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
            val botCommand = botCommands.getOrPut(event.guild.stringID, get())
            botCommand.processor.leaveVoiceChannel(event)
        }
    }
}