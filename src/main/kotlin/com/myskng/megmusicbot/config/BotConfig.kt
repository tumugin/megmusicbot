package com.myskng.megmusicbot.config

data class BotConfig(
    val discordApiKey: String,
    val ffmpegPath: String,
    val dbConnectionString: String,
    val dbConnectionUser: String,
    val dbConnectionPassword: String,
    val musicPaths: Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BotConfig

        if (discordApiKey != other.discordApiKey) return false
        if (ffmpegPath != other.ffmpegPath) return false
        if (dbConnectionString != other.dbConnectionString) return false
        if (!musicPaths.contentEquals(other.musicPaths)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = discordApiKey.hashCode()
        result = 31 * result + ffmpegPath.hashCode()
        result = 31 * result + dbConnectionString.hashCode()
        result = 31 * result + musicPaths.contentHashCode()
        return result
    }
}
