package com.myskng.megmusicbot.main

import com.myskng.megmusicbot.bot.BotConnectionManager
import com.myskng.megmusicbot.config.readEnvConfig
import com.myskng.megmusicbot.di.initializeKoinProduction
import com.myskng.megmusicbot.scanner.SongScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.koin.core.KoinComponent
import org.koin.core.get
import picocli.CommandLine
import java.sql.Connection
import java.sql.DriverManager

class MegmusicMain {
    class AppCommand {
        @CommandLine.Option(names = ["--login"])
        var isLoginMode: Boolean = false

        @CommandLine.Option(names = ["--bot"])
        var isBotMode: Boolean = false

        @CommandLine.Option(names = ["--scanner"])
        var isScannerMode: Boolean = false
        val isModeNotSet
            get() = !(isBotMode || isScannerMode || isLoginMode)

        @CommandLine.Option(names = ["--config"])
        var configName: String? = null

        fun checkCommand() {
            if (isModeNotSet) {
                throw Exception("Specify bot or scanner mode.")
            }
            if (isBotMode && isScannerMode) {
                throw Exception("Multiple modes can not be specified.")
            }
        }
    }

    companion object : KoinComponent {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            val command = AppCommand()
            CommandLine(command).parseArgs(*args)
            command.checkCommand()
            val config = if (command.configName !== null) {
                readEnvConfig(command.configName!!)
            } else {
                readEnvConfig()
            }
            initializeKoinProduction(config)
            Database.connect({ DriverManager.getConnection(config.dbConnectionString) })
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            when {
                command.isLoginMode -> {
                    println("Please login with following url.")
                    println("https://discordapp.com/api/oauth2/authorize?client_id=<CLIENT ID HERE>&permissions=37223488&scope=bot")
                }
                command.isScannerMode -> {
                    val songScanner = get<SongScanner>()
                    songScanner.scanFilesAsync().await()
                }
                else -> {
                    val botConnectionManager = get<BotConnectionManager>()
                    botConnectionManager.initializeBotConnection()
                    while (isActive) {
                        delay(Long.MAX_VALUE)
                    }
                }
            }
        }
    }
}
