package com.myskng.megmusicbot.provider

import com.myskng.megmusicbot.extension.useMultipleCloseableSuspend
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.source
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import sx.blah.discord.handle.audio.IAudioManager

class HttpFileProvider(audioManager: IAudioManager, private val url: String) : KoinComponent,
    AbstractFileProvider(audioManager) {
    companion object {
        const val httpBufferSize = 1024 * 256
    }

    private val okHttpClient by inject<OkHttpClient>()

    override fun fetchOriginStream() = GlobalScope.async<Unit>(coroutineContext) {
        try {
            val request = Request.Builder().url(url).build()
            val httpResponse = okHttpClient.newCall(request).execute()
            httpResponse.use {
                if (httpResponse.isSuccessful.not()) {
                    throw Exception("HTTP(S) connection fault. Response code=${httpResponse.code()}")
                }
                inputDataToEncoder().start()
                val stream = httpResponse.body()!!.byteStream()
                val streamSource = stream.source()
                val streamBuffer = streamSource.buffer()
                useMultipleCloseableSuspend(stream, streamSource, streamBuffer) {
                    while (streamBuffer.exhausted().not() && isActive) {
                        if (streamBuffer.request(httpBufferSize.toLong())) {
                            originStreamQueue.send(streamBuffer.readByteArray(httpBufferSize.toLong()))
                        } else {
                            originStreamQueue.send(streamBuffer.readByteArray())
                        }
                    }
                    originStreamQueue.send(byteArrayOf())
                }
            }
        } catch (ex: Exception) {
            cleanup()
        }
    }
}