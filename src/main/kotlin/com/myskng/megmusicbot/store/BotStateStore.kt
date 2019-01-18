package com.myskng.megmusicbot.store

import com.myskng.megmusicbot.bot.music.LocalSong
import com.myskng.megmusicbot.bot.music.SongQueueManager

class BotStateStore {
    var songQueue: SongQueueManager = SongQueueManager()
    var currentSearchList = listOf<LocalSong>()
}