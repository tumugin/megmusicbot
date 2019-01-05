package com.myskng.megmusicbot.provider

import com.myskng.megmusicbot.extension.useMultipleCloseableSuspend
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
            useMultipleCloseableSuspend(fileSource, fileBuffer) {
                inputDataToEncoder().start()
                while (fileBuffer.exhausted().not() && isActive) {
                    if (fileBuffer.request(fileReaderBufferSize.toLong())) {
                        originStreamQueue.send(fileBuffer.readByteArray(fileReaderBufferSize.toLong()))
                    } else {
                        originStreamQueue.send(fileBuffer.readByteArray())
                    }
                }
                originStreamQueue.send(byteArrayOf())
            }
        } catch (ex: Exception) {
            cleanup()
        }
    }
}