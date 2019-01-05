package com.myskng.megmusicbot.test.provider

import com.myskng.megmusicbot.encoder.IEncoderProcess
import com.myskng.megmusicbot.provider.HttpFileProvider
import com.myskng.megmusicbot.provider.LocalFileProvider
import com.myskng.megmusicbot.test.server.TestHTTPServer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okio.buffer
import okio.source
import org.junit.jupiter.api.*
import org.koin.dsl.module.module
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.StandAloneContext.stopKoin
import org.koin.standalone.get
import sx.blah.discord.handle.audio.IAudioProvider
import sx.blah.discord.handle.audio.impl.AudioManager
import sx.blah.discord.util.audio.providers.AudioInputStreamProvider
import java.io.BufferedInputStream
import java.io.File
import java.io.PipedInputStream
import java.io.PipedOutputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileProviderTest : KoinComponent {
    @BeforeAll
    fun setupKoin() {
        val modules = module {
            factory {
                val pipedOutputStream = PipedOutputStream()
                val pipedInputStream = PipedInputStream()
                pipedInputStream.connect(pipedOutputStream)
                mock<IEncoderProcess> {
                    on { isProcessAlive } doReturn true
                    on { stdInputStream } doReturn pipedOutputStream
                    on { stdOutputStream } doReturn BufferedInputStream(pipedInputStream)
                }
            }
            factory {
                var audioProvider: IAudioProvider? = null
                mock<AudioManager> {
                    on { setAudioProvider(any()) }.then {
                        audioProvider = it.arguments.first() as IAudioProvider
                        Unit
                    }
                    on { getAudioProvider() }.thenAnswer {
                        audioProvider
                    }
                }
            }
            single {
                OkHttpClient()
            }
        }
        startKoin(listOf(modules))
    }

    @AfterAll
    fun cleanupKoin() {
        stopKoin()
    }

    @Test
    fun fileReadTest() {
        val audioManager = get<AudioManager>()
        val testFilePath = "./test2.flac"
        val fileByteArray = File("./test2.flac").source().buffer().readByteArray()
        val provider = LocalFileProvider(audioManager, testFilePath)
        GlobalScope.async { provider.startStream() }
        runBlocking {
            GlobalScope.async {
                withTimeout(5000) {
                    while (audioManager.audioProvider == null && isActive) {
                        delay(10)
                    }
                }
            }.await()
        }
        val discordAudioProvider = audioManager.audioProvider as AudioInputStreamProvider
        //Decoded stream should be bigger than original file
        Assertions.assertTrue(fileByteArray.size < discordAudioProvider.stream.readAllBytes().size)
    }

    @Test
    fun httpReadTest() {
        val server = TestHTTPServer()
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true)
        val fileByteArray = File("./test2.flac").source().buffer().readByteArray()
        val testFileURL = "http://127.0.0.1:8888/test2.flac"
        val audioManager = get<AudioManager>()
        val provider = HttpFileProvider(audioManager, testFileURL)
        GlobalScope.async { provider.startStream() }
        runBlocking {
            GlobalScope.async {
                withTimeout(5000) {
                    while (audioManager.audioProvider == null && isActive) {
                        delay(10)
                    }
                }
            }.await()
        }
        val discordAudioProvider = audioManager.audioProvider as AudioInputStreamProvider
        //Decoded stream should be bigger than original file
        Assertions.assertTrue(fileByteArray.size < discordAudioProvider.stream.readAllBytes().size)
        server.stop()
    }
}