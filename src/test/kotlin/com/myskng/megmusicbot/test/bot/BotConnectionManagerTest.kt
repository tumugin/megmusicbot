package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.BotCommand
import com.myskng.megmusicbot.bot.BotConnectionManager
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.AfterAll
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.StandAloneContext.stopKoin
import org.mockito.Answers
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IGuild
import java.util.logging.Logger

class BotConnectionManagerTest {
    fun setupKoin(additionalModule: Module = module { }) {
        val module = module {
            factory {
                Logger.getLogger(it.javaClass.packageName)
            }
        }
        startKoin(listOf(module, additionalModule))
    }

    @AfterAll
    fun cleanupKoin() {
        stopKoin()
    }

    fun botCommandIsolationTest() {
        var numberCounter = 0
        val botCommandList = mutableListOf<BotCommand>()
        setupKoin(module {
            factory {
                val item = mock<BotCommand>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
                botCommandList.add(item)
                item
            }
        })
        val botConnectionManager = BotConnectionManager()
        val mockMessageReceivedEvent = mock<MessageReceivedEvent>(defaultAnswer = Answers.RETURNS_DEEP_STUBS) {
            on { guild }.thenAnswer {
                mock<IGuild>(defaultAnswer = Answers.RETURNS_DEEP_STUBS) {
                    on { stringID }.thenAnswer { (++numberCounter).toString() }
                }
            }
        }
    }
}