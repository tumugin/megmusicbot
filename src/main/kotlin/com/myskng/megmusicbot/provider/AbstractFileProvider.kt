package com.myskng.megmusicbot.provider

import com.myskng.megmusicbot.encoder.IEncoderProcess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.take
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import sx.blah.discord.handle.audio.IAudioManager
import sx.blah.discord.util.audio.providers.AudioInputStreamProvider
import java.lang.Exception
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import javax.sound.sampled.AudioSystem

abstract class AbstractFileProvider(private val iAudioManager: IAudioManager) : KoinComponent {
    private val encoderProcess by inject<IEncoderProcess>()
    private var audioInputStreamProvider: AudioInputStreamProvider? = null
    private val job = Job()

    protected val originStreamQueue: BlockingQueue<ByteArray> = LinkedBlockingQueue()
    protected abstract fun fetchOriginStream(): Deferred<Unit>
    protected val coroutineContext = Dispatchers.IO + job

    protected open fun cleanup() {
        if (encoderProcess.isProcessAlive) {
            encoderProcess.killProcess()
        }
        job.cancel()
    }

    protected fun inputDataToEncoder() = GlobalScope.async(coroutineContext) {
        try {
            encoderProcess.startProcess()
            getDataFromEncoder().start()
            val stream = encoderProcess.stdInputStream
            lateinit var byteArray: ByteArray
            while ({
                    byteArray = originStreamQueue.take()
                    byteArray.isNotEmpty()
                }.invoke()) {
                stream.write(byteArray)
            }
        } catch (ex: Exception) {
            cleanup()
        }
    }

    protected fun getDataFromEncoder() = GlobalScope.async(coroutineContext) {
        try {
            val audioInputStream = AudioSystem.getAudioInputStream(encoderProcess.stdOutputStream)
            audioInputStreamProvider = AudioInputStreamProvider(audioInputStream)
            iAudioManager.audioProvider = audioInputStreamProvider
        } catch (ex: Exception) {
            cleanup()
        }
    }

    suspend fun startStream() {
        withContext(Dispatchers.Default) {
            fetchOriginStream().start()
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