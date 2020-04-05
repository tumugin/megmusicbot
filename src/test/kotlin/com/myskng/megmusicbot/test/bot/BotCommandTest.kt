package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.BotCommand
import com.myskng.megmusicbot.bot.BotCommandProcessor
import discord4j.core.event.domain.message.MessageCreateEvent
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class BotCommandTest {
    @ParameterizedTest
    @ArgumentsSource(TestBotCommands::class)
    fun testIsBotCommand(testCommand: String, result: Boolean) {
        val botCommand = BotCommand()
        Assert.assertEquals(result, botCommand.isBotCommand(testCommand))
    }

    private class TestBotCommands : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val testCommandsAndResult = listOf(
                Pair("/help", true),
                Pair("/join", true),
                Pair("/leave", true),
                Pair("/search", true),
                Pair("/play", true),
                Pair("/queue", true),
                Pair("/skip", true),
                Pair("/now", true),
                Pair("/nowplaying", true),
                Pair("上田麗奈", false),
                Pair("やっぱ白石晴香なんだよなぁ...", false)
            )
            val botReplyString = "<@BOT_USER#0000>"
            val testCommandsAndResultWithReplyString =
                testCommandsAndResult.map { item -> Pair("$botReplyString ${item.first}", item.second) }
            return Stream.of(*(testCommandsAndResult + testCommandsAndResultWithReplyString).map { item ->
                Arguments.arguments(
                    item.first,
                    item.second
                )
            }.toTypedArray())
        }
    }

    @ParameterizedTest
    @ArgumentsSource(TestBotCommandsWithMockActions::class)
    fun testOnCommandRecieve(
        command: String,
        verifyFunc: suspend (String, BotCommand, BotCommandProcessor, MessageCreateEvent) -> Unit
    ) = runBlockingTest {
        stopKoin()
        val mockBotCommandProcessor = startKoinWithMockBotCommandProcessor()
        val botCommand = BotCommand()
        val mockMessageCreateEvent = mockk<MessageCreateEvent>()
        verifyFunc(command, botCommand, mockBotCommandProcessor, mockMessageCreateEvent)
    }

    private class TestBotCommandsWithMockActions : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val testList =
                listOf<Pair<String, suspend (String, BotCommand, BotCommandProcessor, MessageCreateEvent) -> Unit>>(
                    // ヘルプコマンド
                    Pair("/help") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        coVerify {
                            cmdP.outputHelpText(mesg)
                        }
                    },
                    // 参加コマンド
                    Pair("/join") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        coVerify {
                            cmdP.joinVoiceChannel(mesg)
                        }
                    },
                    // 離脱コマンド
                    Pair("/leave") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        coVerify {
                            cmdP.leaveVoiceChannel(mesg)
                        }
                    },
                    // TODO: 検索コマンドと再生コマンドは厄介そうなのであとでやる
                    // キューコマンド
                    Pair("/queue") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        coVerify {
                            cmdP.printQueue(mesg)
                        }
                    },
                    // スキップコマンド
                    Pair("/skip") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        coVerify {
                            cmdP.skipSong()
                        }
                    },
                    // 再生中コマンド
                    Pair("/now") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        coVerify {
                            cmdP.isNowPlaying(mesg)
                        }
                    },
                    Pair("/nowplaying") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        coVerify {
                            cmdP.isNowPlaying(mesg)
                        }
                    }
                )
            val botReplyString = "<@BOT_USER#0000>"
            val testCommandsAndFuncWithReplyString =
                testList.map { item -> Pair("$botReplyString ${item.first}", item.second) }
            return Stream.of(*(testList + testCommandsAndFuncWithReplyString).map { item ->
                Arguments.arguments(
                    item.first,
                    item.second
                )
            }.toTypedArray())
        }
    }

    private fun startKoinWithMockBotCommandProcessor(): BotCommandProcessor {
        val mockBotCommandProcessor = mockk<BotCommandProcessor>(relaxed = true)
        startKoin {
            modules(
                module {
                    single<BotCommandProcessor> {
                        mockBotCommandProcessor
                    }
                }
            )
        }
        return mockBotCommandProcessor
    }
}
