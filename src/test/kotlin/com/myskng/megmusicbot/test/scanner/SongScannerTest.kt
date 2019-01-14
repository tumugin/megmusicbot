package com.myskng.megmusicbot.test.scanner

import com.myskng.megmusicbot.database.Songs
import com.myskng.megmusicbot.scanner.SongScanner
import com.myskng.megmusicbot.store.BotConfig
import com.myskng.megmusicbot.store.BotStateStore
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.dsl.module.module
import org.koin.standalone.KoinComponent
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class SongScannerTest : AbstractDefaultTester(), KoinComponent {
    @BeforeAll
    override fun setupKoin() {
        additionalKoinModules.add(module {
            single {
                val store = BotStateStore()
                store.config = BotConfig("UNUSED", "UNUSED", "UNUSED", arrayOf("."))
                store
            }
        })
        super.setupKoin()
    }

    @BeforeAll
    fun setup() {
        Database.connect({ DriverManager.getConnection("jdbc:sqlite:megmusictest.db") })
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction {
            addLogger(StdOutSqlLogger)
            drop(Songs)
            create(Songs)
        }
    }

    @AfterAll
    fun shutdown() {
        transaction {
            addLogger(StdOutSqlLogger)
            drop(Songs)
        }
    }

    @Test
    fun canReadTag() {
        val songScanner = SongScanner()
        songScanner.scanFiles()
        val list = transaction {
            Songs.selectAll().toList()
        }
        val result = transaction {
            addLogger(StdOutSqlLogger)
            Songs.select {
                (Songs.title eq "750hz") and
                        (Songs.artist eq "Ron \"Nino\" Batista") and
                        (Songs.album eq "Stereo Test Tones - 20MHz-10Hz") and
                        (Songs.filePath eq File("./test2.flac").absolutePath)
            }.toList()
        }
        Assertions.assertTrue(result.isNotEmpty())
    }
}