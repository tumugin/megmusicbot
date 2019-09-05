package com.myskng.megmusicbot.test.server

import fi.iki.elonen.NanoHTTPD
import okio.Buffer
import okio.buffer
import okio.source
import java.io.File

class TestHTTPServer(port: Int = 8888) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession?): Response {
        when (session?.uri) {
            "/test2.flac" -> {
                val buffer = Buffer()
                val length = File("./test2.flac").source().buffer().readAll(buffer)
                return newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "application/octet-stream", buffer.inputStream(), length
                )
            }
            "/rawopus.blob" -> {
                val buffer = Buffer()
                val length = File("./rawopus.blob").source().buffer().readAll(buffer)
                return newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "application/octet-stream", buffer.inputStream(), length
                )
            }
            else -> {
                return super.serve(session)
            }
        }
    }
}