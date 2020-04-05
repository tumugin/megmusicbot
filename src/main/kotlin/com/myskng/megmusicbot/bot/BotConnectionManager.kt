package com.myskng.megmusicbot.bot

import com.myskng.megmusicbot.config.BotConfig
import com.myskng.megmusicbot.exception.CommandSyntaxException
import discord4j.core.DiscordClient
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
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
    private lateinit var botUserId: Snowflake

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + get<Job>()

    suspend fun initializeBotConnection() {
        discordClient = DiscordClient.create(config.discordApiKey)
        val eventDispatcher = discordClient.login().awaitSingle().eventDispatcher
        botUserId = Snowflake.of(discordClient.self.awaitSingle().id())
        eventDispatcher.on(ReadyEvent::class.java).subscribe(::onReady)
        eventDispatcher.on(MessageCreateEvent::class.java).subscribe(::onMessageReceive)
        eventDispatcher.on(VoiceStateUpdateEvent::class.java).subscribe(::onBotOnlyOnVoiceChannelEvent)
    }

    private fun onReady(event: ReadyEvent) {
        logger.log(Level.INFO, "[BotConnectionManager] Discord connected.")
    }


    private fun onMessageReceive(event: MessageCreateEvent) {
        launch {
            val guild = event.guild.awaitSingle()
            val botCommand = botCommands.getOrPut(guild.id.asString()) { get() }
            val isMention =
                event.message.userMentionIds.contains(botUserId)
            if (isMention && botCommand.isBotCommand(event.message.content)) {
                try {
                    botCommand.onCommandReceive(event.message.content, event)
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
            if (event.old.isEmpty) {
                return@launch
            }
            val voiceChannel = event.old.get().channel.awaitFirstOrNull()
            if (voiceChannel !== null && voiceChannel.voiceStates.count()
                    .awaitSingle() == 1L && voiceChannel.voiceStates.map {
                    it.user.map { user -> user.isBot }
                }.any { true }.awaitSingle()
            ) {
                val botCommand = botCommands.getOrPut(event.current.guildId.asString()) { get() }
                botCommand.processor.leaveVoiceChannel()
            }
        }
    }
}
