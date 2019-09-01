package com.myskng.megmusicbot.test.provider

import com.myskng.megmusicbot.bot.music.RawOpusStreamProvider
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
import org.koin.core.KoinComponent
import java.io.File
import java.io.IOException

class FileProviderTest : KoinComponent, AbstractDefaultTester() {
    @Test
    fun canReadLocalFile() = runBlocking {
        val testFilePath = "./rawopus.blob"
        val fileByteArray = File(testFilePath).source().buffer().readByteArray()
        val audioManager = RawOpusStreamProvider()
        val provider = LocalFileProvider(audioManager, testFilePath)
        provider.onError = ProviderTestUtil.rethrowError
        GlobalScope.async { provider.startStream() }
        withTimeout(5000) {
            while (audioManager.encodedDataInputStream == null && !provider.isOriginStreamAlive && isActive) {
                delay(10)
            }
        }
        // 大前提としてファイルが空っぽでない事が必須
        Assertions.assertTrue(fileByteArray.isNotEmpty())
        // 0xFC 0xFF 0xFE 以外の結果が返ってくればOK
        Assertions.assertTrue(audioManager.provide())
        val testArray = ByteArray(audioManager.buffer.remaining())
        audioManager.buffer.get(testArray)
        Assertions.assertFalse(ProviderTestUtil.silentSoundArray contentEquals testArray)
        provider.stopStream()
    }

    @Test
    fun canReadHTTPRemoteFile() = runBlocking {
        val server = TestHTTPServer()
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true)
        val fileByteArray = File("./rawopus.blob").source().buffer().readByteArray()
        val testFileURL = "http://127.0.0.1:8888/rawopus.blob"
        val audioManager = RawOpusStreamProvider()
        val provider = HttpFileProvider(audioManager, testFileURL)
        provider.onError = ProviderTestUtil.rethrowError
        GlobalScope.async { provider.startStream() }
        withTimeout(5000) {
            while (audioManager.encodedDataInputStream == null && !provider.isOriginStreamAlive && isActive) {
                delay(10)
            }
        }
        // 大前提としてファイルが空っぽでない事が必須
        Assertions.assertTrue(fileByteArray.isNotEmpty())
        // 0xFC 0xFF 0xFE 以外の結果が返ってくればOK
        Assertions.assertTrue(audioManager.provide())
        val testArray = ByteArray(audioManager.buffer.remaining())
        audioManager.buffer.get(testArray)
        Assertions.assertFalse(ProviderTestUtil.silentSoundArray contentEquals testArray)
        provider.stopStream()
        server.stop()
    }

    @Test
    fun errorOnNonExistFile() = runBlocking {
        val audioManager = RawOpusStreamProvider()
        val provider = LocalFileProvider(audioManager, "./DOES-NOT-EXIST-FILE")
        var errorDetected = false
        provider.onError = { exception ->
            Assertions.assertTrue(exception is IOException)
            errorDetected = true
        }
        async {
            try {
                provider.startStream()
            } catch (ex: CancellationException) {
                // dismiss this
            }
        }
        withTimeout(5000) {
            while (errorDetected.not() && isActive) {
                delay(10)
            }
        }
        provider.stopStream()
    }
}