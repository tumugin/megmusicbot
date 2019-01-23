package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.BotCommand
import com.myskng.megmusicbot.bot.BotCommandProcessor
import com.myskng.megmusicbot.database.SearchQuery
import com.myskng.megmusicbot.database.SongSearch
import com.myskng.megmusicbot.database.SongSearchType
import com.myskng.megmusicbot.exception.CommandSyntaxException
import com.myskng.megmusicbot.store.BotStateStore
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import com.myskng.megmusicbot.text.DefaultLangStrings
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.koin.dsl.module.module
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import kotlin.properties.Delegates
import kotlin.reflect.jvm.reflect

class BotCommandTest : AbstractDefaultTester() {
    private lateinit var mockBotCommandProcessor: MockBotCommandProcessor

    @BeforeAll
    override fun setupKoin() {
        additionalKoinModules.add(module {
            single {
                mock<BotStateStore>()
            }
            single {
                mock<DefaultLangStrings>()
            }
            single {
                mock<SongSearch>()
            }
            factory {
                mockBotCommandProcessor = MockBotCommandProcessor()
                mockBotCommandProcessor as BotCommandProcessor
            }
        })
        super.setupKoin()
    }

    @Test
    fun testDiscordCommandLineIsSearchMode() {
        // test with default value
        val discordCommandLineTestCaseA = BotCommand.DiscordCommandLine()
        Assertions.assertFalse(discordCommandLineTestCaseA.isSearchMode)
        val discordCommandLineTestCaseB = BotCommand.DiscordCommandLine()
        discordCommandLineTestCaseB.also {
            it.isSearch = true
        }
        Assertions.assertTrue(discordCommandLineTestCaseB.isSearchMode)
        val discordCommandLineTestCaseC = BotCommand.DiscordCommandLine()
        discordCommandLineTestCaseC.also {
            it.title = "hogehoge"
        }
        Assertions.assertTrue(discordCommandLineTestCaseC.isSearchMode)
    }

    @Test
    fun testCommandSplitter() {
        val testText = "/title 楽園 /artist 関裕美"
        val testArray = arrayOf("/title", "楽園", "/artist", "関裕美")
        val splittedArray = BotCommand().splitCommandToArray(testText)
        Assertions.assertArrayEquals(testArray, splittedArray)
    }

    @Test
    fun testCommandSplitterWithQuote() {
        val testText = "/title \"Last Kiss\" /artist 三船美優"
        val testArray = arrayOf("/title", "Last Kiss", "/artist", "三船美優")
        val splittedArray = BotCommand().splitCommandToArray(testText)
        Assertions.assertArrayEquals(testArray, splittedArray)
    }

    class MockBotCommandProcessor : BotCommandProcessor() {
        var calledFunctionName by Delegates.observable<String?>(null) { _, old, _ ->
            if (old != null) {
                throw AssertionError("Wrong function might be called!!")
            }
        }
        lateinit var searchSongQueryList: Array<SearchQuery>
        var playSongIndex: Int = -1

        override fun outputHelpText(event: MessageReceivedEvent) {
            calledFunctionName = this::outputHelpText.name
        }

        override suspend fun joinVoiceChannel(event: MessageReceivedEvent) {
            calledFunctionName = this::joinVoiceChannel.name
        }

        override fun leaveVoiceChannel(event: MessageReceivedEvent) {
            val func: (MessageReceivedEvent) -> Unit = this::leaveVoiceChannel
            calledFunctionName = func.reflect()!!.name
        }

        override fun searchSong(query: Array<SearchQuery>, event: MessageReceivedEvent) {
            calledFunctionName = this::searchSong.name
            searchSongQueryList = query
        }

        override fun playSong(playIndex: Int, event: MessageReceivedEvent) {
            calledFunctionName = this::playSong.name
            playSongIndex = playIndex
        }

        override fun printQueue(event: MessageReceivedEvent) {
            calledFunctionName = this::printQueue.name
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["/help", "/join", "/leave", "/queue"])
    fun testBotCommand(command: String) = runBlocking {
        val botCommand = BotCommand()
        botCommand.onCommandRecive(command, mock())
        when (command) {
            "/help" -> Assertions.assertEquals(
                mockBotCommandProcessor::outputHelpText.name,
                mockBotCommandProcessor.calledFunctionName
            )
            "/join" -> Assertions.assertEquals(
                mockBotCommandProcessor::joinVoiceChannel.name,
                mockBotCommandProcessor.calledFunctionName
            )
            "/leave" -> {
                val func: (MessageReceivedEvent) -> Unit = mockBotCommandProcessor::leaveVoiceChannel
                Assertions.assertEquals(
                    func.reflect()!!.name,
                    mockBotCommandProcessor.calledFunctionName
                )
            }
            "/queue" -> Assertions.assertEquals(
                mockBotCommandProcessor::printQueue.name,
                mockBotCommandProcessor.calledFunctionName
            )
        }
    }

    @Test
    fun testSearchSongCommand() = runBlocking {
        val botCommand = BotCommand()
        val testArtist = "上田麗奈"
        val testAlbum = "sleepland"
        val testTitle = "誰もわたしを知らない世界へ"
        botCommand.onCommandRecive("/search /artist $testArtist /album $testAlbum /title $testTitle", mock())
        mockBotCommandProcessor.searchSongQueryList.forEach {
            when (it.type) {
                SongSearchType.Title -> Assertions.assertEquals(testTitle, it.searchString)
                SongSearchType.Artist -> Assertions.assertEquals(testArtist, it.searchString)
                SongSearchType.Album -> Assertions.assertEquals(testAlbum, it.searchString)
            }
        }
    }

    @Test
    fun testPlayCommand() = runBlocking {
        val botCommand = BotCommand()
        botCommand.onCommandRecive("/play 10", mock())
        Assertions.assertEquals(mockBotCommandProcessor::playSong.name, mockBotCommandProcessor.calledFunctionName)
        Assertions.assertEquals(9, mockBotCommandProcessor.playSongIndex)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/play ueshamaaaa", "/search hogehoge", "/search /artist hoge /artist hoge", "/search"])
    fun testCommandSyntaxException(command: String) = runBlocking {
        val botCommand = BotCommand()
        try {
            botCommand.onCommandRecive(command, mock())
            throw AssertionError("No exception raised.")
        } catch (ex: Exception) {
            Assertions.assertTrue(ex is CommandSyntaxException)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["/help", "/join", "/leave", "/queue", "/play", "/search", "/artist", "/album", "/title"])
    fun testIsBotCommand(command: String) {
        Assertions.assertTrue(BotCommand().isBotCommand(command))
    }

    @ParameterizedTest
    @ValueSource(strings = ["上田麗奈", "/nanyaine", "白石紬 /help"])
    fun testIsNotBotCommand(command: String) {
        Assertions.assertFalse(BotCommand().isBotCommand(command))
    }
}