package com.myskng.megmusicbot.main

import com.myskng.megmusicbot.bot.BotConnectionManager
import com.myskng.megmusicbot.di.initializeKoinProduction
import com.myskng.megmusicbot.scanner.SongScanner
import com.myskng.megmusicbot.store.readJsonConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.koin.core.KoinComponent
import org.koin.core.get
import picocli.CommandLine
import java.sql.DriverManager

class MegmusicMain {
    class AppCommand {
        @CommandLine.Option(names = ["bot"])
        var isBotMode: Boolean = false

        @CommandLine.Option(names = ["scanner"])
        var isScannerMode: Boolean = false
        val isModeNotSet
            get() = !(isBotMode || isScannerMode)

        @CommandLine.Option(names = ["--config"])
        var configPath = "./config.json"

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
        fun main(args: Array<String>) = runBlocking(Dispatchers.Main) {
            val command = AppCommand()
            CommandLine(command).parse(*args)
            command.checkCommand()
            val config = readJsonConfig(command.configPath)
            initializeKoinProduction(config)
            Database.connect({ DriverManager.getConnection(config.dbConnectionString) })
            when {
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
