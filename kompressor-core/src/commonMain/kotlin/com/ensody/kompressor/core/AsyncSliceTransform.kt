package com.ensody.kompressor.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * An asynchronous data [SliceTransform], using a `suspend` [transform] function.
 *
 * This could be needed if you want to support JS/WASM targets because on the web many operations are async.
 */
public interface AsyncSliceTransform {
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
    public suspend fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean)
}

/**
 * Converts a [SliceTransform] into an [AsyncSliceTransform].
 */
public fun SliceTransform.toAsync(dispatcher: CoroutineContext = Dispatchers.Default): AsyncSliceTransform =
    AsyncSliceTransformConverter(this, dispatcher)

/** Transforms the whole [input] and returns the full transformed result. */
public suspend fun AsyncSliceTransform.transform(input: ByteArray): ByteArray {
    val inputSlice = ByteArraySlice(input)
    val outputSlices = mutableListOf(ByteArraySlice(8192))
    do {
        val outputSlice = outputSlices.last()
        transform(inputSlice, outputSlice, finish = true)
        if (outputSlice.insufficient) {
            outputSlices.add(ByteArraySlice(8192))
        }
    } while (inputSlice.remainingRead != 0 || outputSlice.insufficient)
    return outputSlices.getOutput()
}

private class AsyncSliceTransformConverter(
    private val transform: SliceTransform,
    private val dispatcher: CoroutineContext,
) : AsyncSliceTransform {
    override suspend fun transform(input: ByteArraySlice, output: ByteArraySlice, finish: Boolean) {
        withContext(dispatcher) {
            transform.transform(input, output, finish)
        }
    }
}
