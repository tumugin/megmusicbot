package com.myskng.megmusicbot.bot.music

import com.myskng.megmusicbot.provider.LocalFileProvider

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

    override suspend fun play(rawOpusStreamProvider: RawOpusStreamProvider) {
        localFileProvider = LocalFileProvider(rawOpusStreamProvider, filePath)
        localFileProvider.onError = onError
        localFileProvider.startStream()
    }
}