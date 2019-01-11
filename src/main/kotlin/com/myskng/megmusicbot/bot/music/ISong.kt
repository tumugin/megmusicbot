package com.myskng.megmusicbot.bot.music

import sx.blah.discord.handle.audio.IAudioManager

interface ISong {
    val title: String
    val artist: String
    val album: String
    var onError: (exception: Exception) -> Unit
    suspend fun play(iAudioManager: IAudioManager)
    fun stop()
}