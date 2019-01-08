package com.myskng.megmusicbot.bot

import com.myskng.megmusicbot.provider.LocalFileProvider
import sx.blah.discord.handle.audio.IAudioManager

data class LocalSong(
    override val title: String,
    override val artist: String,
    override val album: String,
    val filePath: String,
    override var onError: (exception: Exception) -> Unit = {}
) : ISong {
    var localFileProvider: LocalFileProvider? = null
    override fun stop() {
        localFileProvider?.stopStream()
    }

    override suspend fun play(iAudioManager: IAudioManager) {
        localFileProvider = LocalFileProvider(iAudioManager, filePath)
        localFileProvider?.onError = onError
        localFileProvider?.startStream()
    }
}