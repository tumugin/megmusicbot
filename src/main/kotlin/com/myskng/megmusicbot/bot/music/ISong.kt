package com.myskng.megmusicbot.bot.music

interface ISong {
    val title: String
    val artist: String
    val album: String
    var onError: (exception: Exception) -> Unit
    suspend fun play(rawOpusStreamProvider: RawOpusStreamProvider)
    fun stop()
}