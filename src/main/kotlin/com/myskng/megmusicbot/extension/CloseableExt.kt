package com.myskng.megmusicbot.extension

import java.io.Closeable

fun <R> Array<Closeable>.use(block: () -> R): R {
    if (isEmpty()) {
        throw Exception("Array must have one or more items.")
    }
    var offset = 0
    fun nestedFunc(): R {
        offset++
        if (offset != count() - 1) {
            get(offset).use {
                return nestedFunc()
            }
        } else {
            return block()
        }
    }
    first().use {
        return nestedFunc()
    }
}

fun <R> useMultipleCloseable(vararg closeable: Closeable, block: () -> R): R {
    if (closeable.isEmpty()) {
        throw Exception("Array must have one or more items.")
    }
    var offset = 0
    fun nestedFunc(): R {
        offset++
        if (offset != closeable.count() - 1) {
            closeable[offset].use {
                return nestedFunc()
            }
        } else {
            return block()
        }
    }
    closeable.first().use {
        return nestedFunc()
    }
}