package com.myskng.megmusicbot.database

import org.jetbrains.exposed.sql.Table

object Songs : Table("songs") {
    val id = integer("id").autoIncrement().primaryKey()
    val title = text("title")
    val album = text("album")
    val artist = text("artist")
    val filePath = text("file_path")
}