package com.myskng.megmusicbot.bot

import com.myskng.megmusicbot.bot.music.HTTPFileSong
import com.myskng.megmusicbot.bot.music.LocalSong
import com.myskng.megmusicbot.bot.music.SongQueueManager
import com.myskng.megmusicbot.database.SearchQuery
import com.myskng.megmusicbot.database.SongSearch
import com.myskng.megmusicbot.store.BotStateStore
import com.myskng.megmusicbot.text.DefaultLangStrings
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

open class BotCommandProcessor : KoinComponent {
    private val store by inject<BotStateStore>()
    private val botStrings by inject<DefaultLangStrings>()
    private val songSearch by inject<SongSearch>()

    open fun outputHelpText(event: MessageReceivedEvent) {
        event.channel.sendMessage(botStrings.botHelpText)
    }

    open suspend fun joinVoiceChannel(event: MessageReceivedEvent) {
        val client = event.client
        val channel =
            client.voiceChannels.firstOrNull {
                it.connectedUsers.any { user -> user.longID == event.author.longID }
            }
        if (channel != null) {
            channel.join()
            store.songQueue = SongQueueManager()
            val randomSongPlayer = RandomSongPlayer()
            store.songQueue.onQueueEmpty = {
                randomSongPlayer.onEmptyQueue()
            }
            store.songQueue.playQueue(channel.guild.audioManager)
        } else {
            event.channel.sendMessage("@${event.message.author.name} ボイスチャンネルに参加してください")
        }
    }

    open fun leaveVoiceChannel(event: MessageReceivedEvent) {
        store.songQueue.stop()
        val channel =
            event.client.connectedVoiceChannels.firstOrNull { it.connectedUsers.any { user -> user.longID == event.author.longID } }
        if (channel != null) {
            channel.leave()
        } else {
            event.channel.sendMessage("@${event.message.author.name} 参加していないチャンネルに対する操作はできません。")
        }
    }

    open fun searchSong(query: Array<SearchQuery>, event: MessageReceivedEvent) {
        val originalResultList = songSearch.searchSong(query)
        val resultList = originalResultList.take(5)
        store.currentSearchList = resultList
        var printText = ""
        resultList.forEachIndexed { num, item ->
            val rowNumHeader = "[${num + 1}] "
            val rowHeaderPlaceHolder = rowNumHeader.replace(Regex("."), " ")
            val rowText = "${rowNumHeader}Title: ${item.title}\n" +
                    "${rowHeaderPlaceHolder}Album: ${item.album}\n" +
                    "${rowHeaderPlaceHolder}Artist: ${item.artist}"
            printText += rowText + "\n"
        }
        event.channel.sendMessage("${originalResultList.count()}件見つかりました\n$printText")
    }

    open fun playSong(playIndex: Int, event: MessageReceivedEvent) {
        if (store.currentSearchList.count() >= playIndex + 1) {
            val song = store.currentSearchList[playIndex]
            store.songQueue.songQueue.add(song)
            event.channel.sendMessage("${song.title}をキューに追加しました。")
        } else {
            event.channel.sendMessage("不正な番号が指定されました。")
        }
    }

    open fun printQueue(event: MessageReceivedEvent) {
        if (store.songQueue.songQueue.isNotEmpty()) {
            var printStr = "現在キューには${store.songQueue.songQueue.count()}件の曲が追加されています。"
            store.songQueue.songQueue.take(10).map {
                when (it) {
                    is LocalSong -> "${it.title} - ${it.artist}"
                    is HTTPFileSong -> it.fileUrl
                    else -> it.title
                }
            }.forEach {
                printStr += "\n" + it
            }
            event.channel.sendMessage(printStr)
        } else {
            event.channel.sendMessage("再生キューが空です。")
        }
    }
}