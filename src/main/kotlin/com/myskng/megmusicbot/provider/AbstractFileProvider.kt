package com.myskng.megmusicbot.provider

import com.myskng.megmusicbot.bot.music.RawOpusStreamProvider
import com.myskng.megmusicbot.encoder.IEncoderProcess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import java.io.BufferedInputStream
import java.util.logging.Level
import java.util.logging.Logger

abstract class AbstractFileProvider(private val rawOpusStreamProvider: RawOpusStreamProvider) : KoinComponent {
    private val encoderProcess by inject<IEncoderProcess>()
    private val job = Job(get())

    protected val logger by inject<Logger>()
    val originStreamQueue = Channel<ByteArray>(Channel.UNLIMITED)
    val encoderOutputStreamQueue = Channel<ByteArray>(Channel.UNLIMITED)
    protected val coroutineContext = Dispatchers.IO + job

    // データ取得元のストリームが有効か示すフラグ(1byteでも受け取れたらtrueにしなければならない)
    var isOriginStreamAlive = false

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

    protected fun inputDataToEncoder() = GlobalScope.async(newSingleThreadContext("inputDataToEncoder") + job) {
        try {
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

    protected fun getDataFromEncoder() = GlobalScope.async(newSingleThreadContext("getDataFromEncoder") + job) {
        try {
            logger.log(Level.INFO, "[Encoder] AudioSystem prepare start.")
            val baseStream = BufferedInputStream(encoderProcess.stdOutputStream, 50485760)
            rawOpusStreamProvider.baseInputStream = baseStream
            logger.log(Level.INFO, "[Encoder] AudioSystem prepare OK.")
        } catch (ex: Exception) {
            logger.log(Level.SEVERE, "[Encoder] $ex")
            cleanupOnError()
            reportError(ex)
        }
    }

    suspend fun startStream() = withContext(newSingleThreadContext("startStream") + job) {
        logger.log(Level.INFO, "[Provider] Provider starting...")
        encoderProcess.startProcess()
        awaitAll(fetchOriginStream(), inputDataToEncoder(), getDataFromEncoder())
        while (isActive && rawOpusStreamProvider.baseInputStream?.available() ?: 0 > 0) {
            delay(10)
        }
        logger.log(Level.INFO, "[Provider] Song play end. Provider disposing...")
    }

    fun stopStream() {
        encoderProcess.killProcess()
        job.cancel()
    }
}
