package com.ensody.kompressor.core

import kotlin.math.max

/**
 * Transforms data contained in [ByteArraySlice]. Examples: compression, encryption, base64, hex, etc.
 */
public interface SliceTransform {
    /**
     * Transforms the given [input] into [output].
     *
     * After every call, the [input]'s [ByteArraySlice.readStart] and [output]'s [ByteArraySlice.writeStart] MUST be
     * incremented by the amount of read/written data.
     *
     * If you leave some remaining [input] data after the call and you need more data, set `input.insufficient = true`.
     * In this case, the next call will either have more [input] data or [finish] will be true (see below).
     *
     * If the available space in [output] is too small, you should try to write as much as possible and then
     * set `output.insufficient = true`. In this case, the next call will have more available space.
     * If you've set `output.insufficient = true`, you MUST set it back to false if the remaining space is sufficient.
     *
     * If [finish] is false, try to transform as much as possible from [input] to [output].
     *
     * If [finish] is true, you MUST read any remaining [input] and fully write all transformed data to [output].
     * If the available space in [output] is too small, try to write as much as possible and then
     * set `output.insufficient = true`. In this case, this method is repeatedly called with [output] having
     * more available space until you set `output.insufficient = false`.
     */
    public fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean)
}

/** Transforms the whole [input] and returns the full transformed result. */
public fun SliceTransform.transform(input: ByteArray): ByteArray {
    val inputSlice = ByteArraySlice(input)
    val blockSize = max(8192, input.size / 10)
    val outputSlices = mutableListOf(ByteArraySlice(blockSize))
    do {
        val outputSlice = outputSlices.last()
        transform(inputSlice, outputSlice, finish = true)
        if (outputSlice.insufficient) {
            outputSlices.add(ByteArraySlice(blockSize))
        }
    } while (inputSlice.remainingRead != 0 || outputSlice.insufficient)
    return outputSlices.getOutput()
}

internal fun List<ByteArraySlice>.getOutput(): ByteArray {
    val size = sumOf { it.remainingRead }
    val result = ByteArray(size)
    var start = 0
    for (slice in this) {
        slice.data.copyInto(result, start, slice.readStart, slice.writeStart)
        start += slice.remainingRead
    }
    return result
}
