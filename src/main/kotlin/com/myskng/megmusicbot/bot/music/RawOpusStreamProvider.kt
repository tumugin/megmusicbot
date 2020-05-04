package com.myskng.megmusicbot.bot.music

import club.minnced.opus.util.OpusLibrary
import com.sun.jna.ptr.PointerByReference
import discord4j.voice.AudioProvider
import kotlinx.coroutines.*
import okio.BufferedSource
import okio.EOFException
import org.koin.core.KoinComponent
import org.koin.core.get
import tomp2p.opuswrapper.Opus
import tomp2p.opuswrapper.Opus.*
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

class RawOpusStreamProvider(sampleRate: Int = 48000, audioChannels: Int = 2, var volume: Double = 1.0) :
    AudioProvider(ByteBuffer.allocate(1568)),
    KoinComponent, CoroutineScope {
    private val job = Job(get())
    override val coroutineContext = Dispatchers.IO + job

    // https://stackoverflow.com/questions/46786922/how-to-confirm-opus-encode-buffer-size
    private val opusFrameSize = 960
    private val readBytes = opusFrameSize * 4
    var decodedPCMBuffer: BufferedSource? = null
    var eofDetected = false
    private var encoderPointer: PointerByReference

    init {
        if (!OpusLibrary.isInitialized()) {
            OpusLibrary.loadFromJar()
        }
        val errorBuffer = IntBuffer.allocate(1)
        encoderPointer = Opus.INSTANCE.opus_encoder_create(
            sampleRate,
            audioChannels,
            Opus.OPUS_APPLICATION_AUDIO,
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
                // 16bit PCM 2chの1フレームは4byte
                decodedPCMBuffer!!.request(readBytes.toLong())
                val pcmBuffer = decodedPCMBuffer!!.readByteArray(readBytes.toLong())
                eofDetected = false
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
                    val encoded = ByteArray(result) { 0.toByte() }
                    encodedBuffer.get(encoded)
                    buffer.put(encoded)
                    buffer.flip()
                    return@withTimeout true
                }
                return@withTimeout false
            }
        } catch (ex: EOFException) {
            eofDetected = true
            return@runBlocking false
        } catch (ex: Exception) {
            return@runBlocking false
        }
    }

    private fun createShortPcmArray(pcm: ByteArray): ShortBuffer? {
        val nonEncodedBuffer = ShortBuffer.allocate(pcm.size / 2)
        for (i in pcm.indices step 2) {
            val firstByte = 0x000000FF and (pcm[i].toDouble() * volume).toInt()
            val secondByte = 0x000000FF and (pcm[i + 1].toDouble() * volume).toInt()
            val combined = ((firstByte shl 8) or secondByte).toShort()
            nonEncodedBuffer.put(combined)
        }
        nonEncodedBuffer.flip()
        return nonEncodedBuffer
    }
}
