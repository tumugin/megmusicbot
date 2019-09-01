package com.myskng.megmusicbot.bot.music

import discord4j.voice.AudioProvider
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

    override fun provide(): Boolean = runBlocking {
        withTimeout(1000) {
            // When stream not available, just return a silent sound array.
            if (encodedDataInputStream == null) {
                buffer.put(byteArrayOf(0xFC.toByte(), 0xFF.toByte(), 0xFE.toByte()))
                buffer.flip()
                return@withTimeout true
            }
            val opusBuffer = mutableListOf<Byte>()
            var frameStarted = false
            try {
                while (isActive) {
                    val availableBytesCount = encodedDataInputStream?.available()
                    // read()は無限にブロッキングするので危険。ちゃんと値が返ってくることが保証されてから呼ぶべし。
                    if (availableBytesCount == 0) {
                        continue
                    }
                    val readByte = encodedDataInputStream?.read()
                    if (!frameStarted && readByte == 0xFC) {
                        frameStarted = true
                        continue
                    } else if (frameStarted && readByte == 0xFC) {
                        break
                    }
                    if (readByte == null || readByte == -1) {
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
                return@withTimeout true
            }
            buffer.put(byteArrayOf(0xFC.toByte(), 0xFF.toByte(), 0xFE.toByte()))
            buffer.flip()
            return@withTimeout true
        }
    }
}