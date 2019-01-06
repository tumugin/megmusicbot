package com.myskng.megmusicbot.provider

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import okio.buffer
import okio.source
import org.koin.standalone.KoinComponent
import sx.blah.discord.handle.audio.IAudioManager
import java.io.File
import java.util.logging.Level

class LocalFileProvider(audioManager: IAudioManager, private val filePath: String) : KoinComponent,
    AbstractFileProvider(audioManager) {
    companion object {
        const val fileReaderBufferSize = 1024 * 256
    }

    override fun fetchOriginStream() = GlobalScope.async(coroutineContext) {
        try {
            logger.log(Level.INFO, "[LocalFile] Load start. file=$filePath")
            val fileSource = File(filePath).source()
            val fileBuffer = fileSource.buffer()
            fileSource.use {
                fileBuffer.use {
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