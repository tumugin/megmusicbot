package com.myskng.megmusicbot.extension

import java.io.Closeable

suspend fun <R> useMultipleCloseableSuspend(vararg closeable: Closeable, block: suspend () -> R): R {
    if (closeable.isEmpty()) {
        throw Exception("Array must have one or more items.")
    }
    suspend fun nestedFunc(offset: Int): R {
        if (offset != closeable.count() - 1) {
            closeable[offset].use {
                return nestedFunc(offset + 1)
            }
        } else {
            return block()
        }
    }
    closeable.first().use {
        return nestedFunc(0)
    }
}