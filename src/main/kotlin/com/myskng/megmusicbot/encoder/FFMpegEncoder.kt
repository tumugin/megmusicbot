package com.myskng.megmusicbot.encoder

import java.io.File
import java.io.InputStream
import java.io.OutputStream

class FFMpegEncoder(executablePath: String) : IEncoderProcess {
    override val isProcessAlive: Boolean
        get() = process.isAlive
    override val stdInputStream: OutputStream
        get() = process.outputStream
    override val stdOutputStream: InputStream
        get() = process.inputStream
    private val command = mutableListOf(executablePath).also {
        it.addAll(
            "-i pipe:0 -ar 48000 -ac 2 -acodec pcm_s16be -map 0:a -f s16be pipe:1".split(" ")
        )
    }
    private val processBuilder: ProcessBuilder = ProcessBuilder(command)
    private lateinit var process: Process

    override fun killProcess() {
        process.destroy()
    }

    override fun startProcess() {
        processBuilder.redirectError(File("/dev/null"))
        process = processBuilder.start()
    }
}
