package com.myskng.megmusicbot.scanner

import com.myskng.megmusicbot.config.BotConfig
import com.myskng.megmusicbot.database.Songs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagException
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class SongScanner : KoinComponent, CoroutineScope {
    private val job = Job(get())
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val logger by inject<Logger>()
    private val config by inject<BotConfig>()
    private val musicFileExtension = arrayOf(".mp3", ".m4a", ".ogg", ".wma", ".flac")

    private fun addFileToDB(file: File): Boolean {
        try {
            val audioFile = AudioFileIO.read(file)
            val tags = audioFile.tag
            return transaction {
                if (Songs.select { Songs.filePath eq file.toString() }.empty()) {
                    Songs.insert {
                        it[Songs.title] = tags.getFirst(FieldKey.TITLE)
                        it[Songs.album] = tags.getFirst(FieldKey.ALBUM)
                        it[Songs.artist] = tags.getFirst(FieldKey.ARTIST)
                        it[Songs.filePath] = file.absolutePath
                    }
                    true
                } else {
                    false
                }
            }
        } catch (ex: Exception) {
            when (ex) {
                is IOException, is CannotReadException -> {
                    logger.log(Level.WARNING, "[SongScanner] IOException while reading ${file.absolutePath}")
                }
                is TagException, is InvalidAudioFrameException -> {
                    logger.log(Level.WARNING, "[SongScanner] Invalid audio file detected on ${file.absolutePath}")
                }
                else -> {
                    throw ex
                }
            }
        }
        return false
    }

    fun scanFilesAsync() = async {
        config.musicPaths.forEach { path ->
            var addedSongs = 0
            logger.log(Level.INFO, "[SongScanner] Scanning folder $path")
            Files.find(
                Paths.get(path), Int.MAX_VALUE, { _, attr -> attr.isRegularFile }, arrayOf(FileVisitOption.FOLLOW_LINKS)
            ).filter { file ->
                // is music file ext.
                musicFileExtension.map { file.fileName.toString().endsWith(it) }.contains(true)
            }.filter { file ->
                // is not in database
                transaction {
                    Songs.select { Songs.filePath eq file.toString() }.empty()
                }
            }.forEach { file ->
                // read file tag
                if (addFileToDB(file.toFile())) {
                    addedSongs++
                    logger.log(Level.INFO, "[SongScanner] Added ${file.toAbsolutePath()} .")
                }
            }
            logger.log(Level.INFO, "[SongScanner] Scanning folder done. $addedSongs songs added to DB.")
        }
    }
}
