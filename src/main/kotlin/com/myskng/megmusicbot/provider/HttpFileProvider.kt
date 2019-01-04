package com.myskng.megmusicbot.provider

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import sx.blah.discord.handle.audio.IAudioManager

class HttpFileProvider(private val audioManager: IAudioManager, private val url: String) : KoinComponent,
    AbstractFileProvider(audioManager) {
    companion object {
        const val httpBufferSize = 1024 * 256
    }

    private val okHttpClient by inject<OkHttpClient>()
    private lateinit var httpResponse: Response

    override fun closeOriginStream() {
        super.closeOriginStream()
        if (::httpResponse.isInitialized) {
            httpResponse.close()
        }
    }

    override fun fetchOriginStream() = GlobalScope.async {
        try {
            val request = Request.Builder().url(url).build()
            httpResponse = okHttpClient.newCall(request).execute()
            if (httpResponse.isSuccessful.not()) {
                throw Exception("HTTP(S) connection fault. Response code=${httpResponse.code()}")
            }
            inputDataToEncoder().start()
            val stream = httpResponse.body()!!.byteStream()
            val bufferArray = ByteArray(httpBufferSize)
            var bufferReadSize = 0
            while ({
                    bufferReadSize = stream.read(bufferArray, 0, httpBufferSize)
                    bufferReadSize != 0
                }.invoke()) {
                originStreamQueue.add(bufferArray.copyOf(bufferReadSize))
            }
            originStreamQueue.add(byteArrayOf())
            httpResponse.close()
        } catch (ex: Exception) {
            closeOriginStream()
            isCanceled = true
        }
    }
}