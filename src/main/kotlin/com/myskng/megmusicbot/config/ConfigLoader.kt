package com.myskng.megmusicbot.config

import io.github.cdimascio.dotenv.dotenv

fun readEnvConfig(envFileName: String = ".env"): BotConfig {
    val dotenv = dotenv {
        filename = envFileName
        ignoreIfMissing = true
    }
    return BotConfig(
        discordApiKey = dotenv["DISCORD_API_KEY"]!!,
        ffmpegPath = dotenv["FFMPEG_PATH"] ?: "ffmpeg",
        dbConnectionString = dotenv["DB_CONNECTION"] ?: "jdbc:sqlite:megmusicbot.db",
        dbConnectionUser = dotenv["DB_USER"] ?: "",
        dbConnectionPassword = dotenv["DB_PASSWORD"] ?: "",
        musicPaths = splitEnvVariableToToList(dotenv["MUSIC_PATHS"]!!)
    )
}

fun splitEnvVariableToToList(command: String): Array<String> {
    val regex = Regex("(\"[^\"]+\"|[^\\s\"]+)")
    val resultList = mutableListOf<String>()
    var tempCommandStyleString = command
    while (regex.find(tempCommandStyleString) != null) {
        val match = regex.find(tempCommandStyleString)!!
        resultList.add(match.value.removeSurrounding("\""))
        val matchRange = match.range
        tempCommandStyleString = tempCommandStyleString.removeRange(matchRange)
    }
    return resultList.toTypedArray()
}
