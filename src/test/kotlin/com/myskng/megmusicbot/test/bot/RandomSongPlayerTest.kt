package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.RandomSongPlayer
import com.myskng.megmusicbot.database.Songs
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

class RandomSongPlayerTest : AbstractDefaultTester() {
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
    fun returnNullOnEmptyDB() {
        val randomSongPlayer = RandomSongPlayer()
        transaction { Songs.deleteAll() }
        Assertions.assertNull(randomSongPlayer.onEmptyQueue())
    }

    @Test
    fun canReturnSong() {
        val randomSongPlayer = RandomSongPlayer()
        transaction {
            Songs.deleteAll()
            Songs.insert {
                it[Songs.filePath] = "TEST-FILE"
                it[Songs.artist] = "TEST-ARTIST"
                it[Songs.album] = "TEST-ALBUM"
                it[Songs.title] = "TEST-TITLE"
            }
        }
        val song = randomSongPlayer.onEmptyQueue()
        Assertions.assertEquals("TEST-FILE", song!!.filePath)
        Assertions.assertEquals("TEST-ARTIST", song.artist)
        Assertions.assertEquals("TEST-ALBUM", song.album)
        Assertions.assertEquals("TEST-TITLE", song.title)
    }

    @Test
    fun canSelectRandomSong() {
        val randomSongPlayer = RandomSongPlayer()
        transaction {
            Songs.deleteAll()
            Songs.insert {
                it[Songs.filePath] = "TEST-FILE"
                it[Songs.artist] = "TEST-ARTIST"
                it[Songs.album] = "TEST-ALBUM"
                it[Songs.title] = "TEST-TITLE"
            }
        }
        val song = randomSongPlayer.onEmptyQueue()
        Assertions.assertEquals("TEST-FILE", song!!.filePath)
        Assertions.assertEquals("TEST-ARTIST", song.artist)
        Assertions.assertEquals("TEST-ALBUM", song.album)
        Assertions.assertEquals("TEST-TITLE", song.title)
        transaction {
            Songs.insert {
                it[Songs.filePath] = "TEST-FILE-2"
                it[Songs.artist] = "TEST-ARTIST-2"
                it[Songs.album] = "TEST-ALBUM-2"
                it[Songs.title] = "TEST-TITLE-2"
            }
        }
        val song2 = randomSongPlayer.onEmptyQueue()
        Assertions.assertEquals("TEST-FILE-2", song2!!.filePath)
        Assertions.assertEquals("TEST-ARTIST-2", song2.artist)
        Assertions.assertEquals("TEST-ALBUM-2", song2.album)
        Assertions.assertEquals("TEST-TITLE-2", song2.title)
    }

    @Test
    fun canSelectSongWhenPlayedAllSongs() {
        val randomSongPlayer = RandomSongPlayer()
        transaction {
            Songs.deleteAll()
            Songs.insert {
                it[Songs.filePath] = "TEST-FILE"
                it[Songs.artist] = "TEST-ARTIST"
                it[Songs.album] = "TEST-ALBUM"
                it[Songs.title] = "TEST-TITLE"
            }
        }
        Assertions.assertNotNull(randomSongPlayer.onEmptyQueue())
        val song = randomSongPlayer.onEmptyQueue()
        Assertions.assertNotNull(song)
        Assertions.assertEquals("TEST-FILE", song!!.filePath)
        Assertions.assertEquals("TEST-ARTIST", song.artist)
        Assertions.assertEquals("TEST-ALBUM", song.album)
        Assertions.assertEquals("TEST-TITLE", song.title)
    }
}