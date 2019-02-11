package com.myskng.megmusicbot.test.database

import com.myskng.megmusicbot.database.SearchQuery
import com.myskng.megmusicbot.database.SongSearch
import com.myskng.megmusicbot.database.SongSearchType
import com.myskng.megmusicbot.database.Songs
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
            Songs.insert {
                it[this.album] = "RefRain"
                it[this.artist] = "上田麗奈"
                it[this.filePath] = "./test2.flac"
                it[this.title] = "ワタシ*ドリ"
            }
        }
    }

    @AfterAll
    fun shutdown() {
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.drop(Songs)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["Ref", "RefRain"])
    fun testCanFindSongAlbum(album: String) {
        val songSearch = SongSearch()
        val searchResult = songSearch.searchSong(arrayOf(SearchQuery(SongSearchType.Album, album)))
        searchResult.first().also {
            Assertions.assertEquals("RefRain", it.album)
            Assertions.assertEquals("上田麗奈", it.artist)
            Assertions.assertEquals("ワタシ*ドリ", it.title)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["上田麗奈", "上田", "麗奈"])
    fun testCanFindSongArtist(artist: String) {
        val songSearch = SongSearch()
        val searchResult = songSearch.searchSong(arrayOf(SearchQuery(SongSearchType.Artist, artist)))
        searchResult.first().also {
            Assertions.assertEquals("RefRain", it.album)
            Assertions.assertEquals("上田麗奈", it.artist)
            Assertions.assertEquals("ワタシ*ドリ", it.title)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["ワタシ", "ドリ", "ワタシ*ドリ"])
    fun testCanFindSongTitle(title: String) {
        val songSearch = SongSearch()
        val searchResult = songSearch.searchSong(arrayOf(SearchQuery(SongSearchType.Title, title)))
        searchResult.first().also {
            Assertions.assertEquals("RefRain", it.album)
            Assertions.assertEquals("上田麗奈", it.artist)
            Assertions.assertEquals("ワタシ*ドリ", it.title)
        }
    }

    @Test
    fun testCanFindSong() {
        val songSearch = SongSearch()
        val searchQueries =
            arrayOf(
                SearchQuery(SongSearchType.Album, "RefRain"),
                SearchQuery(SongSearchType.Artist, "上田麗奈"),
                SearchQuery(SongSearchType.Title, "ワタシ*ドリ")
            )
        val searchResult = songSearch.searchSong(searchQueries)
        searchResult.first().also {
            Assertions.assertEquals("RefRain", it.album)
            Assertions.assertEquals("上田麗奈", it.artist)
            Assertions.assertEquals("ワタシ*ドリ", it.title)
        }
    }
}