package com.myskng.megmusicbot.bot

import com.myskng.megmusicbot.bot.music.HTTPFileSong
import com.myskng.megmusicbot.bot.music.LocalSong
import com.myskng.megmusicbot.bot.music.RawOpusStreamProvider
import com.myskng.megmusicbot.bot.music.SongQueueManager
import com.myskng.megmusicbot.database.SearchQuery
import com.myskng.megmusicbot.database.SongSearch
import com.myskng.megmusicbot.store.BotStateStore
import com.myskng.megmusicbot.text.DefaultLangStrings
import discord4j.core.`object`.entity.VoiceChannel
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.voice.VoiceConnection
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.KoinComponent
import org.koin.core.inject
import reactor.core.publisher.toMono
import java.util.logging.Level
import java.util.logging.Logger

open class BotCommandProcessor : KoinComponent {
    private val logger by inject<Logger>()
    private val store by inject<BotStateStore>()
    private val botStrings by inject<DefaultLangStrings>()
    private val songSearch by inject<SongSearch>()
    private var voiceConnection: VoiceConnection? = null

    open suspend fun outputHelpText(event: MessageCreateEvent) {
        event.message.channel.awaitFirst().createMessage(botStrings.botHelpText).awaitSingle()
    }

    open suspend fun joinVoiceChannel(event: MessageCreateEvent) {
        val channel = event.guild.awaitSingle().channels.filterWhen {
            if (it !is VoiceChannel) return@filterWhen false.toMono()
            it.voiceStates.filter { voiceState -> voiceState.userId == event.message.author.get().id }.hasElements()
        }.awaitFirstOrNull() as VoiceChannel?
        if (channel != null) {
            val rawOpusStreamProvider = RawOpusStreamProvider()
            voiceConnection = channel.join {
                it.setProvider(rawOpusStreamProvider)
            }.awaitSingle()
            store.songQueue = SongQueueManager()
            val randomSongPlayer = RandomSongPlayer()
            store.songQueue.onQueueEmpty = {
                randomSongPlayer.onEmptyQueue()
            }
            store.songQueue.playQueue(rawOpusStreamProvider)
        } else {
            event.message.channel
                .awaitSingle()
                .createMessage("${event.message.author.get().mention} ボイスチャンネルに参加してください")
                .awaitSingle()
        }
    }

    open suspend fun leaveVoiceChannel(event: MessageCreateEvent) {
        store.songQueue.stop()
        if (voiceConnection != null) {
            voiceConnection?.disconnect()
        } else {
            event.message.channel
                .awaitSingle()
                .createMessage("${event.message.author.get().mention} 参加していないチャンネルに対する操作はできません。")
                .awaitSingle()
        }
    }

    open suspend fun leaveVoiceChannel(event: VoiceStateUpdateEvent) {
        voiceConnection?.disconnect()
    }

    open suspend fun searchSong(query: Array<SearchQuery>, event: MessageCreateEvent) {
        val originalResultList = songSearch.searchSong(query)
        val resultList = originalResultList.take(5)
        store.currentSearchList = resultList
        var printText = ""
        resultList.forEachIndexed { num, item ->
            val rowNumHeader = "[${num + 1}] "
            val rowHeaderPlaceHolder = rowNumHeader.replace(Regex("."), " ")
            val rowText = "> ${rowNumHeader}Title: ${item.title}\n" +
                    "> ${rowHeaderPlaceHolder}Album: ${item.album}\n" +
                    "> ${rowHeaderPlaceHolder}Artist: ${item.artist}"
            printText += rowText + "\n"
        }
        event.message.channel
            .awaitSingle()
            .createMessage("${originalResultList.count()}件見つかりました\n$printText")
            .awaitSingle()
    }

    open suspend fun playSong(playIndex: Int, event: MessageCreateEvent) {
        if (store.currentSearchList.count() >= playIndex + 1) {
            val song = store.currentSearchList[playIndex]
            store.songQueue.songQueue.add(song)
            event.message.channel
                .awaitSingle()
                .createMessage("**${song.title}**をキューに追加しました。")
                .awaitSingle()
        } else {
            event.message.channel
                .awaitSingle()
                .createMessage("不正な番号が指定されました。")
                .awaitSingle()
        }
    }

    open suspend fun printQueue(event: MessageCreateEvent) {
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
            event.message.channel
                .awaitSingle()
                .createMessage(printStr)
                .awaitSingle()
        } else {
            event.message.channel
                .awaitSingle()
                .createMessage("再生キューが空です。")
                .awaitSingle()
        }
    }
}
