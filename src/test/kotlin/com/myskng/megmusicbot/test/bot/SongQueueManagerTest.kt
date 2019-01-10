package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.SongQueueManager
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.standalone.KoinComponent
import org.koin.standalone.get
import sx.blah.discord.handle.audio.impl.AudioManager

class SongQueueManagerTest : AbstractDefaultTester(), KoinComponent {
    @Test
    fun canQueueSong() {
        runBlocking {
            val audioManager = get<AudioManager>()
            val songQueue = SongQueueManager()
            val firstSong = MockSong()
            val secondSong = MockSong()
            songQueue.songQueue.addAll(arrayOf(firstSong, secondSong))

            val playQueue = GlobalScope.async {
                songQueue.playQueue(audioManager)
            }

            // wait until property changes
            withTimeout(1000) {
                while (songQueue.playingSong == null) {
                    delay(1)
                }
            }
            Assertions.assertSame(firstSong, songQueue.playingSong)
            firstSong.stop()

            // wait until property changes
            withTimeout(1000) {
                while (songQueue.playingSong == firstSong) {
                    delay(1)
                }
            }
            Assertions.assertSame(secondSong, songQueue.playingSong)
            secondSong.stop()

            withTimeout(5000) {
                playQueue.await()
            }
        }
    }
}