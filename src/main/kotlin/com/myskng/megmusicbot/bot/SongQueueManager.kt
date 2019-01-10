package com.myskng.megmusicbot.bot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import sx.blah.discord.handle.audio.IAudioManager

class SongQueueManager {
    val songQueue = mutableListOf<ISong>()
    var playingSong: ISong? = null
    var onQueueEmpty: (() -> ISong)? = null

    suspend fun playQueue(audioManager: IAudioManager) {
        withContext(Dispatchers.Default) {
            while (isActive) {
                if (songQueue.isNotEmpty()) {
                    playingSong = songQueue.removeAt(0)
                    playingSong?.play(audioManager)
                } else {
                    //On empty queue
                    val result = onQueueEmpty?.invoke()
                    if (result != null) {
                        songQueue.add(result)
                    } else if (onQueueEmpty == null) {
                        break
                    } else {
                        delay(100)
                    }
                }
            }
        }
    }
}