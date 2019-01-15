package com.myskng.megmusicbot.test.database

import com.myskng.megmusicbot.database.SearchQuery
import com.myskng.megmusicbot.database.SongSearch
import com.myskng.megmusicbot.database.SongSearchType
import com.myskng.megmusicbot.database.Songs
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.Connection
import java.sql.DriverManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseTest {
    @BeforeAll
    fun setup() {
        Database.connect({ DriverManager.getConnection("jdbc:sqlite:megmusictest.db") })
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.drop(Songs)
            SchemaUtils.create(Songs)
        }
    }

    @AfterAll
    fun shutdown() {
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.drop(Songs)
        }
    }

    @Test
    fun testCanFindSong() {
        transaction {
            Songs.insert {
                it[this.album] = "RefRain"
                it[this.artist] = "上田麗奈"
                it[this.filePath] = "./test2.flac"
                it[this.title] = "ワタシ*ドリ"
            }
        }
        val songSearch = SongSearch()
        val albumSearchResult = songSearch.searchSong(arrayOf(SearchQuery(SongSearchType.Album, "RefRain")))
        val artistSearchResult = songSearch.searchSong(arrayOf(SearchQuery(SongSearchType.Artist, "上田麗奈")))
        val titleSearchResult = songSearch.searchSong(arrayOf(SearchQuery(SongSearchType.Album, "ワタシ*ドリ")))
        arrayOf(albumSearchResult, artistSearchResult, titleSearchResult).forEach {
            Assertions.assertEquals("RefRain", it.first().album)
            Assertions.assertEquals("上田麗奈", it.first().artist)
            Assertions.assertEquals("ワタシ*ドリ", it.first().title)
        }
    }
}