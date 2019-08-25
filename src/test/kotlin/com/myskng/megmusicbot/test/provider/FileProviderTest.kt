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
import org.koin.core.get
import java.io.File
import java.io.IOException

class FileProviderTest : KoinComponent, AbstractDefaultTester() {
    companion object {
        val silentSoundArray = byteArrayOf(0xFC.toByte(), 0xFF.toByte(), 0xFE.toByte())
    }

    @Test
    fun canReadLocalFile() {
        val testFilePath = "./test2.flac"
        val fileByteArray = File(testFilePath).source().buffer().readByteArray()
        val audioManager = RawOpusStreamProvider()
        val provider = LocalFileProvider(audioManager, testFilePath)
        GlobalScope.async { provider.startStream() }
        runBlocking {
            withTimeout(5000) {
                while (audioManager.encodedDataInputStream == null && isActive) {
                    delay(10)
                }
            }
        }
        // 大前提としてファイルが空っぽでない事が必須
        Assertions.assertTrue(fileByteArray.isNotEmpty())
        // 0xFC 0xFF 0xFE 以外の結果が返ってくればOK
        Assertions.assertTrue(audioManager.provide())
        val testArray = ByteArray(3)
        audioManager.buffer.get(testArray)
        Assertions.assertFalse(silentSoundArray contentEquals testArray)
        provider.stopStream()
    }

    @Test
    fun canReadHTTPRemoteFile() {
        val server = TestHTTPServer()
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true)
        val fileByteArray = File("./test2.flac").source().buffer().readByteArray()
        val testFileURL = "http://127.0.0.1:8888/test2.flac"
        val audioManager = RawOpusStreamProvider()
        val provider = HttpFileProvider(audioManager, testFileURL)
        GlobalScope.async { provider.startStream() }
        runBlocking {
            withTimeout(5000) {
                while (audioManager.encodedDataInputStream == null && isActive) {
                    delay(10)
                }
            }
        }
        // 大前提としてファイルが空っぽでない事が必須
        Assertions.assertTrue(fileByteArray.isNotEmpty())
        // 0xFC 0xFF 0xFE 以外の結果が返ってくればOK

        server.stop()
    }

    @Test
    fun errorOnNonExistFile() {
        val audioManager = RawOpusStreamProvider()
        val provider = LocalFileProvider(audioManager, "./DOES-NOT-EXIST-FILE")
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