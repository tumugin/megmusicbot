package com.myskng.megmusicbot.store

data class BotConfig(
    val discordAPIKey: String,
    val ffmpegPath: String,
    val dbConnectionString: String,
    val musicPaths: Array<String>
)