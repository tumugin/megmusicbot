package com.myskng.megmusicbot.bot.music

import com.myskng.megmusicbot.provider.HttpFileProvider
import sx.blah.discord.handle.audio.IAudioManager

data class HTTPFileSong(
    val fileUrl: String
) : ISong {
    override var onError: (exception: Exception) -> Unit = {}
    override val title: String = "Remote HTTP Song"
    override val artist: String = "Remote HTTP Song"
    override val album: String = "Remote HTTP Song"

    private lateinit var httpFileProvider: HttpFileProvider

    override suspend fun play(iAudioManager: IAudioManager) {
        httpFileProvider = HttpFileProvider(iAudioManager, fileUrl)
        httpFileProvider.onError = onError
        httpFileProvider.startStream()
    }

    override fun stop() {
        if (::httpFileProvider.isInitialized) {
            httpFileProvider.stopStream()
        }
    }
}