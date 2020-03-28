package com.myskng.megmusicbot.bot.music

import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.logging.Level
import java.util.logging.Logger

class SongQueueManager : KoinComponent {
    val songQueue = mutableListOf<ISong>()
    var playingSong: ISong? = null
    var onQueueEmpty: (() -> ISong?)? = null
    var onSongPlay: ((song: ISong) -> Unit)? = null
    var onError: ((exception: Exception) -> Unit)? = null

    private val job = Job()
    private val logger by inject<Logger>()
    private var isPlaying = false

    suspend fun playQueue(rawOpusStreamProvider: RawOpusStreamProvider) {
        when {
            job.isCancelled || job.isCompleted -> throw IllegalStateException("Do not reuse SongQueueManager")
            isPlaying -> throw IllegalStateException("Already playing queue. Do not rerun playQueue().")
        }
        isPlaying = true
        withContext(Dispatchers.Default + job) {
            while (isActive) {
                if (songQueue.isNotEmpty()) {
                    playingSong = songQueue.removeAt(0)
                    playingSong?.onError = { ex -> onError?.invoke(ex) }
                    onSongPlay?.invoke(playingSong!!)
                    try {
                        playingSong?.play(rawOpusStreamProvider)
                    } catch (_: CancellationException) {
                    }
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

    fun stop() {
        logger.log(Level.INFO, "[SongQueueManager] Cancel signal received.")
        playingSong?.stop()
        job.cancel()
    }

    fun skip() {
        logger.log(Level.INFO, "[SongQueueManager] Skip song.")
        playingSong?.stop()
    }
}
