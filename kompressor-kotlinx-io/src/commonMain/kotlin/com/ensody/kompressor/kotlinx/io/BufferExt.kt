package com.ensody.kompressor.kotlinx.io

import com.ensody.kompressor.core.AsyncSliceTransform
import com.ensody.kompressor.core.ByteArraySlice
import com.ensody.kompressor.core.SliceTransform
import kotlinx.io.Buffer
import kotlinx.io.DelicateIoApi
import kotlinx.io.InternalIoApi
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.UnsafeIoApi
import kotlinx.io.readTo
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlinx.io.writeToInternalBuffer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.math.min

@DelicateIoApi
@OptIn(InternalIoApi::class, ExperimentalContracts::class)
public inline fun <T> Sink.writeToInternalBufferReturning(lambda: (Buffer) -> T): T {
    contract {
        callsInPlace(lambda, EXACTLY_ONCE)
    }
    return lambda(this.buffer).also {
        hintEmit()
    }
}

@OptIn(UnsafeIoApi::class, DelicateIoApi::class)
public fun SliceTransform.transform(input: ByteArraySlice, sink: Sink, finish: Boolean): Boolean =
    sink.writeToInternalBufferReturning {
        val tracker = BufferTracker(it)
        transform(input, tracker, finish)
        tracker.insufficient
    }

@OptIn(UnsafeIoApi::class, DelicateIoApi::class)
public fun SliceTransform.transform(input: ByteArraySlice, sink: BufferTracker, finish: Boolean) {
    // We first try to write into the smallest possible segment in order to avoid lots of unused
    // capacity in the sink Buffer's Segments. If the space is insufficient we retry with a large
    // Segment, so we can guarantee progress.
    var outputInsufficient = false
    do {
        val minCapacity = if (outputInsufficient) 4096 else 64
        UnsafeBufferOperations.writeToTail(sink.buffer, minCapacity) { outBytes, outStart, outEndExclusive ->
            val output = ByteArraySlice(outBytes, outStart, outStart, outEndExclusive)
            transform(input, output, finish = finish)
            (output.writeStart - outStart).also {
                // This ensures that we only retry once
                outputInsufficient = !outputInsufficient && output.insufficient
                sink.processed += it
                sink.insufficient = output.insufficient
            }
        }
    } while (outputInsufficient)
}

@OptIn(UnsafeIoApi::class)
public fun Buffer.readAtMostTo(slice: ByteArraySlice, byteCount: Int = slice.remainingWrite): Int =
    UnsafeBufferOperations.readAtMost(
        buffer = this,
        atMost = min(byteCount, slice.remainingWrite),
    ) { bytes, start, endExclusive ->
        bytes.copyInto(slice.data, slice.writeStart, start, endExclusive)
        (endExclusive - start).also {
            slice.writeStart += it
        }
    }

public fun Source.readAtMostTo(slice: ByteArraySlice, size: Int = slice.remainingWrite): Int {
    val endIndex = min(slice.writeStart + size, slice.writeLimit)
    return readAtMostTo(slice.data, slice.writeStart, endIndex).also {
        if (it > 0) {
            slice.writeStart += it
        }
    }
}

public fun Source.readTo(slice: ByteArraySlice, size: Int = slice.remainingWrite) {
    require(size <= slice.remainingWrite) { "size must be <= slice.remainingWrite" }
    val endIndex = slice.writeStart + size
    readTo(slice.data, slice.writeStart, endIndex).also {
        slice.writeStart += endIndex
    }
}

@OptIn(UnsafeIoApi::class, DelicateIoApi::class)
public fun Sink.write(slice: ByteArraySlice, size: Int = slice.remainingRead) {
    if (size <= 0) return
    require(size <= slice.remainingRead) { "size is larger than remainingRead" }
    writeToInternalBuffer {
        UnsafeBufferOperations.saferMoveToTail(
            buffer = it,
            bytes = slice.data,
            startIndex = slice.readStart,
            endIndex = slice.readStart + size,
        )
    }
    slice.readStart += size
    if (!slice.hasData) {
        slice.setNewData()
    }
}

public fun Sink.writeAtMost(slice: ByteArraySlice, size: Int = slice.remainingRead): Int {
    val sizeToWrite = min(size, slice.remainingRead)
    write(slice, sizeToWrite)
    return sizeToWrite
}

/**
 * Reads [atMost] bytes from this [Buffer].
 *
 * This can call [readAction] multiple times (if there are multiple segments) or not at all (if empty).
 */
@UnsafeIoApi
public fun UnsafeBufferOperations.readAtMost(
    buffer: Buffer,
    atMost: Int,
    readAction: (bytes: ByteArray, start: Int, endExclusive: Int) -> Int,
): Int =
    readAll(
        buffer = buffer,
        keepUnread = buffer.size.toInt() - min(buffer.size.toInt(), atMost),
        readAction = readAction,
    )

/**
 * Reads all the data from this [Buffer], except for [keepUnread] number of bytes at the end.
 *
 * This can call [readAction] multiple times (if there are multiple segments) or not at all (if empty).
 */
@UnsafeIoApi
public fun UnsafeBufferOperations.readAll(
    buffer: Buffer,
    keepUnread: Int = 0,
    readAction: (bytes: ByteArray, start: Int, endExclusive: Int) -> Int,
): Int {
    var result = 0
    while (buffer.size > keepUnread) {
        val remaining = buffer.size.toInt() - keepUnread
        result += readFromHead(buffer) { bytes, start, endExclusive ->
            val length = min(endExclusive - start, remaining)
            readAction(bytes, start, start + length)
        }
    }
    return result
}

@OptIn(UnsafeIoApi::class)
public inline fun UnsafeBufferOperations.saferReadFromHead(
    buffer: Buffer,
    readAction: (bytes: ByteArray, startIndexInclusive: Int, endIndexExclusive: Int) -> Int,
): Int =
    if (buffer.exhausted()) {
        readAction(emptyByteArray, 0, 0)
    } else {
        readFromHead(buffer, readAction)
    }

/**
 * A safer version of [UnsafeBufferOperations.moveToTail] which doesn't cause bugs when `startIndex == endIndex`.
 */
@UnsafeIoApi
public fun UnsafeBufferOperations.saferMoveToTail(
    buffer: Buffer,
    bytes: ByteArray,
    startIndex: Int = 0,
    endIndex: Int = bytes.size,
): Int {
    val length = max(0, endIndex - startIndex)
    if (length > 0) {
        moveToTail(buffer, bytes, startIndex, endIndex)
    }
    return length
}

@OptIn(UnsafeIoApi::class, DelicateIoApi::class)
public suspend fun AsyncSliceTransform.transform(input: ByteArraySlice, sink: Sink, finish: Boolean): Boolean =
    sink.writeToInternalBufferReturning {
        val tracker = BufferTracker(it)
        transform(input, tracker, finish)
        tracker.insufficient
    }

@OptIn(UnsafeIoApi::class, DelicateIoApi::class)
public suspend fun AsyncSliceTransform.transform(input: ByteArraySlice, sink: BufferTracker, finish: Boolean) {
    // We first try to write into the smallest possible segment in order to avoid lots of unused
    // capacity in the sink Buffer's Segments. If the space is insufficient we retry with a large
    // Segment, so we can guarantee progress.
    var outputInsufficient = false
    do {
        val minCapacity = if (outputInsufficient) 4096 else 64
        UnsafeBufferOperations.writeToTail(sink.buffer, minCapacity) { outBytes, outStart, outEndExclusive ->
            val output = ByteArraySlice(outBytes, outStart, outStart, outEndExclusive)
            transform(input, output, finish = finish)
            (output.writeStart - outStart).also {
                // This ensures that we only retry once
                outputInsufficient = !outputInsufficient && output.insufficient
                sink.processed += it
                sink.insufficient = output.insufficient
            }
        }
    } while (outputInsufficient)
}

public suspend fun AsyncSliceTransform.transform(input: Buffer, sink: Sink) {
    val helper = AsyncBufferSliceTransformHelper(this)
    while (!input.exhausted()) {
        helper.transform(input, sink, input.size.toInt(), finish = false)
    }
    helper.finishInto(sink)
}

public val emptyByteArray: ByteArray = ByteArray(0)
