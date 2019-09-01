package com.myskng.megmusicbot.test.base

import com.myskng.megmusicbot.encoder.IEncoderProcess
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.BufferedInputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.logging.Logger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDefaultTester {
    protected val additionalKoinModules = mutableListOf<org.koin.core.module.Module>()

    @BeforeAll
    open fun setupKoin() {
        val modules = module {
            factory {
                val pipedOutputStream = PipedOutputStream()
                val pipedInputStream = PipedInputStream()
                pipedInputStream.connect(pipedOutputStream)
                val mockIEncoderProcess = mockk<IEncoderProcess>()
                every {
                    mockIEncoderProcess.isProcessAlive
                } returns true
                every {
                    mockIEncoderProcess.stdInputStream
                } returns pipedOutputStream
                every {
                    mockIEncoderProcess.stdOutputStream
                } returns BufferedInputStream(pipedInputStream)
                mockIEncoderProcess
            }
            factory {
                Logger.getLogger(it.javaClass.packageName)
            }
            single {
                OkHttpClient()
            }
            single {
                SupervisorJob() as Job
            }
        }
        startKoin {
            modules(listOf(modules).plus(additionalKoinModules))
        }
    }

    @AfterAll
    fun cleanupKoin() {
        stopKoin()
    }
}