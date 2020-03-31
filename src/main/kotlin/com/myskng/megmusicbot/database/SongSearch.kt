package com.myskng.megmusicbot.database

import com.myskng.megmusicbot.bot.music.LocalSong
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

data class SearchQuery(val type: SongSearchType, val searchString: String)

open class SongSearch {
    open fun searchSong(query: Array<SearchQuery>): List<LocalSong> {
        val selectQuery = Songs.select {
            var sqlQuery: Op<Boolean>? = null
            query.forEach {
                val innerQuery = when (it.type) {
                    SongSearchType.Album -> Songs.album like "%${it.searchString}%"
                    SongSearchType.Artist -> Songs.artist like "%${it.searchString}%"
                    SongSearchType.Title -> Songs.title like "%${it.searchString}%"
                }
                sqlQuery = if (sqlQuery == null) {
                    innerQuery
                } else {
                    sqlQuery!! and innerQuery
                }
            }
            sqlQuery!!
        }
        val list = transaction {
            selectQuery.toList()
        }
        return list.map {
            LocalSong(it[Songs.title], it[Songs.artist], it[Songs.album], it[Songs.filePath])
        }
    }
}