package com.ensody.kompressor.kotlinx.io

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.core.ByteArraySlice
import kotlinx.io.Buffer
import kotlinx.io.DelicateIoApi
import kotlinx.io.Sink
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlin.math.min

internal class AsyncBufferSliceTransformHelper(
    private val transform: AsyncSliceTransform,
) {
    private val inputBuffer = ByteArraySlice(16 * 1024)

    /**
     * Transforms from source into sink and returns the number of bytes read from source.
     */
    @OptIn(UnsafeIoApi::class)
    suspend fun transform(source: Buffer, sink: BufferTracker, readLimit: Int, finish: Boolean): Int =
        UnsafeBufferOperations.saferReadFromHead(source) { inBytes, inStart, inEndExclusive ->
            val maxReadSize = min(readLimit, inEndExclusive - inStart)
            val inputSegment = ByteArraySlice(inBytes, inStart, inStart + maxReadSize)
            var input = inputSegment
            if (inputBuffer.hasData) {
                inputSegment.readInto(inputBuffer)
                input = inputBuffer
            }
            transform.transform(input, sink, finish = finish)
            val inputInsufficient = input.insufficient
            inputBuffer.moveReadableToStart()
            if (inputInsufficient) {
                inputSegment.readInto(inputBuffer)
            }
            inputSegment.readStart - inStart
        }

    @OptIn(DelicateIoApi::class)
    suspend fun transform(source: Buffer, sink: Sink, readLimit: Int, finish: Boolean): Int =
        sink.writeToInternalBufferReturning {
            transform(source, BufferTracker(it), readLimit, finish)
        }

    @OptIn(DelicateIoApi::class)
    suspend fun finishInto(sink: Sink) {
        sink.writeToInternalBufferReturning {
            val tracker = BufferTracker(it)
            do {
                transform.transform(inputBuffer, tracker, finish = true)
            } while (tracker.insufficient)
            check(!inputBuffer.insufficient) { "Insufficient data in input buffer" }
            check(!inputBuffer.hasData) { "Input buffer was not fully consumed after close()" }
        }
    }
}
