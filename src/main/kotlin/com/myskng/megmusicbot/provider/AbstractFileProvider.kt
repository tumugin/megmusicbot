package com.myskng.megmusicbot.provider

import com.myskng.megmusicbot.bot.music.RawOpusStreamProvider
import com.myskng.megmusicbot.encoder.IEncoderProcess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okio.buffer
import okio.source
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.logging.Level
import java.util.logging.Logger

abstract class AbstractFileProvider(private val rawOpusStreamProvider: RawOpusStreamProvider) : KoinComponent,
    CoroutineScope {
    private val encoderProcess by inject<IEncoderProcess>()
    private val job = Job()
    override val coroutineContext = Dispatchers.IO + job
    protected val logger by inject<Logger>()
    val originStreamQueue = Channel<ByteArray>(Channel.UNLIMITED)

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

    private fun inputDataToEncoder() = async(newSingleThreadContext("inputDataToEncoder")) {
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

    private fun getDataFromEncoder() = async {
        try {
            logger.log(Level.INFO, "[Encoder] Encoder output processor start.")
            val outputStream = encoderProcess.stdOutputStream.source().buffer()
            rawOpusStreamProvider.decodedPCMBuffer = outputStream
            logger.log(Level.INFO, "[Encoder] Encoder output processor end.")
        } catch (ex: Exception) {
            logger.log(Level.SEVERE, "[Encoder] $ex")
            cleanupOnError()
            reportError(ex)
        }
    }

    suspend fun startStream() = withContext(Dispatchers.Default) {
        logger.log(Level.INFO, "[Provider] Provider starting...")
        encoderProcess.startProcess()
        awaitAll(fetchOriginStream(), inputDataToEncoder(), getDataFromEncoder())
        while (isActive && rawOpusStreamProvider.decodedPCMBuffer?.buffer?.size != 0L) {
            delay(20)
        }
        logger.log(Level.INFO, "[Provider] Song play end. Provider disposing...")
    }

    fun stopStream() {
        encoderProcess.killProcess()
        job.cancel()
    }
}
