package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.ISong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sx.blah.discord.handle.audio.IAudioManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockSong : ISong {
    override val title: String
        get() = "プリンセス・アラモード"
    override val artist: String
        get() = "徳川まつり(CV.諏訪彩花)"
    override val album: String
        get() = "THE IDOLM@STER MILLION LIVE! M@STER SPARKLE 02"
    override val onError: (exception: Exception) -> Unit
        get() = {}

    private val latch = CountDownLatch(1)
    
    override suspend fun play(iAudioManager: IAudioManager) {
        withContext(Dispatchers.Default) {
            latch.await(10, TimeUnit.SECONDS)
        }
    }

    override fun stop() {
        latch.countDown()
    }
}