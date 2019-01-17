package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.BotCommandProcessor
import com.myskng.megmusicbot.bot.music.ISong
import com.myskng.megmusicbot.bot.music.LocalSong
import com.myskng.megmusicbot.bot.music.SongQueueManager
import com.myskng.megmusicbot.database.SearchQuery
import com.myskng.megmusicbot.database.SongSearch
import com.myskng.megmusicbot.store.BotStateStore
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import com.myskng.megmusicbot.text.DefaultLangStrings
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.koin.dsl.module.module
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext
import org.koin.standalone.inject
import org.mockito.Mockito
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.*
import java.util.logging.Level
import java.util.logging.Logger

class BotCommandProcessorTest : KoinComponent, AbstractDefaultTester() {
    private lateinit var mockStore: BotStateStore
    private lateinit var mockSongQueueManager: SongQueueManager
    private val songQueue = mutableListOf<ISong>()
    private var currentSearchList: List<ISong>? = null
    private val logger by inject<Logger>()

    @BeforeAll
    override fun setupKoin() {
        val modules = module {
            factory {
                mockStore = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) {
                    on { songQueue }.thenAnswer {
                        mockSongQueueManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) {
                            on { songQueue }.thenAnswer { songQueue }
                        }
                        mockSongQueueManager
                    }
                    on { currentSearchList }.thenAnswer {
                        currentSearchList
                    }
                }
                mockStore
            }
            single {
                DefaultLangStrings()
            }
            single {
                object : SongSearch() {
                    override fun searchSong(query: Array<SearchQuery>): List<LocalSong> {
                        return listOf(
                            LocalSong("ほげ楽曲", "ほげタイトル", "ほげアルバム", "./hoge.flac"),
                            LocalSong("ほげ楽曲2", "ほげタイトル2", "ほげアルバム2", "./hoge.flac")
                        )
                    }
                } as SongSearch
            }
        }
        additionalKoinModules.add(modules)
        super.setupKoin()
    }

    @AfterAll
    fun shutdownKoin() {
        StandAloneContext.stopKoin()
    }

    @BeforeEach
    fun beforeTest() {
        songQueue.clear()
        currentSearchList = null
    }

    @Test
    fun testOutputHelpText() {
        val botCommandProcessor = BotCommandProcessor()
        lateinit var channelMock: IChannel
        val mockEvent = mock<MessageReceivedEvent> {
            on { channel }.thenAnswer {
                channelMock = mock()
                channelMock
            }
        }
        botCommandProcessor.outputHelpText(mockEvent)
        verify(channelMock, atLeastOnce()).sendMessage(any<String>())
    }

    @Test
    fun joinVoiceChannelTest() = runBlocking {
        val botCommandProcessor = BotCommandProcessor()
        val mockUser = mock<IUser> {
            on { longID }.thenAnswer { 12345L }
            on { name }.thenAnswer { "白石紬" }
        }
        val mockVoiceChannel = mock<IVoiceChannel> {
            on { connectedUsers }.thenAnswer { listOf(mockUser) }
            on { guild }.thenAnswer { mock<IGuild>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) }
        }
        val mockClient = mock<IDiscordClient> {
            on { voiceChannels }.thenAnswer { listOf(mockVoiceChannel) }
        }
        val mockChannel = mock<IChannel>()
        val mockEvent = mock<MessageReceivedEvent>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) {
            on { client }.thenAnswer { mockClient }
            on { author }.thenAnswer { mockUser }
            on { channel }.thenAnswer { mockChannel }
        }
        botCommandProcessor.joinVoiceChannel(mockEvent)
        verify(mockVoiceChannel, atLeastOnce()).join()
        verify(mockSongQueueManager, atLeastOnce()).playQueue(any())
    }

    @Test
    fun leaveVoiceChannelTest() {
        val botCommandProcessor = BotCommandProcessor()
        val mockUser = mock<IUser> {
            on { longID }.thenAnswer { 12345L }
            on { name }.thenAnswer { "白石紬" }
        }
        val mockVoiceChannel = mock<IVoiceChannel> {
            on { connectedUsers }.thenAnswer { listOf(mockUser) }
            on { guild }.thenAnswer { mock<IGuild>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) }
        }
        val mockClient = mock<IDiscordClient> {
            on { voiceChannels }.thenAnswer { listOf(mockVoiceChannel) }
            on { connectedVoiceChannels }.thenAnswer { listOf(mockVoiceChannel) }
        }
        val mockChannel = mock<IChannel>()
        val mockEvent = mock<MessageReceivedEvent>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) {
            on { client }.thenAnswer { mockClient }
            on { author }.thenAnswer { mockUser }
            on { channel }.thenAnswer { mockChannel }
        }
        botCommandProcessor.leaveVoiceChannel(mockEvent)
        verify(mockSongQueueManager, atLeastOnce()).stop()
        verify(mockVoiceChannel, atLeastOnce()).leave()
    }

    @Test
    fun searchSongTest() {
        val botCommandProcessor = BotCommandProcessor()
        val mockChannel = mock<IChannel> {
            on { sendMessage(any<String>()) }.thenAnswer { onSendMessage ->
                logger.log(Level.INFO, onSendMessage.getArgument(0) as String)
                mock<IMessage>()
            }
        }
        val mockEvent = mock<MessageReceivedEvent>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) {
            on { channel }.thenAnswer { mockChannel }
        }
        botCommandProcessor.searchSong(emptyArray(), mockEvent)
        verify(mockChannel, atLeastOnce()).sendMessage(any<String>())
    }

    @Test
    fun playSongTest() {
        val botCommandProcessor = BotCommandProcessor()
        currentSearchList = listOf(LocalSong("ほげ楽曲", "ほげタイトル", "ほげアルバム", "./hoge.flac"))
        botCommandProcessor.playSong(0, mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS))
        Assertions.assertEquals(mockStore.currentSearchList.first(), mockSongQueueManager.songQueue.first())
    }

    @Test
    fun printQueueTest() {
        val botCommandProcessor = BotCommandProcessor()
        val mockChannel = mock<IChannel> {
            on { sendMessage(any<String>()) }.thenAnswer { onSendMessage ->
                logger.log(Level.INFO, onSendMessage.getArgument(0) as String)
                mock<IMessage>()
            }
        }
        val mockEvent = mock<MessageReceivedEvent>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) {
            on { channel }.thenAnswer { mockChannel }
        }
        repeat(15) {
            songQueue.add(LocalSong("ほげ楽曲$it", "ほげタイトル$it", "ほげアルバム$it", "./hoge$it.flac"))
        }
        botCommandProcessor.printQueue(mockEvent)
        verify(mockChannel, atLeastOnce()).sendMessage(any<String>())
    }
}