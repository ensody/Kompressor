package com.ensody.kompressor.kotlinx.io

import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import kotlinx.io.Buffer
import kotlinx.io.DelicateIoApi
import kotlinx.io.Sink
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlin.math.min

internal class BufferSliceTransformHelper(
    private val transform: SliceTransform,
) {
    // If the source's head Segment is insufficient for the transform to do anything, we have to write the Segment's
    // data into this buffer in order to collect a larger contiguous ByteArray. Over time we'll collect sufficient data
    // to guarantee progress. We assume that the ByteArray never has to grow beyond the given size. If this can still
    // be too small, the transform is currently responsible for buffering. We might later support custom buffer sizes
    // and even growing/shrinking the buffer dynamically in order to keep the transform's complexity low.
    private val inputBuffer = ByteArraySlice(16 * 1024)

    /**
     * Transforms from source into sink and returns the number of bytes read from source.
     */
    @OptIn(UnsafeIoApi::class)
    fun transform(source: Buffer, sink: BufferTracker, readLimit: Int, finish: Boolean): Int =
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
    fun transform(source: Buffer, sink: Sink, readLimit: Int, finish: Boolean): Int =
        sink.writeToInternalBufferReturning {
            transform(source, BufferTracker(it), readLimit, finish)
        }

    fun finishInto(sink: Sink) {
        while (transform.transform(inputBuffer, sink, finish = true)) {
            // Comment needed to satisfy compiler. This consumes until there's no more data.
        }
        check(!inputBuffer.insufficient) { "Insufficient data in input buffer" }
        check(!inputBuffer.hasData) { "Input buffer was not fully consumed after close()" }
    }
}
