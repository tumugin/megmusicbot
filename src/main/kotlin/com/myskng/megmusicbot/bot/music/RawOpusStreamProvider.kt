package com.myskng.megmusicbot.bot.music

import club.minnced.opus.util.OpusLibrary
import com.sun.jna.ptr.PointerByReference
import discord4j.voice.AudioProvider
import kotlinx.coroutines.Job
import org.koin.core.KoinComponent
import org.koin.core.get
import tomp2p.opuswrapper.Opus
import tomp2p.opuswrapper.Opus.OPUS_SET_COMPLEXITY_REQUEST
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import javax.sound.sampled.AudioInputStream

class RawOpusStreamProvider(sampleRate: Int = 48000, private val audioChannels: Int = 2) :
    AudioProvider(ByteBuffer.allocate(1568)),
    KoinComponent {
    private val job = Job(get())

    // https://stackoverflow.com/questions/46786922/how-to-confirm-opus-encode-buffer-size
    private val opusFrameSize = 960
    var baseInputStream: InputStream? = null
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
        if (errorBuffer[0] != Opus.OPUS_OK) {
            throw Exception("Opus initialize error. code=${errorBuffer[0]}")
        }
    }

    override fun provide(): Boolean{
        try {
            // When stream not available, just return a silent sound array.
            if (baseInputStream == null) {
                buffer.put(byteArrayOf(0xFC.toByte(), 0xFF.toByte(), 0xFE.toByte()))
                buffer.flip()
                return true
            }
            val pcmBuffer = mutableListOf<Byte>()
            while (pcmBuffer.size < opusFrameSize * 4) {
                if (baseInputStream!!.available() >= 4) {
                    pcmBuffer.addAll(
                        baseInputStream!!.readNBytes(4).toTypedArray()
                    )
                }
            }

            val combinedPcmBuffer = createShortPcmArray(pcmBuffer)
            val encodedBuffer = ByteBuffer.allocate(1568)
            val result =
                Opus.INSTANCE.opus_encode(
                    encoderPointer,
                    combinedPcmBuffer,
                    opusFrameSize,
                    encodedBuffer,
                    encodedBuffer.capacity()
                )
            if (result > 0) {
                val encoded: ByteArray = (0..result).map { 0.toByte() }.toByteArray()
                encodedBuffer.get(encoded)
                buffer.put(encoded)
                buffer.flip()
                return true
            }
            return false
        } catch (ex: Exception) {
            return false
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
