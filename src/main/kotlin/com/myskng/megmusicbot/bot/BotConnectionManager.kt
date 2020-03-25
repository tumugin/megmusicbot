package com.myskng.megmusicbot.bot

import com.myskng.megmusicbot.exception.CommandSyntaxException
import com.myskng.megmusicbot.store.BotConfig
import discord4j.core.DiscordClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class BotConnectionManager : KoinComponent, CoroutineScope {
    private val logger by inject<Logger>()
    private val botCommands = mutableMapOf<String, BotCommand>()
    private val config by inject<BotConfig>()
    private lateinit var discordClient: DiscordClient

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + get<Job>()

    suspend fun initializeBotConnection() {
        discordClient = DiscordClientBuilder(config.discordApiKey).build()
        discordClient.eventDispatcher.on(ReadyEvent::class.java).subscribe(::onReady)
        discordClient.eventDispatcher.on(MessageCreateEvent::class.java).subscribe(::onMessageReceive)
        discordClient.eventDispatcher.on(VoiceStateUpdateEvent::class.java).subscribe(::onBotOnlyOnVoiceChannelEvent)
        discordClient.login().awaitSingle()
    }

    private fun onReady(event: ReadyEvent) {
        logger.log(Level.INFO, "[BotConnectionManager] Discord connected.")
    }


    private fun onMessageReceive(event: MessageCreateEvent) {
        launch {
            val guild = event.guild.awaitSingle()
            val botCommand = botCommands.getOrPut(guild.id.asString()) { get() }
            if (botCommand.isBotCommand(event.message.content.orElse(""))) {
                try {
                    botCommand.onCommandRecive(event.message.content.orElse(""), event)
                } catch (ex: CommandSyntaxException) {
                    event.message.channel.awaitSingle().createMessage(ex.message ?: "Unknown Error").awaitSingle()
                } catch (ex: Exception) {
                    logger.log(Level.SEVERE, "[ERROR] $ex")
                }
            }
        }
    }

    private fun onBotOnlyOnVoiceChannelEvent(event: VoiceStateUpdateEvent) {
        launch {
            val voiceChannel = event.current.channel.awaitSingle()
            if (voiceChannel.voiceStates.count().awaitFirst() == 1L && voiceChannel.voiceStates.map {
                    it.user.map { user -> user.isBot }
                }.any { true }.awaitSingle()) {
                val botCommand = botCommands.getOrPut(event.current.guildId.asString()) { get() }
                // TODO: VoiceConnectionを使わないと切断できないらしい
                botCommand.processor.leaveVoiceChannel(event)
            }
        }
    }
}
