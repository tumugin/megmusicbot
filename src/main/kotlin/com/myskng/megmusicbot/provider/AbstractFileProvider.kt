package com.myskng.megmusicbot.provider

import com.myskng.megmusicbot.bot.music.RawOpusStreamProvider
import com.myskng.megmusicbot.encoder.IEncoderProcess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import java.util.logging.Level
import java.util.logging.Logger
import javax.sound.sampled.AudioSystem

abstract class AbstractFileProvider(private val rawOpusStreamProvider: RawOpusStreamProvider) : KoinComponent {
    private val encoderProcess by inject<IEncoderProcess>()
    private val job = Job(get())

    protected val logger by inject<Logger>()
    protected val originStreamQueue = Channel<ByteArray>(Int.MAX_VALUE)
    protected val coroutineContext = Dispatchers.IO + job

    var onError: ((exception: Exception) -> Unit)? = null

    protected fun reportError(exception: Exception) {
        when (exception) {
            is CancellationException -> {
                // dismiss this error
            }
            else -> onError?.invoke(exception)
        }
    }

    protected open fun cleanupOnError() {
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
                logger.log(Level.INFO, "[Encoder] Encoder input start.")
                lateinit var byteArray: ByteArray
                while (suspend {
                        byteArray = originStreamQueue.receive()
                        byteArray.isNotEmpty()
                    }.invoke()) {
                    stream.write(byteArray)
                }
                logger.log(Level.INFO, "[Encoder] Encoder input complete.")
            }
        } catch (ex: Exception) {
            logger.log(Level.SEVERE, "[Encoder] $ex")
            cleanupOnError()
            reportError(ex)
        }
    }

    protected fun getDataFromEncoder() = GlobalScope.async(coroutineContext) {
        try {
            logger.log(Level.INFO, "[Encoder] AudioSystem prepare start.")
            rawOpusStreamProvider.encodedDataInputStream = encoderProcess.stdOutputStream
            logger.log(Level.INFO, "[Encoder] AudioSystem prepare OK.")
        } catch (ex: Exception) {
            logger.log(Level.SEVERE, "[Encoder] $ex")
            cleanupOnError()
            reportError(ex)
        }
    }

    suspend fun startStream() = withContext(Dispatchers.Default) {
        logger.log(Level.INFO, "[Provider] Provider starting...")
        fetchOriginStream().start()
        inputDataToEncoder().start()
        getDataFromEncoder().start()
        // Wait until playing ends.
        while (true) {
            delay(500)
            if (job.isCancelled) {
                break
            }
        }
        logger.log(Level.INFO, "[Provider] Song play end. Provider disposing...")
    }

    fun stopStream() {
        job.cancel()
    }
}