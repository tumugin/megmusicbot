package com.myskng.megmusicbot.test.scanner

import com.myskng.megmusicbot.database.Songs
import com.myskng.megmusicbot.scanner.SongScanner
import com.myskng.megmusicbot.config.BotConfig
import com.myskng.megmusicbot.store.BotStateStore
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.dsl.module
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class SongScannerTest : AbstractDefaultTester(), KoinComponent {
    @BeforeAll
    override fun setupKoin() {
        additionalKoinModules.add(module {
            single {
                BotStateStore()
            }
            single {
                BotConfig("UNUSED", "UNUSED", "UNUSED", arrayOf("."))
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
    fun canReadTag() = runBlocking {
        val songScanner = SongScanner()
        songScanner.scanFilesAsync().await()
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
