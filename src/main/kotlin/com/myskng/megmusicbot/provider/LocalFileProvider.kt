package com.myskng.megmusicbot.provider

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.koin.standalone.KoinComponent
import sx.blah.discord.handle.audio.IAudioManager
import java.io.FileInputStream
import java.lang.Exception

class LocalFileProvider(private val audioManager: IAudioManager, private val filePath: String) : KoinComponent,
    AbstractFileProvider(audioManager) {
    private lateinit var fileInputStream: FileInputStream

    override fun closeOriginStream() {
        super.closeOriginStream()
        fileInputStream.close()
    }

    override fun fetchOriginStream() = GlobalScope.async {
        try{
            fileInputStream = FileInputStream(filePath)
            val bufferArray = ByteArray(HttpFileProvider.httpBufferSize)
            var bufferReadSize = 0
            while ({
                    bufferReadSize = fileInputStream.read(bufferArray, 0, HttpFileProvider.httpBufferSize)
                    bufferReadSize != 0
                }.invoke()) {
                originStreamQueue.add(bufferArray.copyOf(bufferReadSize))
            }
            originStreamQueue.add(byteArrayOf())
            fileInputStream.close()
        }catch(ex:Exception){
            closeOriginStream()
            isCanceled = true
        }
    }
}