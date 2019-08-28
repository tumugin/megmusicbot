package com.myskng.megmusicbot.test.provider

import com.myskng.megmusicbot.bot.music.RawOpusStreamProvider
import com.myskng.megmusicbot.provider.LocalFileProvider
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent

class ProviderPlayTest : KoinComponent, AbstractDefaultTester() {
    @Test
    fun canDetectPlayEnd() {
        val audioManager = RawOpusStreamProvider()
        val provider = LocalFileProvider(audioManager, "./test2.flac")
        val startStream = GlobalScope.async { provider.startStream() }
        runBlocking {
            withTimeout(5000) {
                while (audioManager.encodedDataInputStream == null && isActive) {
                    delay(10)
                }
            }
        }
        audioManager.provide()
        while (audioManager.buffer.hasRemaining()) {
            // read all bytes from stream
            val testArray = ByteArray(3)
            audioManager.buffer.get(testArray)
            if (ProviderTestUtil.silentSoundArray contentEquals testArray) {
                break
            }
            audioManager.provide()
        }
        runBlocking {
            withTimeout(1000) {
                startStream.await()
            }
        }
    }
}