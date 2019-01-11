package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.music.HTTPFileSong
import com.myskng.megmusicbot.bot.music.ISong
import com.myskng.megmusicbot.bot.music.LocalSong
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import com.myskng.megmusicbot.test.server.TestHTTPServer
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.koin.standalone.KoinComponent
import org.koin.standalone.get
import sx.blah.discord.handle.audio.impl.AudioManager
import java.io.IOException
import java.util.stream.Stream

class SongTester : AbstractDefaultTester(), KoinComponent {
    lateinit var testServer: TestHTTPServer

    @BeforeAll
    fun prepareServer() {
        testServer = TestHTTPServer(8888)
        testServer.start()
    }

    @AfterAll
    fun stopServer() {
        testServer.stop()
    }

    class ISongArgumentProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                HTTPFileSong("http://127.0.0.1:8888/test2.flac"),
                LocalSong(
                    "銀河図書館",
                    "鷺沢文香(CV.M・A・O)",
                    "THE IDOLM@STER CINDERELLA GIRLS STARLIGHT MASTER 19 With Love",
                    "./test2.flac"
                )
            ).map { Arguments.of(it) }
        }
    }

    class NonExistISongArgumentProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                HTTPFileSong("http://127.0.0.1:8888/MINAMICHAAAAAAANNNNN.flac"),
                LocalSong(
                    "銀河図書館",
                    "鷺沢文香(CV.M・A・O)",
                    "THE IDOLM@STER CINDERELLA GIRLS STARLIGHT MASTER 19 With Love",
                    "./FUMIFUMIFUMIFUMI.flac"
                )
            ).map { Arguments.of(it) }
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ISongArgumentProvider::class)
    fun httpFileSongTest(song: ISong) = runBlocking {
        val audioManager = get<AudioManager>()
        val playingJob = async { song.play(audioManager) }
        // wait until audioProvider prepared
        withTimeout(5000) {
            while (audioManager.audioProvider == null && isActive) {
                delay(10)
            }
        }
        // read all bytes from stream
        while (true) {
            if (audioManager.audioProvider.provide().isEmpty()) {
                break
            }
        }
        // should stop in few seconds
        withTimeout(5000) {
            playingJob.await()
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ISongArgumentProvider::class)
    fun httpFileSongStopTest(song: ISong) = runBlocking {
        val audioManager = get<AudioManager>()
        val playingJob = async { song.play(audioManager) }
        // wait until audioProvider prepared
        withTimeout(5000) {
            while (audioManager.audioProvider == null && isActive) {
                delay(10)
            }
        }
        song.stop()
        // should stop in few seconds
        withTimeout(5000) {
            playingJob.await()
        }
    }

    @ParameterizedTest
    @ArgumentsSource(NonExistISongArgumentProvider::class)
    fun nonExistSongTest(song: ISong) = runBlocking {
        val audioManager = get<AudioManager>()
        song.onError = { ex ->
            Assertions.assertTrue(ex is IOException)
        }
        song.play(audioManager)
    }
}