package com.myskng.megmusicbot.provider

import com.myskng.megmusicbot.encoder.IEncoderProcess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import sx.blah.discord.handle.audio.IAudioManager
import sx.blah.discord.util.audio.providers.AudioInputStreamProvider
import java.util.logging.Level
import java.util.logging.Logger
import javax.sound.sampled.AudioSystem

abstract class AbstractFileProvider(private val iAudioManager: IAudioManager) : KoinComponent {
    private val encoderProcess by inject<IEncoderProcess>()
    private var audioInputStreamProvider: AudioInputStreamProvider? = null
    private val job = Job()

    protected val logger = Logger.getLogger(this::class.qualifiedName)
    protected val originStreamQueue = Channel<ByteArray>(Int.MAX_VALUE)
    protected val coroutineContext = Dispatchers.Default + job

    protected open fun cleanup() {
        if (encoderProcess.isProcessAlive) {
            encoderProcess.killProcess()
        }
        job.cancel()
    }

    protected abstract fun fetchOriginStream(): Deferred<Unit>

    protected fun inputDataToEncoder() = GlobalScope.async(coroutineContext) {
        try {
            encoderProcess.startProcess()
            val stream = encoderProcess.stdInputStream
            stream.use {
                lateinit var byteArray: ByteArray
                while (suspend {
                        byteArray = originStreamQueue.receive()
                        byteArray.isNotEmpty()
                    }.invoke()) {
                    stream.write(byteArray)
                }
                logger.log(Level.INFO, "Encoder input complete.")
            }
        } catch (ex: Exception) {
            logger.log(Level.SEVERE, ex.toString())
            cleanup()
        }
    }

    protected fun getDataFromEncoder() = GlobalScope.async(coroutineContext) {
        try {
            val audioInputStream = AudioSystem.getAudioInputStream(encoderProcess.stdOutputStream)
            audioInputStreamProvider = AudioInputStreamProvider(audioInputStream)
            iAudioManager.audioProvider = audioInputStreamProvider
            logger.log(Level.INFO, "AudioSystem prepare OK.")
        } catch (ex: Exception) {
            logger.log(Level.SEVERE, ex.toString())
            cleanup()
        }
    }

    suspend fun startStream() {
        withContext(Dispatchers.Default) {
            fetchOriginStream().start()
            inputDataToEncoder().start()
            getDataFromEncoder().start()
            // Wait until playing ends.
            while (true) {
                delay(500)
                if (audioInputStreamProvider?.isReady?.not() == true || job.isCancelled) {
                    break
                }
            }
        }
    }
}