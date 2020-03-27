package com.myskng.megmusicbot.provider

import com.myskng.megmusicbot.bot.music.RawOpusStreamProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import okio.buffer
import okio.source
import org.koin.core.KoinComponent
import java.io.File
import java.util.logging.Level

class LocalFileProvider(rawOpusStreamProvider: RawOpusStreamProvider, private val filePath: String) : KoinComponent,
    AbstractFileProvider(rawOpusStreamProvider) {
    companion object {
        const val fileReaderBufferSize = 1024
    }

    override fun fetchOriginStream() = GlobalScope.async(coroutineContext) {
        try {
            logger.log(Level.INFO, "[LocalFile] Load start. file=$filePath")
            val fileSource = File(filePath).source()
            val fileBuffer = fileSource.buffer()
            fileSource.use {
                fileBuffer.use {
                    if (!fileBuffer.exhausted()) {
                        isOriginStreamAlive = true
                    }
                    while (fileBuffer.exhausted().not() && isActive) {
                        if (fileBuffer.request(fileReaderBufferSize.toLong())) {
                            originStreamQueue.send(fileBuffer.readByteArray(fileReaderBufferSize.toLong()))
                        } else {
                            originStreamQueue.send(fileBuffer.readByteArray())
                        }
                    }
                    originStreamQueue.send(byteArrayOf())
                    logger.log(Level.INFO, "[LocalFile] Loaded all bytes.")
                }
            }
        } catch (ex: Exception) {
            logger.log(Level.SEVERE, "[LocalFile] $ex")
            cleanupOnError()
            reportError(ex)
        }
    }
}
