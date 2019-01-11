package com.myskng.megmusicbot.bot.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import sx.blah.discord.handle.audio.IAudioManager

class SongQueueManager {
    val songQueue = mutableListOf<ISong>()
    var playingSong: ISong? = null
    var onQueueEmpty: (() -> ISong)? = null
    var onSongPlay: ((song: ISong) -> Unit)? = null
    var onError: ((exception: Exception) -> Unit)? = null

    suspend fun playQueue(audioManager: IAudioManager) = withContext(Dispatchers.Default) {
        while (isActive) {
            if (songQueue.isNotEmpty()) {
                playingSong = songQueue.removeAt(0)
                playingSong?.onError = { ex -> onError?.invoke(ex) }
                onSongPlay?.invoke(playingSong!!)
                playingSong?.play(audioManager)
            } else {
                //On empty queue
                val result = onQueueEmpty?.invoke()
                if (result != null) {
                    songQueue.add(result)
                } else if (onQueueEmpty == null) {
                    break
                } else {
                    // retry in 1000msec.
                    delay(1000)
                }
            }
        }
    }
}