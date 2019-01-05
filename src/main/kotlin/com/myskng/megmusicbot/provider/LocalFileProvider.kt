package com.myskng.megmusicbot.provider

import com.myskng.megmusicbot.extension.useMultipleCloseable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import okio.buffer
import okio.source
import org.koin.standalone.KoinComponent
import sx.blah.discord.handle.audio.IAudioManager
import java.io.File

class LocalFileProvider(audioManager: IAudioManager, private val filePath: String) : KoinComponent,
    AbstractFileProvider(audioManager) {
    companion object {
        const val fileReaderBufferSize = 1024 * 256
    }

    override fun fetchOriginStream() = GlobalScope.async<Unit>(coroutineContext) {
        try {
            val fileSource = File(filePath).source()
            val fileBuffer = fileSource.buffer()
            useMultipleCloseable(fileSource, fileBuffer) {
                inputDataToEncoder().start()
                while (fileBuffer.exhausted().not() && isActive) {
                    if (fileBuffer.request(fileReaderBufferSize.toLong())) {
                        originStreamQueue.add(fileBuffer.readByteArray(fileReaderBufferSize.toLong()))
                    } else {
                        originStreamQueue.add(fileBuffer.readByteArray())
                    }
                }
                originStreamQueue.add(byteArrayOf())
            }
        } catch (ex: Exception) {
            cleanup()
        }
    }
}