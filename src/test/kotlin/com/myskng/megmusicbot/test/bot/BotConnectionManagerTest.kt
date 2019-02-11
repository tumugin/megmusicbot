package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.BotCommand
import com.myskng.megmusicbot.bot.BotConnectionManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.StandAloneContext.stopKoin
import org.mockito.Answers
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import java.util.logging.Logger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    @Test
    fun botCommandIsolationTest() = runBlocking {
        var numberCounter = 0
        val job = Job()
        val botCommandList = mutableListOf<BotCommand>()
        setupKoin(module {
            factory {
                val item = mock<BotCommand>(defaultAnswer = Answers.RETURNS_DEEP_STUBS) {
                    on { isBotCommand(any()) }.thenReturn(true)
                }
                botCommandList.add(item)
                item
            }
            single {
                job
            }
        })
        val mockMessageReceivedEvent = mock<MessageReceivedEvent>(defaultAnswer = Answers.RETURNS_DEEP_STUBS) {
            on { guild }.thenAnswer {
                mock<IGuild>(defaultAnswer = Answers.RETURNS_DEEP_STUBS) {
                    on { stringID }.thenAnswer { (++numberCounter).toString() }
                }
            }
            on { message }.thenAnswer {
                mock<IMessage> {
                    on { content }.thenAnswer { "hoge message" }
                }
            }
        }
        val botConnectionManager = BotConnectionManager()
        botConnectionManager.onMessageReceive.handle(mockMessageReceivedEvent)
        botConnectionManager.onMessageReceive.handle(mockMessageReceivedEvent)
        job.children.forEach {
            it.join()
        }
        botCommandList.forEach {
            verify(it, times(1)).isBotCommand(any())
            verify(it, times(1)).onCommandRecive(any(), any())
        }
    }
}