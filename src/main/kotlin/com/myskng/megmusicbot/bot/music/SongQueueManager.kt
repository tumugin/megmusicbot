package com.myskng.megmusicbot.bot.music

import kotlinx.coroutines.*
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import sx.blah.discord.handle.audio.IAudioManager
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

    suspend fun playQueue(audioManager: IAudioManager) {
        try {
            withContext(Dispatchers.Default + job) {
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
        } catch (_: CancellationException) {
            // dismiss CancellationException
            logger.log(Level.INFO, "[SongQueueManager] On Queue play cancel.")
        }
    }

    fun stop() {
        logger.log(Level.INFO, "[SongQueueManager] Cancel signal received.")
        job.cancel()
    }
}