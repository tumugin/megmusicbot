package com.myskng.megmusicbot.di

import com.myskng.megmusicbot.bot.BotCommand
import com.myskng.megmusicbot.bot.BotCommandProcessor
import com.myskng.megmusicbot.bot.BotConnectionManager
import com.myskng.megmusicbot.database.SongSearch
import com.myskng.megmusicbot.encoder.FFMpegEncoder
import com.myskng.megmusicbot.scanner.SongScanner
import com.myskng.megmusicbot.store.BotConfig
import com.myskng.megmusicbot.store.BotStateStore
import com.myskng.megmusicbot.text.DefaultLangStrings
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.startKoin
import java.util.logging.Logger

fun initializeKoinProduction(config: BotConfig) {
    val module = module {
        factory {
            FFMpegEncoder(config.ffmpegPath)
        }
        factory {
            Logger.getLogger(it.javaClass.packageName)
        }
        single {
            OkHttpClient()
        }
        single {
            DefaultLangStrings()
        }
        single {
            SongSearch()
        }
        single {
            SongScanner()
        }
        single {
            BotConnectionManager()
        }
        single {
            config
        }
        // Must be separated by guild so must not be singleton
        factory {
            BotStateStore()
        }
        factory {
            BotCommand()
        }
        factory {
            BotCommandProcessor()
        }
        single {
            SupervisorJob()
        }
    }
    startKoin(listOf(module))
}