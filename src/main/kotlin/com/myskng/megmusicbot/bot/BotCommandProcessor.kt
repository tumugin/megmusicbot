package com.myskng.megmusicbot.bot

import com.myskng.megmusicbot.bot.music.HTTPFileSong
import com.myskng.megmusicbot.bot.music.LocalSong
import com.myskng.megmusicbot.bot.music.RawOpusStreamProvider
import com.myskng.megmusicbot.bot.music.SongQueueManager
import com.myskng.megmusicbot.database.SearchQuery
import com.myskng.megmusicbot.database.SongSearch
import com.myskng.megmusicbot.store.BotStateStore
import com.myskng.megmusicbot.text.DefaultLangStrings
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.voice.VoiceConnection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.KoinComponent
import org.koin.core.inject
import reactor.kotlin.core.publisher.toMono
import java.awt.Color

open class BotCommandProcessor : KoinComponent {
    private val store by inject<BotStateStore>()
    private val botStrings by inject<DefaultLangStrings>()
    private val songSearch by inject<SongSearch>()
    private val rawOpusStreamProvider by inject<RawOpusStreamProvider>()
    private var voiceConnection: VoiceConnection? = null

    open suspend fun outputHelpText(event: MessageCreateEvent) {
        event.message.channel.awaitSingle()
            .createEmbed {
                it.setTitle("使い方")
                    .setDescription(botStrings.botHelpText)
                    .setColor(Color(92, 230, 38))
            }
            .awaitSingle()
    }

    open suspend fun joinVoiceChannel(event: MessageCreateEvent) {
        val channel = event.guild.awaitSingle().channels.filterWhen {
            if (it !is VoiceChannel) return@filterWhen false.toMono()
            it.voiceStates.filter { voiceState -> voiceState.userId == event.message.author.get().id }.hasElements()
        }.awaitFirstOrNull() as VoiceChannel?
        if (channel != null) {
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
            voiceConnection?.disconnect()?.awaitFirstOrNull()
        } else {
            event.message.channel
                .awaitSingle()
                .createMessage("${event.message.author.get().mention} 参加していないチャンネルに対する操作はできません。")
                .awaitSingle()
        }
    }

    open suspend fun leaveVoiceChannel() {
        store.songQueue.stop()
        voiceConnection?.disconnect()?.awaitFirstOrNull()
    }

    open suspend fun searchSong(query: Array<SearchQuery>, event: MessageCreateEvent) {
        val resultList = songSearch.searchSong(query)
        store.currentSearchList = resultList
        var printText = ""
        resultList.take(5).forEachIndexed { num, item ->
            val rowNumHeader = "[${num + 1}] "
            val rowHeaderPlaceHolder = rowNumHeader.replace(Regex("."), " ")
            val rowText = "> ${rowNumHeader}Title: ${item.title}\n" +
                    "> ${rowHeaderPlaceHolder}Album: ${item.album}\n" +
                    "> ${rowHeaderPlaceHolder}Artist: ${item.artist}"
            printText += rowText + "\n"
        }
        event.message.channel.awaitSingle()
            .createEmbed {
                it.setTitle("検索結果")
                    .setDescription("**${resultList.count()}件**見つかりました\n$printText")
                    .setColor(Color(247, 161, 186))
            }
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

    open suspend fun playSongWarikomi(playIndex: Int, event: MessageCreateEvent) {
        if (store.currentSearchList.count() >= playIndex + 1) {
            val song = store.currentSearchList[playIndex]
            store.songQueue.songQueue.add(0, song)
            event.message.channel
                .awaitSingle()
                .createMessage("**${song.title}**をキューの先頭に追加しました。")
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
            event.message.channel.awaitSingle()
                .createEmbed {
                    it.setTitle("再生キュー")
                        .setDescription(printStr)
                        .setColor(Color(122, 218, 214))
                }
                .awaitSingle()
        } else {
            event.message.channel.awaitSingle()
                .createEmbed {
                    it.setTitle("再生キュー")
                        .setDescription("再生キューが空です。")
                        .setColor(Color(215, 56, 95))
                }
                .awaitSingle()
        }
    }

    open suspend fun skipSong() {
        store.songQueue.skip()
    }

    open suspend fun isNowPlaying(event: MessageCreateEvent) {
        if (store.songQueue.playingSong == null) {
            event.message.channel
                .awaitSingle()
                .createMessage("現在何も再生していません。")
                .awaitSingle()
            return
        }
        val song = store.songQueue.playingSong!!
        event.message.channel.awaitSingle()
            .createEmbed {
                it.setTitle("再生中の曲")
                    .setDescription("**Title:** ${song.title}\n**Album:** ${song.album}\n**Artist:** ${song.artist}")
                    .setColor(Color(209, 98, 203))
            }
            .awaitSingle()
    }

    open suspend fun clearAllQueue(event: MessageCreateEvent) {
        store.songQueue.songQueue.clear()
        event.message.channel
            .awaitSingle()
            .createMessage("キューをすべて消去しました。")
            .awaitSingle()
    }

    open suspend fun playAllSongs(event: MessageCreateEvent) {
        store.songQueue.songQueue.addAll(store.currentSearchList)
        val songCount = store.currentSearchList.count()
        event.message.channel
            .awaitSingle()
            .createMessage("検索結果にある全ての楽曲(${songCount}曲)をキューに追加しました。")
            .awaitSingle()
    }

    open suspend fun setVolume(volume: Int, event: MessageCreateEvent) {
        if (volume !in 0..100) {
            event.message.channel
                .awaitSingle()
                .createMessage("不正な音量設定です(0から100の値で指定してください)")
                .awaitSingle()
            return
        }
        rawOpusStreamProvider.volume = volume / 100.0
        event.message.channel
            .awaitSingle()
            .createMessage("音量を${volume}に設定しました")
            .awaitSingle()
    }
}
