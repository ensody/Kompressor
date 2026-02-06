package com.ensody.kompressor.core

import kotlin.jvm.JvmField

// TODO: Make the slice usable for read and write by having three indices:
//  readStart < writeStart < writeLimit

/**
 * A slice of a ByteArray that can be used for reading/writing via [SliceTransform]/[AsyncSliceTransform].
 *
 * The readable region goes from [readStart] to [writeStart] exclusive. After reading, [readStart] gets moved forward.
 * The writable region goes from [writeStart] to [writeLimit] exclusive. After writing, [writeStart] gets moved forward.
 */
public class ByteArraySlice(
    @JvmField
    public var data: ByteArray,
    @JvmField
    public var readStart: Int = 0,
    @JvmField
    public var writeStart: Int = data.size,
    @JvmField
    public var writeLimit: Int = data.size,
    @JvmField
    public var insufficient: Boolean = false,
) {
    /**
     * Creates a new [ByteArraySlice] of the given size, with [writeStart] at 0, ready to be filled with data.
     */
    public constructor(size: Int) : this(ByteArray(size), writeStart = 0)

    public val remainingRead: Int get() = writeStart - readStart
    public val remainingWrite: Int get() = writeLimit - writeStart

    public val hasData: Boolean get() = remainingRead != 0
    public val isFull: Boolean get() = writeStart == writeLimit

    public val shouldMoveReadable: Boolean get() = !hasData || insufficient
    public val shouldFlushWritten: Boolean get() = isFull || insufficient

    public fun onInputConsumed() {
        if (shouldMoveReadable) {
            moveReadableToStart()
        }
    }

    public fun moveReadableToStart() {
        if (hasData) {
            data.copyInto(data, 0, readStart, writeStart)
        }
        writeStart = remainingRead
        readStart = 0
        insufficient = false
    }

    public fun setNewData(
        data: ByteArray = ByteArray(this.data.size),
        readStart: Int = 0,
        writeStart: Int = 0,
        writeLimit: Int = data.size,
    ) {
        this.data = data
        this.readStart = readStart
        this.writeStart = writeStart
        this.writeLimit = writeLimit
        this.insufficient = false
    }

    public fun readInto(other: ByteArraySlice, size: Int = remainingRead) {
        check(size >= 0) { "size must not be negative" }
        check(size <= remainingRead) { "size is larger than input slice" }
        check(size <= other.remainingWrite) { "size is larger than output slice" }
        val endExclusive = readStart + size
        data.copyInto(other.data, other.writeStart, readStart, endExclusive)
        readStart = endExclusive
        other.writeStart += size
    }

    public fun readInto(dest: ByteArray, destOffset: Int, size: Int = remainingRead) {
        check(size >= 0) { "size must not be negative" }
        check(size <= remainingRead) { "size is larger than input slice" }
        val destEnd = destOffset + size
        check(destEnd <= dest.size) { "size + destOffset is larger than output size" }
        val endExclusive = readStart + size
        data.copyInto(dest, destOffset, readStart, endExclusive)
        readStart = endExclusive
    }

    override fun toString(): String =
        "ByteArraySlice(remainingRead=$remainingRead, insufficient=$insufficient)"
}
