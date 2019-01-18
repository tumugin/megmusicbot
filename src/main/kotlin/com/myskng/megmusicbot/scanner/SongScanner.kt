package com.myskng.megmusicbot.scanner

import com.myskng.megmusicbot.database.Songs
import com.myskng.megmusicbot.store.BotConfig
import com.myskng.megmusicbot.store.BotStateStore
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagException
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Level
import java.util.logging.Logger

class SongScanner : KoinComponent {
    val state by inject<BotStateStore>()
    val logger by inject<Logger>()
    val config by inject<BotConfig>()
    val musicFileExtension = arrayOf(".mp3", ".m4a", ".ogg", ".wma", ".flac")

    fun scanFiles() {
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
                try {
                    val audioFile = AudioFileIO.read(file.toFile())
                    val tags = audioFile.tag
                    transaction {
                        Songs.insert {
                            it[Songs.title] = tags.getFirst(FieldKey.TITLE)
                            it[Songs.album] = tags.getFirst(FieldKey.ALBUM)
                            it[Songs.artist] = tags.getFirst(FieldKey.ARTIST)
                            it[Songs.filePath] = file.toAbsolutePath().toString()
                        }
                    }
                    addedSongs++
                } catch (ex: Exception) {
                    when (ex) {
                        is IOException, is CannotReadException -> {
                            logger.log(Level.WARNING, "[SongScanner] IOException while reading $file")
                        }
                        is TagException, is InvalidAudioFrameException -> {
                            logger.log(Level.WARNING, "[SongScanner] Invalid audio file detected on $file")
                        }
                        else -> {
                            throw ex
                        }
                    }
                }
            }
            logger.log(Level.INFO, "[SongScanner] Scanning folder done. $addedSongs songs added to DB.")
        }
    }
}