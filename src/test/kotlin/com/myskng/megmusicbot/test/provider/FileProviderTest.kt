package com.myskng.megmusicbot.test.provider

import com.myskng.megmusicbot.provider.HttpFileProvider
import com.myskng.megmusicbot.provider.LocalFileProvider
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import com.myskng.megmusicbot.test.server.TestHTTPServer
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.*
import okio.buffer
import okio.source
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.standalone.KoinComponent
import org.koin.standalone.get
import sx.blah.discord.handle.audio.impl.AudioManager
import sx.blah.discord.util.audio.providers.AudioInputStreamProvider
import java.io.File
import java.io.IOException

class FileProviderTest : KoinComponent, AbstractDefaultTester() {
    @Test
    fun canReadLocalFile() {
        val audioManager = get<AudioManager>()
        val testFilePath = "./test2.flac"
        val fileByteArray = File("./test2.flac").source().buffer().readByteArray()
        val provider = LocalFileProvider(audioManager, testFilePath)
        GlobalScope.async { provider.startStream() }
        runBlocking {
            withTimeout(5000) {
                while (audioManager.audioProvider == null && isActive) {
                    delay(10)
                }
            }
        }
        val discordAudioProvider = audioManager.audioProvider as AudioInputStreamProvider
        //Decoded stream should be bigger than original file
        Assertions.assertTrue(fileByteArray.size < discordAudioProvider.stream.readAllBytes().size)
    }

    @Test
    fun canReadHTTPRemoteFile() {
        val server = TestHTTPServer()
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true)
        val fileByteArray = File("./test2.flac").source().buffer().readByteArray()
        val testFileURL = "http://127.0.0.1:8888/test2.flac"
        val audioManager = get<AudioManager>()
        val provider = HttpFileProvider(audioManager, testFileURL)
        GlobalScope.async { provider.startStream() }
        runBlocking {
            withTimeout(5000) {
                while (audioManager.audioProvider == null && isActive) {
                    delay(10)
                }
            }
        }
        val discordAudioProvider = audioManager.audioProvider as AudioInputStreamProvider
        //Decoded stream should be bigger than original file
        Assertions.assertTrue(fileByteArray.size < discordAudioProvider.stream.readAllBytes().size)
        server.stop()
    }

    @Test
    fun errorOnNonExistFile() {
        val provider = LocalFileProvider(get<AudioManager>(), "./DOES-NOT-EXIST-FILE")
        var errorDetected = false
        provider.onError = { exception ->
            Assertions.assertTrue(exception is IOException)
            errorDetected = true
        }
        runBlocking {
            withTimeout(5000) {
                provider.startStream()
                while (errorDetected.not()) {
                    delay(10)
                }
            }
        }
    }
}