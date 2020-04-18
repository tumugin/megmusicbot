package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.BotCommand
import com.myskng.megmusicbot.bot.BotCommandProcessor
import com.myskng.megmusicbot.database.SearchQuery
import com.myskng.megmusicbot.database.SongSearchType
import discord4j.core.event.domain.message.MessageCreateEvent
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
                Pair("/clear", true),
                Pair("/playall", true),
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
                    // 検索コマンド
                    Pair("/search title ネプテューヌ☆サガして") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        val queryList = mutableListOf(SearchQuery(SongSearchType.Title, "ネプテューヌ☆サガして"))
                            .toTypedArray()
                        coVerify {
                            cmdP.searchSong(queryList, mesg)
                        }
                    },
                    Pair("/search artist 純情のアフィリア") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        val queryList = mutableListOf(SearchQuery(SongSearchType.Artist, "純情のアフィリア"))
                            .toTypedArray()
                        coVerify {
                            cmdP.searchSong(queryList, mesg)
                        }
                    },
                    Pair("/search album ジュンジョウ・ガイドストーン") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        val queryList = mutableListOf(SearchQuery(SongSearchType.Album, "ジュンジョウ・ガイドストーン"))
                            .toTypedArray()
                        coVerify {
                            cmdP.searchSong(queryList, mesg)
                        }
                    },
                    Pair("/search title 魔法のチョコレート伝説 artist 純情のアフィリア album ジュンジョウ・ガイドストーン") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        val queryList = mutableListOf(
                            SearchQuery(SongSearchType.Album, "ジュンジョウ・ガイドストーン"),
                            SearchQuery(SongSearchType.Artist, "純情のアフィリア"),
                            SearchQuery(SongSearchType.Title, "魔法のチョコレート伝説")
                        ).toTypedArray()
                        coVerify {
                            cmdP.searchSong(queryList, mesg)
                        }
                    },
                    // 再生コマンド
                    Pair("/play 1") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        coVerify {
                            cmdP.playSong(0, mesg)
                        }
                    },
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
                    },
                    // キュー全消去コマンド
                    Pair("/clear") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        coVerify {
                            cmdP.clearAllQueue(mesg)
                        }
                    },
                    // 検索結果を全て再生コマンド
                    Pair("/playall") { cmdS, cmd, cmdP, mesg ->
                        cmd.onCommandReceive(cmdS, mesg)
                        coVerify {
                            cmdP.playAllSongs(mesg)
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

    @AfterAll
    fun afterAll() {
        stopKoin()
    }
}
