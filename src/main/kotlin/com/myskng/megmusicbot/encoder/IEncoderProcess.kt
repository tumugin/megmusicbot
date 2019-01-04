package com.myskng.megmusicbot.encoder

import java.io.InputStream
import java.io.OutputStream

interface IEncoderProcess {
    fun startProcess()
    fun killProcess()
    val isProcessAlive: Boolean
    val stdInputStream: OutputStream
    val stdOutputStream: InputStream
}