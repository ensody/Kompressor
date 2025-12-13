package com.ensody.kompressor.test

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlin.math.min
import kotlin.random.Random

/**
 * A [kotlinx.io.RawSource] that generates at most [maxBytes] of random data from the given [random] source.
 */
public class RandomSource(private val random: Random, private val maxBytes: Long) : RawSource {
    private var generated: Long = 0L
    private var closed = false

    @OptIn(UnsafeIoApi::class)
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "RandomRawSource is closed" }
        check(byteCount > 0) { "readAtMostTo's byteCount parameter must be greater than 0" }

        val maxBytes = min(byteCount, maxBytes - generated)
        if (maxBytes == 0L) return -1

        val result = UnsafeBufferOperations.writeToTail(sink, 1) { bytes, start, endExclusive ->
            val end = min(endExclusive, (start + maxBytes).toInt())
            random.nextBytes(bytes, start, end)
            end - start
        }
        generated += result
        return generated
    }

    override fun close() {
        closed = true
    }
}
