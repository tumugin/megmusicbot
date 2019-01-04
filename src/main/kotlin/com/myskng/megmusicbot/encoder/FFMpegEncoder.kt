package com.myskng.megmusicbot.encoder

import java.io.InputStream
import java.io.OutputStream

class FFMpegEncoder(private val executablePath: String) : IEncoderProcess {
    override val isProcessAlive: Boolean
        get() = process.isAlive
    override val stdInputStream: OutputStream
        get() = process.outputStream
    override val stdOutputStream: InputStream
        get() = process.inputStream
    private val processBuilder: ProcessBuilder = ProcessBuilder(executablePath, "-i pipe:0 -f wav pipe:1")
    private lateinit var process: Process

    override fun killProcess() {
        process.destroy()
    }

    override fun startProcess() {
        process = processBuilder.start()
    }
}