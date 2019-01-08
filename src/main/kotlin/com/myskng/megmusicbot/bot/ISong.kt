package com.myskng.megmusicbot.bot

import sx.blah.discord.handle.audio.IAudioManager

interface ISong {
    val title: String
    val artist: String
    val album: String
    val onError: (exception: Exception) -> Unit
    suspend fun play(iAudioManager: IAudioManager)
    fun stop()
}