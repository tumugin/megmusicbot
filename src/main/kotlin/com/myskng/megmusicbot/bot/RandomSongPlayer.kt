package com.myskng.megmusicbot.bot

import com.myskng.megmusicbot.bot.music.LocalSong
import com.myskng.megmusicbot.database.Songs
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class RandomSongPlayer {
    val playedSongId = mutableListOf<Int>()

    fun onEmptyQueue(): LocalSong? {
        val maxId = Songs.id.max()
        val biggest = transaction { Songs.slice(maxId).selectAll().map { it[maxId] } }
        if (biggest.isEmpty() || biggest.firstOrNull() == null) {
            return null
        }
        val randomList = (1..biggest.first()!!).toMutableList()
        if (playedSongId.containsAll(randomList).not()) {
            randomList.removeAll(playedSongId)
        } else {
            playedSongId.clear()
        }
        val missingIdList = mutableListOf<Int>()
        while (randomList.isNotEmpty()) {
            randomList.removeAll(missingIdList)
            val random = randomList.random()
            val randomSongList = transaction { Songs.select { Songs.id eq random }.toList() }
            if (randomSongList.isEmpty()) {
                missingIdList.add(random)
                continue
            }
            val selectedRandomSong = randomSongList.first()
            playedSongId.add(selectedRandomSong[Songs.id])
            return LocalSong(
                selectedRandomSong[Songs.title],
                selectedRandomSong[Songs.artist],
                selectedRandomSong[Songs.album],
                selectedRandomSong[Songs.filePath]
            )
        }
        return null
    }
}
