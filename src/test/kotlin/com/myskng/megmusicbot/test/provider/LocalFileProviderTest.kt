package com.myskng.megmusicbot.test.provider

import com.myskng.megmusicbot.encoder.IEncoderProcess
import com.myskng.megmusicbot.provider.LocalFileProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.*
import okio.Buffer
import okio.buffer
import okio.source
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.startKoin
import sx.blah.discord.handle.audio.IAudioProvider
import sx.blah.discord.handle.audio.impl.AudioManager
import sx.blah.discord.util.audio.providers.AudioInputStreamProvider
import java.io.BufferedInputStream
import java.io.File

class LocalFileProviderTest {
    @BeforeEach
    fun setupKoin() {
        val modules = module {
            factory {
                val buffer = Buffer()
                mock<IEncoderProcess> {
                    on { isProcessAlive } doReturn true
                    on { stdInputStream } doReturn buffer.outputStream()
                    on { stdOutputStream } doReturn BufferedInputStream(buffer.inputStream())
                }
            }
        }
        startKoin(listOf(modules))
    }

    @Test
    fun fileReadTest() {
        var audioProvider: IAudioProvider? = null
        val audioManager = mock<AudioManager> {
            on { setAudioProvider(any()) }.then {
                audioProvider = it.arguments.first() as IAudioProvider
                Unit
            }
            on { getAudioProvider() }.thenAnswer {
                audioProvider
            }
        }
        val testFilePath = "./test2.flac"
        val fileByteArray = File(testFilePath).source().buffer().readByteArray()
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
}