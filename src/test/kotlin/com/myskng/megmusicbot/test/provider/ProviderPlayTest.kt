package com.myskng.megmusicbot.test.provider

import com.myskng.megmusicbot.bot.music.RawOpusStreamProvider
import com.myskng.megmusicbot.provider.LocalFileProvider
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.count
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent

class ProviderPlayTest : KoinComponent, AbstractDefaultTester() {
    @Test
    fun canDetectPlayEnd() = runBlocking {
        val audioManager = RawOpusStreamProvider()
        val provider = LocalFileProvider(audioManager, "./rawopus.blob")
        provider.onError = ProviderTestUtil.rethrowError
        val startStream = GlobalScope.async { provider.startStream() }
        withTimeout(5000) {
            while (audioManager.encodedDataInputStream == null && !provider.isOriginStreamAlive && isActive) {
                delay(10)
            }
        }
        audioManager.provide()
        val nullStream = async(context = Dispatchers.IO) {
            while (isActive) {
                audioManager.encodedDataInputStream?.readAllBytes()
            }
        }
        withTimeout(10000) {
            startStream.await()
            nullStream.cancel()
        }
    }
}