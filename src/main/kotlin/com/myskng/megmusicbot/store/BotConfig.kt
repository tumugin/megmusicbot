package com.myskng.megmusicbot.store

data class BotConfig(
    val discordAPIKey: String,
    val ffmpegPath: String,
    val dbConnectionString: String,
    val musicPaths: Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BotConfig

        if (discordAPIKey != other.discordAPIKey) return false
        if (ffmpegPath != other.ffmpegPath) return false
        if (dbConnectionString != other.dbConnectionString) return false
        if (!musicPaths.contentEquals(other.musicPaths)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = discordAPIKey.hashCode()
        result = 31 * result + ffmpegPath.hashCode()
        result = 31 * result + dbConnectionString.hashCode()
        result = 31 * result + musicPaths.contentHashCode()
        return result
    }
}