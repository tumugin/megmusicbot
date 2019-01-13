package com.myskng.megmusicbot.store

import com.myskng.megmusicbot.bot.music.SongQueueManager

class BotStateStore {
    lateinit var config: BotConfig
    var songQueue: SongQueueManager = SongQueueManager()
}