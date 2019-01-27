package com.myskng.megmusicbot.test.base

import com.myskng.megmusicbot.encoder.IEncoderProcess
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext
import sx.blah.discord.handle.audio.IAudioProvider
import sx.blah.discord.handle.audio.impl.AudioManager
import java.io.BufferedInputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.logging.Logger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDefaultTester {
    protected val additionalKoinModules = mutableListOf<Module>()

    @BeforeAll
    open fun setupKoin() {
        val modules = module {
            factory {
                val pipedOutputStream = PipedOutputStream()
                val pipedInputStream = PipedInputStream()
                pipedInputStream.connect(pipedOutputStream)
                mock<IEncoderProcess> {
                    on { isProcessAlive } doReturn true
                    on { stdInputStream } doReturn pipedOutputStream
                    on { stdOutputStream } doReturn BufferedInputStream(pipedInputStream)
                }
            }
            factory {
                var audioProvider: IAudioProvider? = null
                mock<AudioManager> {
                    on { setAudioProvider(any()) }.then {
                        audioProvider = it.arguments.first() as IAudioProvider
                        Unit
                    }
                    on { getAudioProvider() }.thenAnswer {
                        audioProvider
                    }
                }
            }
            factory {
                Logger.getLogger(it.javaClass.packageName)
            }
            single {
                OkHttpClient()
            }
            single {
                SupervisorJob()
            }
        }
        StandAloneContext.startKoin(listOf(modules).plus(additionalKoinModules))
    }

    @AfterAll
    fun cleanupKoin() {
        StandAloneContext.stopKoin()
    }
}