package com.myskng.megmusicbot.provider

import com.myskng.megmusicbot.encoder.IEncoderProcess
import kotlinx.coroutines.*
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

    protected var isCanceled = false
    protected val originStreamQueue: BlockingQueue<ByteArray> = LinkedBlockingQueue()

    protected abstract fun fetchOriginStream(): Deferred<Unit>

    protected open fun closeOriginStream() {
        // Do nothing here
    }

    protected fun inputDataToEncoder() = GlobalScope.async {
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
            closeOriginStream()
            if (encoderProcess.isProcessAlive) {
                encoderProcess.killProcess()
            }
            isCanceled = true
        }
    }

    protected fun getDataFromEncoder() = GlobalScope.async {
        try {
            val audioInputStream = AudioSystem.getAudioInputStream(encoderProcess.stdOutputStream)
            audioInputStreamProvider = AudioInputStreamProvider(audioInputStream)
            iAudioManager.audioProvider = audioInputStreamProvider
        } catch (ex: Exception) {
            closeOriginStream()
            if (encoderProcess.isProcessAlive) {
                encoderProcess.killProcess()
            }
            isCanceled = true
        }
    }

    suspend fun startStream() {
        withContext(Dispatchers.Default) {
            fetchOriginStream().start()
            while (true) {
                delay(500)
                if (audioInputStreamProvider?.isReady?.not() == true || isCanceled) {
                    break
                }
            }
        }
    }
}