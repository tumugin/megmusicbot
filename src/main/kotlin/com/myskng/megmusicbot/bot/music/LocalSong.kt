package com.myskng.megmusicbot.bot.music

import com.myskng.megmusicbot.provider.LocalFileProvider
import sx.blah.discord.handle.audio.IAudioManager

data class LocalSong(
    override val title: String,
    override val artist: String,
    override val album: String,
    val filePath: String
) : ISong {
    override var onError: (exception: Exception) -> Unit = {}
    private lateinit var localFileProvider: LocalFileProvider

    override fun stop() {
        if (::localFileProvider.isInitialized) {
            localFileProvider.stopStream()
        }
    }

    override suspend fun play(iAudioManager: IAudioManager) {
        localFileProvider = LocalFileProvider(iAudioManager, filePath)
        localFileProvider.onError = onError
        localFileProvider.startStream()
    }
}