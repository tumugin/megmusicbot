package com.myskng.megmusicbot.bot.music

import discord4j.voice.AudioProvider
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger

class RawOpusStreamProvider : AudioProvider(ByteBuffer.allocate(1568)), KoinComponent {
    var encodedDataInputStream: InputStream? = null
    private val logger by inject<Logger>()

    override fun provide(): Boolean {
        // When stream not available, just return a silent sound array.
        if (encodedDataInputStream == null) {
            buffer.put(byteArrayOf(0xFC.toByte(), 0xFF.toByte(), 0xFE.toByte()))
            buffer.flip()
            return true
        }
        val opusBuffer = mutableListOf<Byte>()
        try {
            while (true) {
                val readByte = encodedDataInputStream?.read()
                if (readByte == null || readByte == 0xFC || readByte == -1) {
                    break
                }
                opusBuffer.add(readByte.toByte())
            }
        } catch (ex: IOException) {
            logger.log(Level.WARNING, "[RawOpusStreamProvider] IO Error while reading byte from stream.")
        }
        if (opusBuffer.size != 0) {
            buffer.put(0xFC.toByte())
            buffer.put(opusBuffer.toByteArray())
            buffer.flip()
            return true
        }
        return false
    }
}