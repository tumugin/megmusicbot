package com.myskng.megmusicbot.test.provider

import com.myskng.megmusicbot.provider.LocalFileProvider
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.get
import sx.blah.discord.handle.audio.impl.AudioManager
import sx.blah.discord.util.audio.providers.AudioInputStreamProvider

class ProviderPlayTest : KoinComponent, AbstractDefaultTester() {
    @Test
    fun canDetectPlayEnd() {
        val audioManager = get<AudioManager>()
        val provider = LocalFileProvider(audioManager, "./test2.flac")
        val startStream = GlobalScope.async { provider.startStream() }
        runBlocking {
            withTimeout(5000) {
                while (audioManager.audioProvider == null && isActive) {
                    delay(10)
                }
            }
        }
        val discordAudioProvider = audioManager.audioProvider as AudioInputStreamProvider
        while (discordAudioProvider.provide().isNotEmpty()) {
            // read all bytes from stream
        }
        runBlocking {
            withTimeout(1000) {
                startStream.await()
            }
        }
    }
}