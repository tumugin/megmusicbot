package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.BotCommandProcessor
import com.myskng.megmusicbot.database.SongSearch
import com.myskng.megmusicbot.store.BotConfig
import com.myskng.megmusicbot.store.BotStateStore
import com.myskng.megmusicbot.text.DefaultLangStrings
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.dsl.module.module
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BotCommandProcessorTest : KoinComponent {
    @BeforeAll
    fun setupKoin() {
        val modules = module {
            factory {
                val store = BotStateStore()
                store.config = BotConfig("", "", "", arrayOf("."))
                store
            }
            single {
                DefaultLangStrings()
            }
            single {
                SongSearch()
            }
        }
        StandAloneContext.startKoin(listOf(modules))
    }

    @AfterAll
    fun shutdownKoin() {
        StandAloneContext.stopKoin()
    }

    @Test
    fun testOutputHelpText() {
        val botCommandProcessor = BotCommandProcessor()
        val mockEvent = mock<MessageReceivedEvent> {
            on { channel }.thenAnswer {
                mock<IChannel> {
                    on { sendMessage(any<String>()) }
                }
            }
        }
    }
}