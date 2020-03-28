package com.myskng.megmusicbot.bot.music

import club.minnced.opus.util.OpusLibrary
import com.sun.jna.ptr.PointerByReference
import discord4j.voice.AudioProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.koin.core.KoinComponent
import org.koin.core.get
import tomp2p.opuswrapper.Opus
import tomp2p.opuswrapper.Opus.*
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

class RawOpusStreamProvider(sampleRate: Int = 48000, audioChannels: Int = 2) :
    AudioProvider(ByteBuffer.allocate(1568)),
    KoinComponent, CoroutineScope {
    private val job = Job(get())
    override val coroutineContext = Dispatchers.IO + job

    // https://stackoverflow.com/questions/46786922/how-to-confirm-opus-encode-buffer-size
    private val opusFrameSize = 960
    var decodedPCMBuffer: Channel<Byte>? = null
    private var encoderPointer: PointerByReference

    init {
        if (!OpusLibrary.isInitialized()) {
            OpusLibrary.loadFromJar()
        }
        val errorBuffer = IntBuffer.allocate(1)
        encoderPointer = Opus.INSTANCE.opus_encoder_create(
            sampleRate,
            audioChannels,
            Opus.OPUS_APPLICATION_RESTRICTED_LOWDELAY,
            errorBuffer
        )
        Opus.INSTANCE.opus_encoder_ctl(encoderPointer, OPUS_SET_COMPLEXITY_REQUEST, 10)
        Opus.INSTANCE.opus_encoder_ctl(encoderPointer, OPUS_SET_BANDWIDTH_REQUEST, OPUS_BANDWIDTH_FULLBAND)
        Opus.INSTANCE.opus_encoder_ctl(encoderPointer, OPUS_SET_BITRATE_REQUEST, OPUS_BITRATE_MAX)
        if (errorBuffer[0] != Opus.OPUS_OK) {
            throw Exception("Opus initialize error. code=${errorBuffer[0]}")
        }
    }

    override fun provide(): Boolean = runBlocking {
        try {
            return@runBlocking withTimeout(30) {
                // When stream not available, just return a silent sound array.
                if (decodedPCMBuffer == null) {
                    buffer.put(byteArrayOf(0xFC.toByte(), 0xFF.toByte(), 0xFE.toByte()))
                    buffer.flip()
                    return@withTimeout true
                }
                val pcmBuffer = mutableListOf<Byte>()
                // 16bit PCM 2chの1フレームは4byte
                while (pcmBuffer.size < opusFrameSize * 4) {
                    pcmBuffer.add(decodedPCMBuffer!!.receive())
                }
                val combinedPcmBuffer = createShortPcmArray(pcmBuffer)
                val encodedBuffer = ByteBuffer.allocate(512)
                val result =
                    Opus.INSTANCE.opus_encode(
                        encoderPointer,
                        combinedPcmBuffer,
                        opusFrameSize,
                        encodedBuffer,
                        encodedBuffer.capacity()
                    )
                if (result > 0) {
                    val encoded: ByteArray = (1..result).map { 0.toByte() }.toByteArray()
                    encodedBuffer.get(encoded)
                    buffer.put(encoded)
                    buffer.flip()
                    return@withTimeout true
                }
                return@withTimeout false
            }
        } catch (ex: Exception) {
            return@runBlocking false
        }
    }

    private fun createShortPcmArray(pcm: List<Byte>): ShortBuffer? {
        val nonEncodedBuffer = ShortBuffer.allocate(pcm.size / 2)
        for (i in pcm.indices step 2) {
            val firstByte = 0x000000FF and pcm[i].toInt()
            val secondByte = 0x000000FF and pcm[i + 1].toInt()
            val combined = ((firstByte shl 8) or secondByte).toShort()
            nonEncodedBuffer.put(combined)
        }
        nonEncodedBuffer.flip()
        return nonEncodedBuffer
    }
}
