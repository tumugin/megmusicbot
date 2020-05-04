package com.myskng.megmusicbot.di

import com.myskng.megmusicbot.bot.BotCommand
import com.myskng.megmusicbot.bot.BotCommandProcessor
import com.myskng.megmusicbot.bot.BotConnectionManager
import com.myskng.megmusicbot.bot.music.RawOpusStreamProvider
import com.myskng.megmusicbot.config.BotConfig
import com.myskng.megmusicbot.database.SongSearch
import com.myskng.megmusicbot.encoder.FFMpegEncoder
import com.myskng.megmusicbot.encoder.IEncoderProcess
import com.myskng.megmusicbot.scanner.SongScanner
import com.myskng.megmusicbot.store.BotStateStore
import com.myskng.megmusicbot.text.DefaultLangStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.util.logging.Logger

fun initializeKoinProduction(config: BotConfig) {
    val module = module {
        factory<IEncoderProcess> {
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
            RawOpusStreamProvider()
        }
        factory {
            BotStateStore()
        }
        factory {
            BotCommand()
        }
        factory {
            BotCommandProcessor()
        }
        single<Job> {
            SupervisorJob()
        }
    }
    startKoin {
        modules(module)
    }
}
